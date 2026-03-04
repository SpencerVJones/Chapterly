package com.example.chapterly.ui.booklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.chapterly.data.auth.AuthRepository
import com.example.chapterly.data.auth.AuthState
import com.example.chapterly.data.repository.BookRepository
import com.example.chapterly.data.repository.DEFAULT_QUERY
import com.example.chapterly.model.Book
import com.example.chapterly.model.BookSortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BookListViewModel
    @Inject
    constructor(
        private val repository: BookRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val queryInput = MutableStateFlow(DEFAULT_QUERY)
        private val activeQuery = MutableStateFlow(DEFAULT_QUERY)
        private val sortOption = MutableStateFlow(BookSortOption.RELEVANCE)
        private val favoritesOnly = MutableStateFlow(false)

        val uiState: StateFlow<BookListUiState.Content> =
            combine(
                queryInput,
                activeQuery,
                sortOption,
                favoritesOnly,
                repository.observeSearchHistory(),
            ) { queryText, currentQuery, currentSort, favoritesFilter, history ->
                BookListUiState.Content(
                    queryInput = queryText,
                    activeQuery = currentQuery,
                    sortOption = currentSort,
                    favoritesOnly = favoritesFilter,
                    searchHistory = history,
                    favoriteCount = 0,
                    isSignedIn = false,
                )
            }.combine(
                repository.observeFavoriteCount(),
            ) { currentState, favoriteCount ->
                currentState.copy(
                    favoriteCount = favoriteCount,
                )
            }.combine(
                authRepository.authState,
            ) { currentState, authState ->
                currentState.copy(
                    isSignedIn = authState is AuthState.SignedIn,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    BookListUiState.Content(
                        queryInput = DEFAULT_QUERY,
                        activeQuery = DEFAULT_QUERY,
                        sortOption = BookSortOption.RELEVANCE,
                        favoritesOnly = false,
                        searchHistory = emptyList(),
                        favoriteCount = 0,
                        isSignedIn = false,
                    ),
            )

        val pagedBooks: Flow<PagingData<Book>> =
            combine(
                activeQuery,
                sortOption,
                favoritesOnly,
            ) { query, sort, favorites ->
                Triple(query, sort, favorites)
            }.distinctUntilChanged()
                .flatMapLatest { (query, sort, favorites) ->
                    repository.pagedBooks(
                        query = query,
                        sortOption = sort,
                        favoritesOnly = favorites,
                    )
                }.cachedIn(viewModelScope)

        init {
            submitSearch(DEFAULT_QUERY)
        }

        fun onQueryInputChanged(newValue: String) {
            queryInput.value = newValue
        }

        fun submitSearch(rawQuery: String = queryInput.value) {
            val trimmedQuery = rawQuery.trim()
            val normalizedQuery =
                if (favoritesOnly.value) {
                    trimmedQuery
                } else {
                    trimmedQuery.ifBlank { DEFAULT_QUERY }
            }
            queryInput.value = normalizedQuery
            activeQuery.value = normalizedQuery
            if (normalizedQuery.isNotBlank() || !favoritesOnly.value) {
                viewModelScope.launch {
                    repository.saveSearchQuery(normalizedQuery)
                }
            }
        }

        fun onSearchHistoryClicked(query: String) {
            submitSearch(query)
        }

        fun onSortOptionSelected(newSortOption: BookSortOption) {
            sortOption.value = newSortOption
        }

        fun onFavoritesFilterChanged(enabled: Boolean) {
            favoritesOnly.value = enabled
        }

        fun prepareFavoritesTab() {
            favoritesOnly.value = true
            if (activeQuery.value.equals(DEFAULT_QUERY, ignoreCase = true)) {
                queryInput.value = ""
                activeQuery.value = ""
            }
        }

        fun onFavoriteClicked(book: Book) {
            viewModelScope.launch {
                repository.toggleFavorite(
                    bookId = book.id,
                    currentlyFavorite = book.isFavorite,
                )
            }
        }

        fun clearSearchHistory() {
            viewModelScope.launch {
                repository.clearSearchHistory()
            }
        }
    }
