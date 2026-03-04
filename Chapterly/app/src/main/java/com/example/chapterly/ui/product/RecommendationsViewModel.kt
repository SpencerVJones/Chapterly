package com.example.chapterly.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chapterly.data.repository.BookRepository
import com.example.chapterly.data.repository.ProductRepository
import com.example.chapterly.model.Book
import com.example.chapterly.model.Recommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecommendationUiItem(
    val recommendation: Recommendation,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
)

data class RecommendationsUiState(
    val items: List<RecommendationUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class RecommendationsViewModel
    @Inject
    constructor(
        private val productRepository: ProductRepository,
        private val bookRepository: BookRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecommendationsUiState())
        val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { current -> current.copy(isLoading = true, message = null) }
                runCatching { productRepository.getRecommendations() }
                    .onSuccess { recommendations ->
                        val items = buildRecommendationItems(recommendations)
                        _uiState.update { current ->
                            current.copy(
                                items = items,
                                isLoading = false,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                message = error.localizedMessage ?: "Unable to load recommendations.",
                            )
                        }
                    }
            }
        }

        private suspend fun buildRecommendationItems(
            recommendations: List<Recommendation>,
        ): List<RecommendationUiItem> {
            return coroutineScope {
                recommendations.map { recommendation ->
                    async {
                        val book = resolveBook(recommendation.bookId)
                        RecommendationUiItem(
                            recommendation = recommendation,
                            title = book?.title ?: "Book ${recommendation.bookId}",
                            subtitle =
                                buildString {
                                    if (!book?.authors.isNullOrEmpty()) {
                                        append(book?.authors?.joinToString(", "))
                                    } else {
                                        append("Recommended for your reading profile")
                                    }
                                    if (!book?.publishedDate.isNullOrBlank()) {
                                        append(" • ")
                                        append(book?.publishedDate)
                                    }
                                },
                            thumbnailUrl = book?.thumbnailUrl.orEmpty(),
                        )
                    }
                }.awaitAll()
            }
        }

        private suspend fun resolveBook(bookId: String): Book? {
            val cached = bookRepository.observeBook(bookId).firstOrNull()
            if (cached != null) {
                return cached
            }

            runCatching { bookRepository.refreshBookDetail(bookId) }
            return bookRepository.observeBook(bookId).firstOrNull()
        }
    }
