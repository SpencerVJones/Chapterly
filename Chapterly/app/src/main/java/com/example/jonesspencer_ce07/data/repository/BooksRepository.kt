package com.example.jonesspencer_ce07.data.repository

import androidx.paging.PagingData
import com.example.jonesspencer_ce07.model.Book
import com.example.jonesspencer_ce07.model.SortOption
import kotlinx.coroutines.flow.Flow

interface BooksRepository {
    fun pagedBooks(
        query: String,
        sortOption: SortOption,
        favoritesOnly: Boolean
    ): Flow<PagingData<Book>>

    fun observeBook(bookId: String): Flow<Book?>

    fun observeSearchHistory(limit: Int = SEARCH_HISTORY_LIMIT): Flow<List<String>>

    suspend fun saveSearchQuery(query: String)

    suspend fun clearSearchHistory()

    suspend fun toggleFavorite(bookId: String, currentlyFavorite: Boolean)

    suspend fun refreshBookDetail(bookId: String)
}
