package com.example.jonesspencer_ce07.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BooksDao {

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
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            CASE WHEN f.bookId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM books b
        LEFT JOIN favorites f ON f.bookId = b.bookId
        WHERE b.queryKey = :queryKey
        ORDER BY b.listPosition ASC
        """
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
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            CASE WHEN f.bookId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM books b
        LEFT JOIN favorites f ON f.bookId = b.bookId
        WHERE b.queryKey = :queryKey
        ORDER BY b.title COLLATE NOCASE ASC
        """
    )
    fun pagingSourceTitle(queryKey: String): PagingSource<Int, BookRow>

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
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            1 AS isFavorite
        FROM books b
        INNER JOIN favorites f ON f.bookId = b.bookId
        WHERE b.queryKey = :queryKey
        ORDER BY b.listPosition ASC
        """
    )
    fun pagingSourceFavoritesRelevance(queryKey: String): PagingSource<Int, BookRow>

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
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            1 AS isFavorite
        FROM books b
        INNER JOIN favorites f ON f.bookId = b.bookId
        WHERE b.queryKey = :queryKey
        ORDER BY b.title COLLATE NOCASE ASC
        """
    )
    fun pagingSourceFavoritesTitle(queryKey: String): PagingSource<Int, BookRow>

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
            b.listPosition AS listPosition,
            b.cachedAt AS cachedAt,
            CASE WHEN f.bookId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM books b
        LEFT JOIN favorites f ON f.bookId = b.bookId
        WHERE b.bookId = :bookId
        ORDER BY b.cachedAt DESC
        LIMIT 1
        """
    )
    fun observeBook(bookId: String): Flow<BookRow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE queryKey = :queryKey")
    suspend fun clearBooksForQuery(queryKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRemoteKey(remoteKey: RemoteKeysEntity)

    @Query("SELECT * FROM remote_keys WHERE queryKey = :queryKey")
    suspend fun remoteKey(queryKey: String): RemoteKeysEntity?

    @Query("DELETE FROM remote_keys WHERE queryKey = :queryKey")
    suspend fun clearRemoteKey(queryKey: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE bookId = :bookId")
    suspend fun removeFavorite(bookId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(searchHistory: SearchHistoryEntity)

    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeSearchHistory(limit: Int): Flow<List<String>>

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
