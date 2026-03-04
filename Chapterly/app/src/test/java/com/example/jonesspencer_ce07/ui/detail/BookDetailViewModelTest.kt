package com.example.jonesspencer_ce07.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.example.jonesspencer_ce07.MainDispatcherRule
import com.example.jonesspencer_ce07.data.repository.BooksRepository
import com.example.jonesspencer_ce07.model.Book
import com.example.jonesspencer_ce07.model.SortOption
import com.example.jonesspencer_ce07.ui.navigation.BOOK_ID_ARG
import java.io.IOException
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

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startsInLoadingState() {
        val repository = FakeBooksRepository()
        val viewModel = buildViewModel(repository)

        assertEquals(BookDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun emitsSuccessWhenBookIsAvailable() = runTest {
        val repository = FakeBooksRepository(
            seedBook = sampleBook()
        )
        val viewModel = buildViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is BookDetailUiState.Success)
        assertEquals(sampleBook().id, (state as BookDetailUiState.Success).book.id)
    }

    @Test
    fun emitsErrorWhenRefreshFailsAndNoCacheExists() = runTest {
        val repository = FakeBooksRepository(
            shouldThrowOnRefresh = true
        )
        val viewModel = buildViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is BookDetailUiState.Error)
        assertEquals("Unable to load book details.", (state as BookDetailUiState.Error).message)
    }

    private fun buildViewModel(repository: FakeBooksRepository): BookDetailViewModel {
        return BookDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(BOOK_ID_ARG to sampleBook().id)
            ),
            repository = repository
        )
    }
}

private class FakeBooksRepository(
    seedBook: Book? = null,
    private val shouldThrowOnRefresh: Boolean = false
) : BooksRepository {
    private val bookFlow = MutableStateFlow(seedBook)

    override fun pagedBooks(
        query: String,
        sortOption: SortOption,
        favoritesOnly: Boolean
    ): Flow<PagingData<Book>> = flowOf(PagingData.empty())

    override fun observeBook(bookId: String): Flow<Book?> = bookFlow

    override fun observeSearchHistory(limit: Int): Flow<List<String>> = flowOf(emptyList())

    override suspend fun saveSearchQuery(query: String) = Unit

    override suspend fun clearSearchHistory() = Unit

    override suspend fun toggleFavorite(bookId: String, currentlyFavorite: Boolean) {
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

private fun sampleBook(): Book = Book(
    id = "book-1",
    title = "Compose for Android",
    authors = listOf("Jane Developer"),
    description = "A practical guide to Jetpack Compose.",
    thumbnailUrl = "https://images.example.com/book-1.jpg",
    previewLink = "https://books.google.com/book-1",
    publishedDate = "2025-01-01",
    isFavorite = false
)
