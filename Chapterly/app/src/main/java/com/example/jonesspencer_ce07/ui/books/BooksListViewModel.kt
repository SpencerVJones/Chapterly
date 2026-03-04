package com.example.jonesspencer_ce07.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.jonesspencer_ce07.data.repository.BooksRepository
import com.example.jonesspencer_ce07.data.repository.DEFAULT_QUERY
import com.example.jonesspencer_ce07.model.Book
import com.example.jonesspencer_ce07.model.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BooksListViewModel @Inject constructor(
    private val repository: BooksRepository
) : ViewModel() {

    private val queryInput = MutableStateFlow(DEFAULT_QUERY)
    private val activeQuery = MutableStateFlow(DEFAULT_QUERY)
    private val sortOption = MutableStateFlow(SortOption.RELEVANCE)
    private val favoritesOnly = MutableStateFlow(false)

    val uiState: StateFlow<BooksUiState.Content> = combine(
        queryInput,
        activeQuery,
        sortOption,
        favoritesOnly,
        repository.observeSearchHistory()
    ) { queryText, currentQuery, currentSort, favoritesFilter, history ->
        BooksUiState.Content(
            queryInput = queryText,
            activeQuery = currentQuery,
            sortOption = currentSort,
            favoritesOnly = favoritesFilter,
            searchHistory = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BooksUiState.Content(
            queryInput = DEFAULT_QUERY,
            activeQuery = DEFAULT_QUERY,
            sortOption = SortOption.RELEVANCE,
            favoritesOnly = false,
            searchHistory = emptyList()
        )
    )

    val pagedBooks: Flow<PagingData<Book>> = combine(
        activeQuery,
        sortOption,
        favoritesOnly
    ) { query, sort, favorites ->
        Triple(query, sort, favorites)
    }.distinctUntilChanged()
        .flatMapLatest { (query, sort, favorites) ->
            repository.pagedBooks(
                query = query,
                sortOption = sort,
                favoritesOnly = favorites
            )
        }.cachedIn(viewModelScope)

    init {
        submitSearch(DEFAULT_QUERY)
    }

    fun onQueryInputChanged(newValue: String) {
        queryInput.value = newValue
    }

    fun submitSearch(rawQuery: String = queryInput.value) {
        val normalizedQuery = rawQuery.trim().ifBlank { DEFAULT_QUERY }
        queryInput.value = normalizedQuery
        activeQuery.value = normalizedQuery
        viewModelScope.launch {
            repository.saveSearchQuery(normalizedQuery)
        }
    }

    fun onSearchHistoryClicked(query: String) {
        submitSearch(query)
    }

    fun onSortOptionSelected(newSortOption: SortOption) {
        sortOption.value = newSortOption
    }

    fun onFavoritesFilterChanged(enabled: Boolean) {
        favoritesOnly.value = enabled
    }

    fun onFavoriteClicked(book: Book) {
        viewModelScope.launch {
            repository.toggleFavorite(
                bookId = book.id,
                currentlyFavorite = book.isFavorite
            )
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }
}
