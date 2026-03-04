package com.example.chapterly.ui.booklist

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.chapterly.model.Book
import com.example.chapterly.model.BookSortOption
import java.util.Locale

const val SEARCH_FIELD_TAG = "search_field"
const val FAVORITES_FILTER_TAG = "favorites_filter_chip"
const val BOOK_CARD_PREFIX = "book_card_"
const val HISTORY_PREFIX = "history_"

private val ScreenLightBackground = Color(0xFFF5F2FA)
private val ScreenDarkBackground = Color(0xFF111422)
private val PanelLight = Color(0xFFEFEAF6)
private val PanelDark = Color(0xFF242A43)
private val ChipBorderLight = Color(0xFFCDC7DD)
private val ChipBorderDark = Color(0xFF3A4262)
private val SelectedChipLight = Color(0xFFDAD3F6)
private val SelectedChipDark = Color(0xFF2C397D)
private val CardLight = Color(0xFFF2EFF8)
private val CardDark = Color(0xFF1D2237)
private val MetaLight = Color(0xFF676377)
private val MetaDark = Color(0xFFBFC3DD)
private val BookmarkRibbon = Color(0xFF6B58CC)

@Suppress("FunctionName")
@Composable
fun BookListRoute(
    onBookClick: (String) -> Unit,
    viewModel: BookListViewModel,
    screenTitle: String = "Chapterly",
    defaultFavoritesOnly: Boolean? = null,
    lockFavoritesFilter: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagedBooks = viewModel.pagedBooks.collectAsLazyPagingItems()

    LaunchedEffect(defaultFavoritesOnly) {
        when (defaultFavoritesOnly) {
            true -> viewModel.prepareFavoritesTab()
            false -> viewModel.onFavoritesFilterChanged(false)
            null -> Unit
        }
    }

    BookListScreen(
        uiState = uiState,
        books = pagedBooks,
        onQueryInputChanged = viewModel::onQueryInputChanged,
        onSearchSubmitted = viewModel::submitSearch,
        onSortOptionSelected = viewModel::onSortOptionSelected,
        onFavoritesFilterChanged = viewModel::onFavoritesFilterChanged,
        onSearchHistoryClicked = viewModel::onSearchHistoryClicked,
        onClearSearchHistory = viewModel::clearSearchHistory,
        onFavoriteClicked = viewModel::onFavoriteClicked,
        onBookClick = onBookClick,
        screenTitle = screenTitle,
        lockFavoritesFilter = lockFavoritesFilter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    uiState: BookListUiState.Content,
    books: LazyPagingItems<Book>,
    onQueryInputChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onSortOptionSelected: (BookSortOption) -> Unit,
    onFavoritesFilterChanged: (Boolean) -> Unit,
    onSearchHistoryClicked: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onFavoriteClicked: (Book) -> Unit,
    onBookClick: (String) -> Unit,
    screenTitle: String = "Chapterly",
    lockFavoritesFilter: Boolean = false,
) {
    val darkMode = isSystemInDarkTheme()
    val pageGradient =
        if (darkMode) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        ScreenDarkBackground,
                        Color(0xFF12172A),
                        Color(0xFF0B0E1B),
                    ),
            )
        } else {
            Brush.verticalGradient(
                colors =
                    listOf(
                        ScreenLightBackground,
                        Color(0xFFF1EDF8),
                    ),
            )
        }

    val elevatedPanel = if (darkMode) PanelDark else PanelLight
    val cardColor = if (darkMode) CardDark else CardLight
    val cardMetaColor = if (darkMode) MetaDark else MetaLight
    val chipContainer = if (darkMode) Color(0xFF2A2F49) else Color(0xFFEAE5F3)
    val selectedChip = if (darkMode) SelectedChipDark else SelectedChipLight
    val selectedChipText = if (darkMode) Color(0xFFEFF2FF) else Color(0xFF1F2758)
    val unselectedChipText = if (darkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF4C4C63)
    val chipBorder = if (darkMode) ChipBorderDark else ChipBorderLight

    val isRefreshing = books.loadState.refresh is LoadState.Loading && books.itemCount > 0
    val pullRefreshState = rememberPullToRefreshState()
    val shimmerBrush = rememberShimmerBrush()

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            books.refresh()
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(pageGradient),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = screenTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))

                if (lockFavoritesFilter) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "Favorites",
                        tint = BookmarkRibbon,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    IconButton(
                        onClick = { onFavoritesFilterChanged(!uiState.favoritesOnly) },
                        modifier =
                            Modifier
                                .size(40.dp)
                                .testTag(FAVORITES_FILTER_TAG)
                                .semantics {
                                    contentDescription =
                                        if (uiState.favoritesOnly) {
                                            "Show all books"
                                        } else {
                                            "Show favorites only"
                                        }
                                },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor =
                                    if (uiState.favoritesOnly) {
                                        BookmarkRibbon
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            ),
                    ) {
                        Icon(
                            imageVector =
                                if (uiState.favoritesOnly) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                            contentDescription = null,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = uiState.queryInput,
                onValueChange = onQueryInputChanged,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .testTag(SEARCH_FIELD_TAG)
                        .semantics { contentDescription = "Search books input" },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(
                        onSearch = { onSearchSubmitted(uiState.queryInput) },
                    ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (uiState.queryInput.isNotBlank()) {
                        IconButton(onClick = { onQueryInputChanged("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear query",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = elevatedPanel,
                        unfocusedContainerColor = elevatedPanel,
                        disabledContainerColor = elevatedPanel,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BookSortOption.entries.forEach { option ->
                    SortPillChip(
                        text = option.label,
                        selected = uiState.sortOption == option,
                        onClick = { onSortOptionSelected(option) },
                        selectedContainer = selectedChip,
                        unselectedContainer = chipContainer,
                        selectedText = selectedChipText,
                        unselectedText = unselectedChipText,
                        borderColor = chipBorder,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                ActionCircleButton(
                    onClick = onClearSearchHistory,
                    containerColor = chipContainer,
                    borderColor = Color.Transparent,
                    size = 52.dp,
                    modifier = Modifier.semantics { contentDescription = "Clear search history" },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = unselectedChipText,
                    )
                }
            }

            if (uiState.searchHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent searches",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = onClearSearchHistory) {
                        Text(
                            text = "Clear",
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(uiState.searchHistory.take(3)) { query ->
                        Surface(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onSearchHistoryClicked(query) }
                                    .testTag("$HISTORY_PREFIX$query")
                                    .semantics { contentDescription = "History $query" },
                            color = chipContainer,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text(
                                text = query,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (uiState.searchHistory.size > 3) {
                        item {
                            Surface(
                                color = chipContainer,
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text(
                                    text = ".....",
                                    color = unselectedChipText,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(pullRefreshState.nestedScrollConnection),
            ) {
                when {
                    books.loadState.refresh is LoadState.Loading && books.itemCount == 0 -> {
                        PlaceholderGrid(
                            cardColor = cardColor,
                            shimmerBrush = shimmerBrush,
                        )
                    }
                    books.loadState.refresh is LoadState.Error && books.itemCount == 0 -> {
                        val message =
                            (books.loadState.refresh as LoadState.Error).error.message
                                ?: "Something went wrong."
                        ErrorState(
                            message = message,
                            onRetry = { books.retry() },
                        )
                    }
                    books.itemCount == 0 -> {
                        EmptyState(
                            favoritesOnly = uiState.favoritesOnly,
                            query = uiState.activeQuery,
                            favoriteCount = uiState.favoriteCount,
                            isSignedIn = uiState.isSignedIn,
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 26.dp),
                        ) {
                            items(
                                count = books.itemCount,
                                key = { index -> books[index]?.id ?: index },
                            ) { index ->
                                val book = books[index] ?: return@items
                                BookCard(
                                    book = book,
                                    cardColor = cardColor,
                                    cardMetaColor = cardMetaColor,
                                    onClick = { onBookClick(book.id) },
                                    onFavoriteClick = { onFavoriteClicked(book) },
                                )
                            }

                            if (books.loadState.append is LoadState.Loading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(26.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                PullToRefreshContainer(
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    cardColor: Color,
    cardMetaColor: Color,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("$BOOK_CARD_PREFIX${book.id}")
                .semantics { contentDescription = "Book card ${book.id}" },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            if (book.thumbnailUrl.isBlank()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(182.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No image",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                AsyncImage(
                    model = book.thumbnailUrl,
                    contentDescription = "Book cover ${book.title}",
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(182.dp),
                )
            }

            Surface(
                onClick = onFavoriteClick,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 0.dp, end = 12.dp)
                        .size(width = 28.dp, height = 40.dp)
                        .semantics { contentDescription = "Toggle favorite ${book.id}" },
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                color = BookmarkRibbon,
            ) {
                Icon(
                    imageVector =
                        if (book.isFavorite) {
                            Icons.Filled.Bookmark
                        } else {
                            Icons.Filled.BookmarkBorder
                        },
                    tint = Color.White,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.authors.joinToString().ifBlank { "Unknown author" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = cardMetaColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            BookMetaLine(
                book = book,
                color = cardMetaColor,
            )
        }
    }
}

@Composable
private fun BookMetaLine(
    book: Book,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (book.averageRating != null) {
            val filledStars = book.averageRating.toInt().coerceIn(1, 5)
            repeat(filledStars) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFE5B33B),
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text =
                buildAnnotatedString {
                    append(book.publishedDate)
                    append(" • ")
                    append(
                        book.averageRating?.let { String.format(Locale.US, "%.1f rating", it) }
                            ?: "unrated",
                    )
                },
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaceholderGrid(
    cardColor: Color,
    shimmerBrush: Brush,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(6) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(172.dp)
                                .background(
                                    brush = shimmerBrush,
                                    shape = RoundedCornerShape(14.dp),
                                ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .background(
                                    brush = shimmerBrush,
                                    shape = RoundedCornerShape(8.dp),
                                ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.72f)
                                .height(12.dp)
                                .background(
                                    brush = shimmerBrush,
                                    shape = RoundedCornerShape(8.dp),
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer_offset",
    )

    return Brush.linearGradient(
        colors =
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
            ),
        start = Offset(translateAnimation - 240f, 0f),
        end = Offset(translateAnimation, 240f),
    )
}

@Composable
private fun EmptyState(
    favoritesOnly: Boolean,
    query: String,
    favoriteCount: Int,
    isSignedIn: Boolean,
) {
    if (favoritesOnly) {
        val title =
            if (favoriteCount > 0) {
                "No favorites match this filter"
            } else {
                "No favorite books yet"
            }
        val subtitle =
            when {
                favoriteCount > 0 -> "Try a different search, and your saved books will show up here."
                isSignedIn -> "Save books you love to this list, and they\u2019ll show up here."
                else -> "Sign in to sync favorites across devices, and your saved books will show up here."
            }

        FavoritesEmptyState(
            title = title,
            subtitle = subtitle,
        )
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No books found for \"$query\".",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun FavoritesEmptyState(
    title: String,
    subtitle: String,
) {
    val illustrationOrb = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val illustrationBase = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val illustrationBook = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
    val illustrationPage = MaterialTheme.colorScheme.surface
    val sparkle = Color(0xFFE5A93C)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(170.dp),
                    shape = CircleShape,
                    color = illustrationOrb,
                ) {}

                Surface(
                    modifier =
                        Modifier
                            .width(170.dp)
                            .height(32.dp)
                            .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(50),
                    color = illustrationBase,
                ) {}

                Surface(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .align(Alignment.TopCenter),
                    shape = CircleShape,
                    color = sparkle.copy(alpha = 0.14f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = sparkle,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Surface(
                    modifier =
                        Modifier
                            .width(150.dp)
                            .height(100.dp)
                            .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(20.dp),
                    color = illustrationBook,
                    shadowElevation = 4.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier =
                                Modifier
                                    .width(110.dp)
                                    .height(74.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = illustrationPage,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f),
            )
        }
    }
}

@Composable
private fun SortPillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedContainer: Color,
    unselectedContainer: Color,
    selectedText: Color,
    unselectedText: Color,
    borderColor: Color,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) selectedContainer else unselectedContainer,
        border =
            if (selected) {
                null
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = if (selected) selectedText else unselectedText,
        )
    }
}

@Composable
private fun ActionCircleButton(
    onClick: () -> Unit,
    containerColor: Color,
    borderColor: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = containerColor,
        border =
            if (borderColor == Color.Transparent) {
                null
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
