package com.example.jonesspencer_ce07.ui.detail

import com.example.jonesspencer_ce07.model.Book

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data class Success(val book: Book) : BookDetailUiState
    data class Error(val message: String) : BookDetailUiState
}
