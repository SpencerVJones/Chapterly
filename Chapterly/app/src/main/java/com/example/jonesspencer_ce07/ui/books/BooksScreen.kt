package com.example.jonesspencer_ce07.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.jonesspencer_ce07.model.Book
import com.example.jonesspencer_ce07.model.SortOption

const val SEARCH_FIELD_TAG = "search_field"
const val FAVORITES_FILTER_TAG = "favorites_filter_chip"
const val BOOK_CARD_PREFIX = "book_card_"
const val HISTORY_PREFIX = "history_"

@Composable
fun BooksRoute(
    onBookClick: (String) -> Unit,
    viewModel: BooksListViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagedBooks = viewModel.pagedBooks.collectAsLazyPagingItems()

    BooksScreen(
        uiState = uiState,
        books = pagedBooks,
        onQueryInputChanged = viewModel::onQueryInputChanged,
        onSearchSubmitted = viewModel::submitSearch,
        onSortOptionSelected = viewModel::onSortOptionSelected,
        onFavoritesFilterChanged = viewModel::onFavoritesFilterChanged,
        onSearchHistoryClicked = viewModel::onSearchHistoryClicked,
        onClearSearchHistory = viewModel::clearSearchHistory,
        onFavoriteClicked = viewModel::onFavoriteClicked,
        onBookClick = onBookClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    uiState: BooksUiState.Content,
    books: LazyPagingItems<Book>,
    onQueryInputChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onFavoritesFilterChanged: (Boolean) -> Unit,
    onSearchHistoryClicked: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onFavoriteClicked: (Book) -> Unit,
    onBookClick: (String) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(text = "Google Books Explorer") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(SEARCH_FIELD_TAG)
                        .semantics { contentDescription = "Search books input" },
                    value = uiState.queryInput,
                    onValueChange = onQueryInputChanged,
                    label = { Text("Search books") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onSearchSubmitted(uiState.queryInput) },
                    modifier = Modifier.semantics { contentDescription = "Search books" }
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AssistChip(
                        onClick = { sortMenuExpanded = true },
                        label = { Text("Sort: ${uiState.sortOption.label}") }
                    )
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    sortMenuExpanded = false
                                    onSortOptionSelected(option)
                                }
                            )
                        }
                    }
                }

                FilterChip(
                    selected = uiState.favoritesOnly,
                    onClick = { onFavoritesFilterChanged(!uiState.favoritesOnly) },
                    label = { Text("Favorites") },
                    modifier = Modifier
                        .testTag(FAVORITES_FILTER_TAG)
                        .semantics { contentDescription = "Favorites filter" }
                )
            }

            if (uiState.searchHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent searches",
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(onClick = onClearSearchHistory) {
                        Text("Clear")
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(uiState.searchHistory) { query ->
                        AssistChip(
                            onClick = { onSearchHistoryClicked(query) },
                            label = { Text(query, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier
                                .testTag("$HISTORY_PREFIX$query")
                                .semantics { contentDescription = "History $query" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    books.loadState.refresh is LoadState.Loading && books.itemCount == 0 -> {
                        PlaceholderGrid()
                    }
                    books.loadState.refresh is LoadState.Error && books.itemCount == 0 -> {
                        val message = (books.loadState.refresh as LoadState.Error).error.message
                            ?: "Something went wrong."
                        ErrorState(
                            message = message,
                            onRetry = { books.retry() }
                        )
                    }
                    books.itemCount == 0 -> {
                        EmptyState(
                            favoritesOnly = uiState.favoritesOnly,
                            query = uiState.activeQuery
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                count = books.itemCount,
                                key = { index -> books[index]?.id ?: index }
                            ) { index ->
                                val book = books[index] ?: return@items
                                BookCard(
                                    book = book,
                                    onClick = { onBookClick(book.id) },
                                    onFavoriteClick = { onFavoriteClicked(book) }
                                )
                            }

                            if (books.loadState.append is LoadState.Loading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("$BOOK_CARD_PREFIX${book.id}")
            .semantics { contentDescription = "Book card ${book.id}" }
    ) {
        Column {
            if (book.thumbnailUrl.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No image")
                }
            } else {
                AsyncImage(
                    model = book.thumbnailUrl,
                    contentDescription = "Book cover ${book.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = book.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.semantics { contentDescription = "Toggle favorite ${book.id}" }
                ) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null
                    )
                }
            }
            Text(
                text = book.authors.joinToString().ifBlank { "Unknown author" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Text(
                text = book.publishedDate,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PlaceholderGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    favoritesOnly: Boolean,
    query: String
) {
    val message = if (favoritesOnly) {
        "No favorite books match your query yet."
    } else {
        "No books found for \"$query\"."
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
