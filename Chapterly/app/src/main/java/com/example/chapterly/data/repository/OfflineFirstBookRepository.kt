package com.example.chapterly.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.example.chapterly.data.cloud.CloudAnnotationEntry
import com.example.chapterly.data.cloud.CloudReadingProgressEntry
import com.example.chapterly.data.cloud.CloudSearchHistoryEntry
import com.example.chapterly.data.cloud.CloudSyncDataSource
import com.example.chapterly.data.local.AnnotationEntity
import com.example.chapterly.data.local.BookDao
import com.example.chapterly.data.local.BookDatabase
import com.example.chapterly.data.local.BookEntity
import com.example.chapterly.data.local.FavoriteEntity
import com.example.chapterly.data.local.ReadingProgressEntity
import com.example.chapterly.data.local.SearchHistoryEntity
import com.example.chapterly.data.remote.BookRemoteMediator
import com.example.chapterly.data.remote.GoogleBooksApi
import com.example.chapterly.model.BookAnnotation
import com.example.chapterly.model.Book
import com.example.chapterly.model.BookSortOption
import com.example.chapterly.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstBookRepository
    @Inject
    constructor(
        private val api: GoogleBooksApi,
        private val database: BookDatabase,
        private val bookDao: BookDao,
        private val cloudSyncDataSource: CloudSyncDataSource,
    ) : BookRepository {
        @OptIn(ExperimentalPagingApi::class)
        override fun pagedBooks(
            query: String,
            sortOption: BookSortOption,
            favoritesOnly: Boolean,
        ): Flow<PagingData<Book>> {
            return if (favoritesOnly) {
                favoritesPagedBooks(query = query, sortOption = sortOption)
            } else {
                remotePagedBooks(query = query, sortOption = sortOption)
            }
        }

        private fun favoritesPagedBooks(
            query: String,
            sortOption: BookSortOption,
        ): Flow<PagingData<Book>> {
            val favoritesSearchQuery =
                query.trim().let { trimmed ->
                    if (trimmed.equals(DEFAULT_QUERY, ignoreCase = true)) {
                        ""
                    } else {
                        trimmed.lowercase()
                    }
                }
            return Pager(
                config = pagingConfig(),
                pagingSourceFactory = {
                    when (sortOption) {
                        BookSortOption.NEWEST -> bookDao.pagingSourceFavoritesNewest(favoritesSearchQuery)
                        BookSortOption.RATING -> bookDao.pagingSourceFavoritesRating(favoritesSearchQuery)
                        BookSortOption.RELEVANCE -> bookDao.pagingSourceFavoritesRelevance(favoritesSearchQuery)
                    }
                },
            ).flow.map { pagingData ->
                pagingData.map { row -> row.toDomain() }
            }
        }

        @OptIn(ExperimentalPagingApi::class)
        private fun remotePagedBooks(
            query: String,
            sortOption: BookSortOption,
        ): Flow<PagingData<Book>> {
            val normalizedQuery = normalizeQuery(query)
            val queryKey = normalizeQueryKey(query)
            return Pager(
                config = pagingConfig(),
                remoteMediator =
                    BookRemoteMediator(
                        query = normalizedQuery,
                        api = api,
                        database = database,
                    ),
                pagingSourceFactory = {
                    when (sortOption) {
                        BookSortOption.NEWEST -> bookDao.pagingSourceNewest(queryKey)
                        BookSortOption.RATING -> bookDao.pagingSourceRating(queryKey)
                        BookSortOption.RELEVANCE -> bookDao.pagingSourceRelevance(queryKey)
                    }
                },
            ).flow.map { pagingData ->
                pagingData.map { row -> row.toDomain() }
            }
        }

        private fun pagingConfig(): PagingConfig {
            return PagingConfig(
                pageSize = BOOKS_PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = BOOKS_PAGE_SIZE / 2,
            )
        }

        override fun observeBook(bookId: String): Flow<Book?> {
            return bookDao.observeBook(bookId).map { row ->
                row?.toDomain()
            }
        }

        override fun observeSearchHistory(limit: Int): Flow<List<String>> {
            return bookDao.observeSearchHistory(limit)
        }

        override suspend fun saveSearchQuery(query: String) {
            val normalized = normalizeQuery(query)
            bookDao.upsertSearchHistory(
                SearchHistoryEntity(
                    query = normalized,
                    timestamp = System.currentTimeMillis(),
                ),
            )
            cloudSyncDataSource.saveSearchQuery(normalized)
        }

        override suspend fun clearSearchHistory() {
            bookDao.clearSearchHistory()
            cloudSyncDataSource.clearSearchHistory()
        }

        override suspend fun toggleFavorite(
            bookId: String,
            currentlyFavorite: Boolean,
        ) {
            val isFavoriteAfterToggle =
                if (currentlyFavorite) {
                    bookDao.removeFavorite(bookId)
                    false
                } else {
                    bookDao.addFavorite(FavoriteEntity(bookId))
                    true
                }

            cloudSyncDataSource.syncFavorite(
                bookId = bookId,
                isFavorite = isFavoriteAfterToggle,
            )
        }

        override suspend fun refreshBookDetail(bookId: String) {
            val volume = api.getVolume(bookId)
            val volumeInfo = volume.volumeInfo

            bookDao.upsertBooks(
                listOf(
                    BookEntity(
                        queryKey = DETAIL_QUERY_KEY,
                        bookId = volume.id,
                        title = volumeInfo?.title?.ifBlank { DEFAULT_TITLE } ?: DEFAULT_TITLE,
                        authors =
                            volumeInfo?.authors
                                ?.takeIf { it.isNotEmpty() }
                                ?.joinToString(AUTHOR_SEPARATOR)
                                .orEmpty(),
                        description = volumeInfo?.description?.ifBlank { DEFAULT_DESCRIPTION },
                        thumbnailUrl =
                            buildImageUrl(
                                thumbnail = volumeInfo?.imageLinks?.thumbnail,
                                fallback = volumeInfo?.imageLinks?.smallThumbnail,
                            ),
                        previewLink = buildPreviewUrl(volumeInfo?.previewLink),
                        publishedDate = volumeInfo?.publishedDate?.ifBlank { DEFAULT_PUBLISHED_DATE },
                        pageCount = volumeInfo?.pageCount?.takeIf { it > 0 },
                        averageRating = volumeInfo?.averageRating,
                        listPosition = 0,
                        cachedAt = System.currentTimeMillis(),
                    ),
                ),
            )
        }

        override suspend fun syncUserDataFromCloud() {
            val remoteFavoriteIds = cloudSyncDataSource.fetchFavoriteBookIds()
            val remoteHistory = cloudSyncDataSource.fetchSearchHistory()
            val remoteProgress = cloudSyncDataSource.fetchReadingProgress()
            val remoteAnnotations = cloudSyncDataSource.fetchAnnotations()

            val localFavorites = bookDao.getFavorites()
            val localHistory = bookDao.getSearchHistory()
            val localProgress = bookDao.getReadingProgress()
            val localAnnotations = bookDao.getAnnotations()

            val localFavoriteIds = localFavorites.map { favorite -> favorite.bookId }.toSet()
            val mergedFavoriteIds = localFavoriteIds + remoteFavoriteIds

            val remoteHistoryByQuery =
                remoteHistory.associateBy { entry -> entry.query }
            val remoteProgressByBookId =
                remoteProgress.associateBy { entry -> entry.bookId }
            val remoteAnnotationsById =
                remoteAnnotations.associateBy { entry -> entry.id }
            val mergedHistory =
                (remoteHistory + localHistory.map { entry ->
                    CloudSearchHistoryEntry(
                        query = entry.query,
                        timestamp = entry.timestamp,
                    )
                }).groupBy { entry -> entry.query }
                    .mapNotNull { (query, entries) ->
                        val normalizedQuery = query.trim()
                        if (normalizedQuery.isBlank()) {
                            null
                        } else {
                            SearchHistoryEntity(
                                query = normalizedQuery,
                                timestamp = entries.maxOf { entry -> entry.timestamp },
                            )
                        }
                    }
            val mergedProgress =
                (remoteProgress.map { entry -> entry.toEntity() } + localProgress)
                    .groupBy { entry -> entry.bookId }
                    .map { (_, entries) -> entries.maxBy { entry -> entry.updatedAt } }
            val mergedAnnotations =
                (remoteAnnotations.map { entry -> entry.toEntity() } + localAnnotations)
                    .groupBy { entry -> entry.id }
                    .mapNotNull { (_, entries) -> entries.maxByOrNull { entry -> entry.updatedAt } }

            database.withTransaction {
                bookDao.clearFavorites()
                if (mergedFavoriteIds.isNotEmpty()) {
                    bookDao.upsertFavorites(
                        mergedFavoriteIds.map { bookId -> FavoriteEntity(bookId) },
                    )
                }

                bookDao.clearSearchHistory()
                if (mergedHistory.isNotEmpty()) {
                    bookDao.upsertSearchHistory(mergedHistory)
                }

                bookDao.clearReadingProgress()
                if (mergedProgress.isNotEmpty()) {
                    bookDao.upsertReadingProgress(mergedProgress)
                }

                bookDao.clearAnnotations()
                if (mergedAnnotations.isNotEmpty()) {
                    bookDao.upsertAnnotations(mergedAnnotations)
                }
            }

            (localFavoriteIds - remoteFavoriteIds).forEach { bookId ->
                cloudSyncDataSource.syncFavorite(
                    bookId = bookId,
                    isFavorite = true,
                )
            }

            localHistory.forEach { entry ->
                val remoteEntry = remoteHistoryByQuery[entry.query]
                if (remoteEntry == null || entry.timestamp > remoteEntry.timestamp) {
                    cloudSyncDataSource.saveSearchQuery(
                        query = entry.query,
                        timestamp = entry.timestamp,
                    )
                }
            }

            localProgress.forEach { entry ->
                val remoteEntry = remoteProgressByBookId[entry.bookId]
                if (remoteEntry == null || entry.updatedAt > remoteEntry.updatedAt) {
                    cloudSyncDataSource.upsertReadingProgress(entry.toCloudEntry())
                }
            }

            localAnnotations.forEach { entry ->
                val remoteEntry = remoteAnnotationsById[entry.id]
                if (remoteEntry == null || entry.updatedAt > remoteEntry.updatedAt) {
                    cloudSyncDataSource.upsertAnnotation(entry.toCloudEntry())
                }
            }
        }

        override suspend fun clearUserLocalData() {
            database.withTransaction {
                bookDao.clearFavorites()
                bookDao.clearSearchHistory()
                bookDao.clearReadingProgress()
                bookDao.clearAnnotations()
            }
        }

        override fun observeFavoriteCount(): Flow<Int> = bookDao.observeFavoritesCount()

        override fun observeSearchHistoryCount(): Flow<Int> = bookDao.observeSearchHistoryCount()

        override fun observeCachedBookCount(): Flow<Int> = bookDao.observeCachedBookCount()

        override fun observeReadingProgress(bookId: String): Flow<ReadingProgress?> {
            return bookDao.observeReadingProgress(bookId).map { progress ->
                progress?.toDomain()
            }
        }

        override fun observeCurrentlyReading(): Flow<List<ReadingProgress>> {
            return bookDao.observeCurrentlyReading().map { progress ->
                progress.map { entry -> entry.toDomain() }
            }
        }

        override fun observeCurrentlyReadingCount(): Flow<Int> = bookDao.observeCurrentlyReadingCount()

        override suspend fun upsertReadingProgress(progress: ReadingProgress) {
            val normalized = progress.normalize()
            bookDao.upsertReadingProgress(normalized.toEntity())
            cloudSyncDataSource.upsertReadingProgress(normalized.toCloudEntry())
        }

        override fun observeAnnotations(bookId: String): Flow<List<BookAnnotation>> {
            return bookDao.observeAnnotations(bookId).map { annotations ->
                annotations.map { entry -> entry.toDomain() }
            }
        }

        override fun observeAnnotationCount(annotationType: String?): Flow<Int> {
            val normalizedType = annotationType?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
            return if (normalizedType == null) {
                bookDao.observeAnnotationCount()
            } else {
                bookDao.observeAnnotationCount(normalizedType)
            }
        }

        override suspend fun saveAnnotation(annotation: BookAnnotation) {
            val normalized = annotation.normalize()
            bookDao.upsertAnnotation(normalized.toEntity())
            cloudSyncDataSource.upsertAnnotation(normalized.toCloudEntry())
        }

        override suspend fun clearOfflineCache() {
            database.withTransaction {
                bookDao.clearAllBooks()
                bookDao.clearAllRemoteKeys()
            }
        }

        private companion object {
            private const val DETAIL_QUERY_KEY = "_detail_"
        }
    }

private fun ReadingProgress.normalize(): ReadingProgress {
    return copy(
        bookId = bookId.trim(),
        percentComplete = percentComplete.coerceIn(0, 100),
        chaptersCompleted = chaptersCompleted.coerceAtLeast(0),
        minutesRead = minutesRead.coerceAtLeast(0),
        currentShelf = currentShelf.trim().uppercase().ifBlank { "TO_READ" },
        lastReadAt = lastReadAt.coerceAtLeast(0L),
        updatedAt = maxOf(updatedAt, System.currentTimeMillis()),
    )
}

private fun ReadingProgress.toEntity(): ReadingProgressEntity {
    return ReadingProgressEntity(
        bookId = bookId,
        percentComplete = percentComplete,
        chaptersCompleted = chaptersCompleted,
        minutesRead = minutesRead,
        currentShelf = currentShelf,
        lastReadAt = lastReadAt,
        updatedAt = updatedAt,
    )
}

private fun ReadingProgressEntity.toDomain(): ReadingProgress {
    return ReadingProgress(
        bookId = bookId,
        percentComplete = percentComplete,
        chaptersCompleted = chaptersCompleted,
        minutesRead = minutesRead,
        currentShelf = currentShelf,
        lastReadAt = lastReadAt,
        updatedAt = updatedAt,
    )
}

private fun ReadingProgress.toCloudEntry(): CloudReadingProgressEntry {
    return CloudReadingProgressEntry(
        bookId = bookId,
        percentComplete = percentComplete,
        chaptersCompleted = chaptersCompleted,
        minutesRead = minutesRead,
        currentShelf = currentShelf,
        lastReadAt = lastReadAt,
        updatedAt = updatedAt,
    )
}

private fun ReadingProgressEntity.toCloudEntry(): CloudReadingProgressEntry {
    return CloudReadingProgressEntry(
        bookId = bookId,
        percentComplete = percentComplete,
        chaptersCompleted = chaptersCompleted,
        minutesRead = minutesRead,
        currentShelf = currentShelf,
        lastReadAt = lastReadAt,
        updatedAt = updatedAt,
    )
}

private fun CloudReadingProgressEntry.toEntity(): ReadingProgressEntity {
    return ReadingProgressEntity(
        bookId = bookId.trim(),
        percentComplete = percentComplete.coerceIn(0, 100),
        chaptersCompleted = chaptersCompleted.coerceAtLeast(0),
        minutesRead = minutesRead.coerceAtLeast(0),
        currentShelf = currentShelf.trim().uppercase().ifBlank { "TO_READ" },
        lastReadAt = lastReadAt.coerceAtLeast(0L),
        updatedAt = updatedAt.coerceAtLeast(0L),
    )
}

private fun BookAnnotation.normalize(): BookAnnotation {
    return copy(
        id = id.trim().ifBlank { UUID.randomUUID().toString() },
        bookId = bookId.trim(),
        annotationType = annotationType.trim().uppercase().ifBlank { "NOTE" },
        content = content.trim(),
        chapterLabel = chapterLabel.trim(),
        pageRef = pageRef.trim(),
        visibility = visibility.trim().uppercase().ifBlank { "PRIVATE" },
        updatedAt = maxOf(updatedAt, System.currentTimeMillis()),
    )
}

private fun BookAnnotation.toEntity(): AnnotationEntity {
    return AnnotationEntity(
        id = id,
        bookId = bookId,
        annotationType = annotationType,
        content = content,
        chapterLabel = chapterLabel,
        pageRef = pageRef,
        visibility = visibility,
        updatedAt = updatedAt,
    )
}

private fun AnnotationEntity.toDomain(): BookAnnotation {
    return BookAnnotation(
        id = id,
        bookId = bookId,
        annotationType = annotationType,
        content = content,
        chapterLabel = chapterLabel,
        pageRef = pageRef,
        visibility = visibility,
        updatedAt = updatedAt,
    )
}

private fun BookAnnotation.toCloudEntry(): CloudAnnotationEntry {
    return CloudAnnotationEntry(
        id = id,
        bookId = bookId,
        annotationType = annotationType,
        content = content,
        chapterLabel = chapterLabel,
        pageRef = pageRef,
        visibility = visibility,
        updatedAt = updatedAt,
    )
}

private fun AnnotationEntity.toCloudEntry(): CloudAnnotationEntry {
    return CloudAnnotationEntry(
        id = id,
        bookId = bookId,
        annotationType = annotationType,
        content = content,
        chapterLabel = chapterLabel,
        pageRef = pageRef,
        visibility = visibility,
        updatedAt = updatedAt,
    )
}

private fun CloudAnnotationEntry.toEntity(): AnnotationEntity {
    return AnnotationEntity(
        id = id.trim(),
        bookId = bookId.trim(),
        annotationType = annotationType.trim().uppercase().ifBlank { "NOTE" },
        content = content.trim(),
        chapterLabel = chapterLabel.trim(),
        pageRef = pageRef.trim(),
        visibility = visibility.trim().uppercase().ifBlank { "PRIVATE" },
        updatedAt = updatedAt.coerceAtLeast(0L),
    )
}
