package com.example.chapterly.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.chapterly.model.Book
import com.example.chapterly.model.BookSortOption
import com.example.chapterly.ui.bookdetail.BookDetailScreen
import com.example.chapterly.ui.bookdetail.BookDetailUiState
import com.example.chapterly.ui.booklist.BOOK_CARD_PREFIX
import com.example.chapterly.ui.booklist.BookListScreen
import com.example.chapterly.ui.booklist.BookListUiState
import com.example.chapterly.ui.booklist.FAVORITES_FILTER_TAG
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookListAndDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun favoritesFilterChip_invokesCallback() {
        var favoritesEnabled: Boolean? = null

        composeRule.setContent {
            testBookListScreen(
                booksFlow = flowOf(PagingData.from(sampleBooks())),
                onFavoritesFilterChanged = { favoritesEnabled = it },
                onBookClick = {},
                onFavoriteClicked = {},
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
            testBookListScreen(
                booksFlow = flowOf(PagingData.from(sampleBooks())),
                onFavoritesFilterChanged = {},
                onBookClick = { clickedBookId = it },
                onFavoriteClicked = {},
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
                onRetry = { retried = true },
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
private fun testBookListScreen(
    booksFlow: Flow<PagingData<Book>>,
    onFavoritesFilterChanged: (Boolean) -> Unit,
    onBookClick: (String) -> Unit,
    onFavoriteClicked: (Book) -> Unit,
) {
    val lazyBooks = booksFlow.collectAsLazyPagingItems()
    MaterialTheme {
        BookListScreen(
            uiState =
                BookListUiState.Content(
                    queryInput = "android",
                    activeQuery = "android",
                    sortOption = BookSortOption.RELEVANCE,
                    favoritesOnly = false,
                    searchHistory = listOf("compose", "kotlin"),
                ),
            books = lazyBooks,
            onQueryInputChanged = {},
            onSearchSubmitted = {},
            onSortOptionSelected = {},
            onFavoritesFilterChanged = onFavoritesFilterChanged,
            onSearchHistoryClicked = {},
            onClearSearchHistory = {},
            onFavoriteClicked = onFavoriteClicked,
            onBookClick = onBookClick,
        )
    }
}

private fun sampleBooks(): List<Book> =
    listOf(
        Book(
            id = "book-1",
            title = "Compose in Action",
            authors = listOf("Alex Android"),
            description = "Compose patterns and architecture.",
            thumbnailUrl = "",
            previewLink = "https://books.google.com/book-1",
            publishedDate = "2024",
            averageRating = 4.2,
            isFavorite = false,
        ),
        Book(
            id = "book-2",
            title = "Kotlin for Mobile",
            authors = listOf("Taylor Kotlin"),
            description = "Modern Kotlin techniques for Android.",
            thumbnailUrl = "",
            previewLink = "https://books.google.com/book-2",
            publishedDate = "2023",
            averageRating = 4.0,
            isFavorite = false,
        ),
    )
