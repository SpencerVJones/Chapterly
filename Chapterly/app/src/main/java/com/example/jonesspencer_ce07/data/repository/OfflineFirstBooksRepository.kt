package com.example.jonesspencer_ce07.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.jonesspencer_ce07.data.local.BookEntity
import com.example.jonesspencer_ce07.data.local.BooksDao
import com.example.jonesspencer_ce07.data.local.BooksDatabase
import com.example.jonesspencer_ce07.data.local.FavoriteEntity
import com.example.jonesspencer_ce07.data.local.SearchHistoryEntity
import com.example.jonesspencer_ce07.data.remote.BooksRemoteMediator
import com.example.jonesspencer_ce07.data.remote.GoogleBooksApi
import com.example.jonesspencer_ce07.model.Book
import com.example.jonesspencer_ce07.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstBooksRepository @Inject constructor(
    private val api: GoogleBooksApi,
    private val database: BooksDatabase,
    private val booksDao: BooksDao
) : BooksRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun pagedBooks(
        query: String,
        sortOption: SortOption,
        favoritesOnly: Boolean
    ): Flow<PagingData<Book>> {
        val normalizedQuery = normalizeQuery(query)
        val queryKey = normalizeQueryKey(query)

        return Pager(
            config = PagingConfig(
                pageSize = BOOKS_PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = BOOKS_PAGE_SIZE / 2
            ),
            remoteMediator = BooksRemoteMediator(
                query = normalizedQuery,
                api = api,
                database = database
            ),
            pagingSourceFactory = {
                when {
                    favoritesOnly && sortOption == SortOption.TITLE_ASC -> {
                        booksDao.pagingSourceFavoritesTitle(queryKey)
                    }
                    favoritesOnly -> booksDao.pagingSourceFavoritesRelevance(queryKey)
                    sortOption == SortOption.TITLE_ASC -> booksDao.pagingSourceTitle(queryKey)
                    else -> booksDao.pagingSourceRelevance(queryKey)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { row -> row.toDomain() }
        }
    }

    override fun observeBook(bookId: String): Flow<Book?> {
        return booksDao.observeBook(bookId).map { row ->
            row?.toDomain()
        }
    }

    override fun observeSearchHistory(limit: Int): Flow<List<String>> {
        return booksDao.observeSearchHistory(limit)
    }

    override suspend fun saveSearchQuery(query: String) {
        val normalized = normalizeQuery(query)
        booksDao.upsertSearchHistory(
            SearchHistoryEntity(
                query = normalized,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearSearchHistory() {
        booksDao.clearSearchHistory()
    }

    override suspend fun toggleFavorite(bookId: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            booksDao.removeFavorite(bookId)
        } else {
            booksDao.addFavorite(FavoriteEntity(bookId))
        }
    }

    override suspend fun refreshBookDetail(bookId: String) {
        val volume = api.getVolume(bookId)
        val volumeInfo = volume.volumeInfo

        booksDao.upsertBooks(
            listOf(
                BookEntity(
                    queryKey = DETAIL_QUERY_KEY,
                    bookId = volume.id,
                    title = volumeInfo?.title?.ifBlank { DEFAULT_TITLE } ?: DEFAULT_TITLE,
                    authors = volumeInfo?.authors
                        ?.takeIf { it.isNotEmpty() }
                        ?.joinToString(AUTHOR_SEPARATOR)
                        .orEmpty(),
                    description = volumeInfo?.description?.ifBlank { DEFAULT_DESCRIPTION },
                    thumbnailUrl = buildImageUrl(
                        thumbnail = volumeInfo?.imageLinks?.thumbnail,
                        fallback = volumeInfo?.imageLinks?.smallThumbnail
                    ),
                    previewLink = buildPreviewUrl(volumeInfo?.previewLink),
                    publishedDate = volumeInfo?.publishedDate?.ifBlank { DEFAULT_PUBLISHED_DATE },
                    listPosition = 0,
                    cachedAt = System.currentTimeMillis()
                )
            )
        )
    }

    private companion object {
        private const val DETAIL_QUERY_KEY = "_detail_"
    }
}
