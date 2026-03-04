package com.example.chapterly.ui.bookdetail

import com.example.chapterly.model.Book

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState

    data class Success(val book: Book) : BookDetailUiState

    data class Error(val message: String) : BookDetailUiState
}
