package com.example.jonesspencer_ce07.ui.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jonesspencer_ce07.data.repository.BooksRepository
import com.example.jonesspencer_ce07.ui.navigation.BOOK_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BooksRepository
) : ViewModel() {

    private val bookId: String = Uri.decode(checkNotNull(savedStateHandle[BOOK_ID_ARG]))

    private val _uiState = MutableStateFlow<BookDetailUiState>(BookDetailUiState.Loading)
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadBook()
        observeBook()
    }

    fun loadBook() {
        viewModelScope.launch {
            _uiState.value = BookDetailUiState.Loading
            runCatching {
                repository.refreshBookDetail(bookId)
            }

            val cachedBook = repository.observeBook(bookId).firstOrNull()
            if (cachedBook == null) {
                _uiState.value = BookDetailUiState.Error("Unable to load book details.")
            }
        }
    }

    fun onFavoriteClicked() {
        val currentBook = (_uiState.value as? BookDetailUiState.Success)?.book ?: return
        viewModelScope.launch {
            repository.toggleFavorite(
                bookId = currentBook.id,
                currentlyFavorite = currentBook.isFavorite
            )
        }
    }

    private fun observeBook() {
        viewModelScope.launch {
            repository.observeBook(bookId).collect { book ->
                if (book != null) {
                    _uiState.value = BookDetailUiState.Success(book)
                }
            }
        }
    }
}
