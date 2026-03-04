package com.example.jonesspencer_ce07.ui.books

import com.example.jonesspencer_ce07.model.SortOption

sealed interface BooksUiState {
    data class Content(
        val queryInput: String,
        val activeQuery: String,
        val sortOption: SortOption,
        val favoritesOnly: Boolean,
        val searchHistory: List<String>
    ) : BooksUiState
}
