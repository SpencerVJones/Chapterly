package com.example.chapterly.ui.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.example.chapterly.MainDispatcherRule
import com.example.chapterly.data.repository.BookRepository
import com.example.chapterly.model.Book
import com.example.chapterly.model.BookSortOption
import com.example.chapterly.ui.navigation.BOOK_ID_ARG
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startsInLoadingState() {
        val repository = FakeBookRepository()
        val viewModel = buildViewModel(repository)

        assertEquals(BookDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun emitsSuccessWhenBookIsAvailable() =
        runTest {
            val repository =
                FakeBookRepository(
                    seedBook = sampleBook(),
                )
            val viewModel = buildViewModel(repository)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is BookDetailUiState.Success)
            assertEquals(sampleBook().id, (state as BookDetailUiState.Success).book.id)
        }

    @Test
    fun emitsErrorWhenRefreshFailsAndNoCacheExists() =
        runTest {
            val repository =
                FakeBookRepository(
                    shouldThrowOnRefresh = true,
                )
            val viewModel = buildViewModel(repository)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is BookDetailUiState.Error)
            assertEquals("Unable to load book details.", (state as BookDetailUiState.Error).message)
        }

    private fun buildViewModel(repository: FakeBookRepository): BookDetailViewModel {
        return BookDetailViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf(BOOK_ID_ARG to sampleBook().id),
                ),
            repository = repository,
        )
    }
}

private class FakeBookRepository(
    seedBook: Book? = null,
    private val shouldThrowOnRefresh: Boolean = false,
) : BookRepository {
    private val bookFlow = MutableStateFlow(seedBook)

    override fun pagedBooks(
        query: String,
        sortOption: BookSortOption,
        favoritesOnly: Boolean,
    ): Flow<PagingData<Book>> = flowOf(PagingData.empty())

    override fun observeBook(bookId: String): Flow<Book?> = bookFlow

    override fun observeSearchHistory(limit: Int): Flow<List<String>> = flowOf(emptyList())

    override suspend fun saveSearchQuery(query: String) = Unit

    override suspend fun clearSearchHistory() = Unit

    override suspend fun toggleFavorite(
        bookId: String,
        currentlyFavorite: Boolean,
    ) {
        val current = bookFlow.value ?: return
        if (current.id == bookId) {
            bookFlow.value = current.copy(isFavorite = !currentlyFavorite)
        }
    }

    override suspend fun refreshBookDetail(bookId: String) {
        if (shouldThrowOnRefresh) {
            throw IOException("Network error")
        }
        if (bookFlow.value == null) {
            bookFlow.value = sampleBook()
        }
    }
}

private fun sampleBook(): Book =
    Book(
        id = "book-1",
        title = "Compose for Android",
        authors = listOf("Jane Developer"),
        description = "A practical guide to Jetpack Compose.",
        thumbnailUrl = "https://images.example.com/book-1.jpg",
        previewLink = "https://books.google.com/book-1",
        publishedDate = "2025-01-01",
        averageRating = 4.5,
        isFavorite = false,
    )
