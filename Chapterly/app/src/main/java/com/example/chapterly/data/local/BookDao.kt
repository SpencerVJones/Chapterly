package com.example.chapterly.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query(
        """
        SELECT
            b.queryKey AS queryKey,
            b.bookId AS bookId,
            b.title AS title,
            b.authors AS authors,
            b.description AS description,
            b.thumbnailUrl AS thumbnailUrl,
            b.previewLink AS previewLink,
            b.publishedDate AS publishedDate,
            b.pageCount AS pageCount,
            b.averageRating AS averageRating,
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            CASE WHEN f.bookId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM books b
        LEFT JOIN favorites f ON f.bookId = b.bookId
        WHERE b.queryKey = :queryKey
        ORDER BY b.listPosition ASC
        """,
    )
    fun pagingSourceRelevance(queryKey: String): PagingSource<Int, BookRow>

    @Query(
        """
        SELECT
            b.queryKey AS queryKey,
            b.bookId AS bookId,
            b.title AS title,
            b.authors AS authors,
            b.description AS description,
            b.thumbnailUrl AS thumbnailUrl,
            b.previewLink AS previewLink,
            b.publishedDate AS publishedDate,
            b.pageCount AS pageCount,
            b.averageRating AS averageRating,
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            CASE WHEN f.bookId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM books b
        LEFT JOIN favorites f ON f.bookId = b.bookId
        WHERE b.queryKey = :queryKey
        ORDER BY b.publishedDate DESC, b.listPosition ASC
        """,
    )
    fun pagingSourceNewest(queryKey: String): PagingSource<Int, BookRow>

    @Query(
        """
        SELECT
            b.queryKey AS queryKey,
            b.bookId AS bookId,
            b.title AS title,
            b.authors AS authors,
            b.description AS description,
            b.thumbnailUrl AS thumbnailUrl,
            b.previewLink AS previewLink,
            b.publishedDate AS publishedDate,
            b.pageCount AS pageCount,
            b.averageRating AS averageRating,
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            CASE WHEN f.bookId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM books b
        LEFT JOIN favorites f ON f.bookId = b.bookId
        WHERE b.queryKey = :queryKey
        ORDER BY COALESCE(b.averageRating, 0) DESC, b.listPosition ASC
        """,
    )
    fun pagingSourceRating(queryKey: String): PagingSource<Int, BookRow>

    @Query(
        """
        SELECT
            b.queryKey AS queryKey,
            b.bookId AS bookId,
            b.title AS title,
            b.authors AS authors,
            b.description AS description,
            b.thumbnailUrl AS thumbnailUrl,
            b.previewLink AS previewLink,
            b.publishedDate AS publishedDate,
            b.pageCount AS pageCount,
            b.averageRating AS averageRating,
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            1 AS isFavorite
        FROM books b
        INNER JOIN favorites f ON f.bookId = b.bookId
        WHERE b.rowid = (
            SELECT b2.rowid
            FROM books b2
            WHERE b2.bookId = b.bookId
            ORDER BY b2.cachedAt DESC
            LIMIT 1
        )
        AND (
            :searchQuery = ''
            OR lower(b.title) LIKE '%' || :searchQuery || '%'
            OR lower(b.authors) LIKE '%' || :searchQuery || '%'
        )
        ORDER BY b.cachedAt DESC, b.listPosition ASC
        """,
    )
    fun pagingSourceFavoritesRelevance(searchQuery: String): PagingSource<Int, BookRow>

    @Query(
        """
        SELECT
            b.queryKey AS queryKey,
            b.bookId AS bookId,
            b.title AS title,
            b.authors AS authors,
            b.description AS description,
            b.thumbnailUrl AS thumbnailUrl,
            b.previewLink AS previewLink,
            b.publishedDate AS publishedDate,
            b.pageCount AS pageCount,
            b.averageRating AS averageRating,
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            1 AS isFavorite
        FROM books b
        INNER JOIN favorites f ON f.bookId = b.bookId
        WHERE b.rowid = (
            SELECT b2.rowid
            FROM books b2
            WHERE b2.bookId = b.bookId
            ORDER BY b2.cachedAt DESC
            LIMIT 1
        )
        AND (
            :searchQuery = ''
            OR lower(b.title) LIKE '%' || :searchQuery || '%'
            OR lower(b.authors) LIKE '%' || :searchQuery || '%'
        )
        ORDER BY b.publishedDate DESC, b.cachedAt DESC, b.listPosition ASC
        """,
    )
    fun pagingSourceFavoritesNewest(searchQuery: String): PagingSource<Int, BookRow>

    @Query(
        """
        SELECT
            b.queryKey AS queryKey,
            b.bookId AS bookId,
            b.title AS title,
            b.authors AS authors,
            b.description AS description,
            b.thumbnailUrl AS thumbnailUrl,
            b.previewLink AS previewLink,
            b.publishedDate AS publishedDate,
            b.pageCount AS pageCount,
            b.averageRating AS averageRating,
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            1 AS isFavorite
        FROM books b
        INNER JOIN favorites f ON f.bookId = b.bookId
        WHERE b.rowid = (
            SELECT b2.rowid
            FROM books b2
            WHERE b2.bookId = b.bookId
            ORDER BY b2.cachedAt DESC
            LIMIT 1
        )
        AND (
            :searchQuery = ''
            OR lower(b.title) LIKE '%' || :searchQuery || '%'
            OR lower(b.authors) LIKE '%' || :searchQuery || '%'
        )
        ORDER BY COALESCE(b.averageRating, 0) DESC, b.cachedAt DESC, b.listPosition ASC
        """,
    )
    fun pagingSourceFavoritesRating(searchQuery: String): PagingSource<Int, BookRow>

    @Query(
        """
        SELECT
            b.queryKey AS queryKey,
            b.bookId AS bookId,
            b.title AS title,
            b.authors AS authors,
            b.description AS description,
            b.thumbnailUrl AS thumbnailUrl,
            b.previewLink AS previewLink,
            b.publishedDate AS publishedDate,
            b.pageCount AS pageCount,
            b.averageRating AS averageRating,
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            CASE WHEN f.bookId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM books b
        LEFT JOIN favorites f ON f.bookId = b.bookId
        WHERE b.bookId = :bookId
        ORDER BY b.cachedAt DESC
        LIMIT 1
        """,
    )
    fun observeBook(bookId: String): Flow<BookRow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE queryKey = :queryKey")
    suspend fun clearBooksForQuery(queryKey: String)

    @Query("DELETE FROM books")
    suspend fun clearAllBooks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRemoteKey(remoteKey: RemoteKeysEntity)

    @Query("SELECT * FROM remote_keys WHERE queryKey = :queryKey")
    suspend fun remoteKey(queryKey: String): RemoteKeysEntity?

    @Query("DELETE FROM remote_keys WHERE queryKey = :queryKey")
    suspend fun clearRemoteKey(queryKey: String)

    @Query("DELETE FROM remote_keys")
    suspend fun clearAllRemoteKeys()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorites(favorites: List<FavoriteEntity>)

    @Query("SELECT * FROM favorites")
    suspend fun getFavorites(): List<FavoriteEntity>

    @Query("DELETE FROM favorites WHERE bookId = :bookId")
    suspend fun removeFavorite(bookId: String)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(searchHistory: SearchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(searchHistory: List<SearchHistoryEntity>)

    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeSearchHistory(limit: Int): Flow<List<String>>

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getSearchHistory(): List<SearchHistoryEntity>

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeFavoritesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM search_history")
    fun observeSearchHistoryCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT bookId) FROM books")
    fun observeCachedBookCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: List<ReadingProgressEntity>)

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    fun observeReadingProgress(bookId: String): Flow<ReadingProgressEntity?>

    @Query(
        """
        SELECT * FROM reading_progress
        WHERE currentShelf = 'CURRENTLY_READING'
        ORDER BY updatedAt DESC
        """,
    )
    fun observeCurrentlyReading(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC")
    suspend fun getReadingProgress(): List<ReadingProgressEntity>

    @Query("SELECT COUNT(*) FROM reading_progress WHERE currentShelf = 'CURRENTLY_READING'")
    fun observeCurrentlyReadingCount(): Flow<Int>

    @Query("DELETE FROM reading_progress")
    suspend fun clearReadingProgress()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnotation(annotation: AnnotationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnotations(annotations: List<AnnotationEntity>)

    @Query(
        """
        SELECT * FROM annotations
        WHERE bookId = :bookId
        ORDER BY updatedAt DESC
        """,
    )
    fun observeAnnotations(bookId: String): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations ORDER BY updatedAt DESC")
    suspend fun getAnnotations(): List<AnnotationEntity>

    @Query("SELECT COUNT(*) FROM annotations")
    fun observeAnnotationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM annotations WHERE annotationType = :annotationType")
    fun observeAnnotationCount(annotationType: String): Flow<Int>

    @Query("DELETE FROM annotations")
    suspend fun clearAnnotations()

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
