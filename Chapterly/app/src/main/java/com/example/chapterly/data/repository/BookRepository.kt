package com.example.chapterly.data.repository

import androidx.paging.PagingData
import com.example.chapterly.model.BookAnnotation
import com.example.chapterly.model.Book
import com.example.chapterly.model.BookSortOption
import com.example.chapterly.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun pagedBooks(
        query: String,
        sortOption: BookSortOption,
        favoritesOnly: Boolean,
    ): Flow<PagingData<Book>>

    fun observeBook(bookId: String): Flow<Book?>

    fun observeSearchHistory(limit: Int = SEARCH_HISTORY_LIMIT): Flow<List<String>>

    suspend fun saveSearchQuery(query: String)

    suspend fun clearSearchHistory()

    suspend fun toggleFavorite(
        bookId: String,
        currentlyFavorite: Boolean,
    )

    suspend fun refreshBookDetail(bookId: String)

    suspend fun syncUserDataFromCloud()

    suspend fun clearUserLocalData()

    fun observeFavoriteCount(): Flow<Int>

    fun observeSearchHistoryCount(): Flow<Int>

    fun observeCachedBookCount(): Flow<Int>

    fun observeReadingProgress(bookId: String): Flow<ReadingProgress?>

    fun observeCurrentlyReading(): Flow<List<ReadingProgress>>

    fun observeCurrentlyReadingCount(): Flow<Int>

    suspend fun upsertReadingProgress(progress: ReadingProgress)

    fun observeAnnotations(bookId: String): Flow<List<BookAnnotation>>

    fun observeAnnotationCount(annotationType: String? = null): Flow<Int>

    suspend fun saveAnnotation(annotation: BookAnnotation)

    suspend fun clearOfflineCache()
}
