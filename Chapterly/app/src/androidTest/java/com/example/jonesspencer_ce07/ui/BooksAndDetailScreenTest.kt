package com.example.jonesspencer_ce07.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.jonesspencer_ce07.model.Book
import com.example.jonesspencer_ce07.model.SortOption
import com.example.jonesspencer_ce07.ui.books.BOOK_CARD_PREFIX
import com.example.jonesspencer_ce07.ui.books.BooksScreen
import com.example.jonesspencer_ce07.ui.books.BooksUiState
import com.example.jonesspencer_ce07.ui.books.FAVORITES_FILTER_TAG
import com.example.jonesspencer_ce07.ui.detail.BookDetailScreen
import com.example.jonesspencer_ce07.ui.detail.BookDetailUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class BooksAndDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun favoritesFilterChip_invokesCallback() {
        var favoritesEnabled: Boolean? = null

        composeRule.setContent {
            TestBooksScreen(
                booksFlow = flowOf(PagingData.from(sampleBooks())),
                onFavoritesFilterChanged = { favoritesEnabled = it },
                onBookClick = {},
                onFavoriteClicked = {}
            )
        }

        composeRule.onNodeWithTag(FAVORITES_FILTER_TAG).performClick()

        composeRule.runOnIdle {
            assertEquals(true, favoritesEnabled)
        }
    }

    @Test
    fun clickingBookCard_invokesBookClickCallback() {
        var clickedBookId: String? = null
        val book = sampleBooks().first()

        composeRule.setContent {
            TestBooksScreen(
                booksFlow = flowOf(PagingData.from(sampleBooks())),
                onFavoritesFilterChanged = {},
                onBookClick = { clickedBookId = it },
                onFavoriteClicked = {}
            )
        }

        composeRule.onNodeWithTag("$BOOK_CARD_PREFIX${book.id}").performClick()

        composeRule.runOnIdle {
            assertEquals(book.id, clickedBookId)
        }
    }

    @Test
    fun detailErrorState_showsRetryAndInvokesCallback() {
        var retried = false

        composeRule.setContent {
            BookDetailScreen(
                uiState = BookDetailUiState.Error("Unable to load book details."),
                onBackClicked = {},
                onFavoriteClicked = {},
                onRetry = { retried = true }
            )
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        composeRule.runOnIdle {
            assertTrue(retried)
        }
    }
}

@Composable
private fun TestBooksScreen(
    booksFlow: Flow<PagingData<Book>>,
    onFavoritesFilterChanged: (Boolean) -> Unit,
    onBookClick: (String) -> Unit,
    onFavoriteClicked: (Book) -> Unit
) {
    val lazyBooks = booksFlow.collectAsLazyPagingItems()
    MaterialTheme {
        BooksScreen(
            uiState = BooksUiState.Content(
                queryInput = "android",
                activeQuery = "android",
                sortOption = SortOption.RELEVANCE,
                favoritesOnly = false,
                searchHistory = listOf("compose", "kotlin")
            ),
            books = lazyBooks,
            onQueryInputChanged = {},
            onSearchSubmitted = {},
            onSortOptionSelected = {},
            onFavoritesFilterChanged = onFavoritesFilterChanged,
            onSearchHistoryClicked = {},
            onClearSearchHistory = {},
            onFavoriteClicked = onFavoriteClicked,
            onBookClick = onBookClick
        )
    }
}

private fun sampleBooks(): List<Book> = listOf(
    Book(
        id = "book-1",
        title = "Compose in Action",
        authors = listOf("Alex Android"),
        description = "Compose patterns and architecture.",
        thumbnailUrl = "",
        previewLink = "https://books.google.com/book-1",
        publishedDate = "2024",
        isFavorite = false
    ),
    Book(
        id = "book-2",
        title = "Kotlin for Mobile",
        authors = listOf("Taylor Kotlin"),
        description = "Modern Kotlin techniques for Android.",
        thumbnailUrl = "",
        previewLink = "https://books.google.com/book-2",
        publishedDate = "2023",
        isFavorite = false
    )
)
