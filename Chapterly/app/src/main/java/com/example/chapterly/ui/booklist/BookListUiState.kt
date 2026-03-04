package com.example.chapterly.ui.booklist

import com.example.chapterly.model.BookSortOption

sealed interface BookListUiState {
    data class Content(
        val queryInput: String,
        val activeQuery: String,
        val sortOption: BookSortOption,
        val favoritesOnly: Boolean,
        val searchHistory: List<String>,
        val favoriteCount: Int,
        val isSignedIn: Boolean,
    ) : BookListUiState
}
