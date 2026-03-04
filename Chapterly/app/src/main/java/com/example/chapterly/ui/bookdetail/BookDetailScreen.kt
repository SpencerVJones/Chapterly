package com.example.chapterly.ui.bookdetail

import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.chapterly.model.Book
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@Composable
fun BookDetailRoute(
    onBackClicked: () -> Unit,
    viewModel: BookDetailViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val readingUiState by viewModel.readingUiState.collectAsStateWithLifecycle()
    BookDetailScreen(
        uiState = uiState,
        readingUiState = readingUiState,
        onBackClicked = onBackClicked,
        onFavoriteClicked = viewModel::onFavoriteClicked,
        onProgressPercentChanged = viewModel::onProgressPercentChanged,
        onProgressMinutesChanged = viewModel::onProgressMinutesChanged,
        onProgressChaptersChanged = viewModel::onProgressChaptersChanged,
        onSaveProgress = viewModel::saveProgress,
        onNoteChanged = viewModel::onNoteChanged,
        onSaveNote = viewModel::saveNote,
        onRetry = viewModel::loadBook,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    uiState: BookDetailUiState,
    readingUiState: BookDetailReadingUiState,
    onBackClicked: () -> Unit,
    onFavoriteClicked: () -> Unit,
    onProgressPercentChanged: (String) -> Unit,
    onProgressMinutesChanged: (String) -> Unit,
    onProgressChaptersChanged: (String) -> Unit,
    onSaveProgress: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveNote: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier.semantics { contentDescription = "Back to books" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    val book = (uiState as? BookDetailUiState.Success)?.book
                    if (book != null) {
                        IconButton(
                            onClick = onFavoriteClicked,
                            modifier = Modifier.semantics { contentDescription = "Toggle detail favorite" },
                        ) {
                            Icon(
                                imageVector =
                                    if (book.isFavorite) {
                                        Icons.Default.Bookmark
                                    } else {
                                        Icons.Default.BookmarkBorder
                                    },
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            BookDetailUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is BookDetailUiState.Error -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.message,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }
            is BookDetailUiState.Success -> {
                BookDetailContent(
                    book = uiState.book,
                    readingUiState = readingUiState,
                    onProgressPercentChanged = onProgressPercentChanged,
                    onProgressMinutesChanged = onProgressMinutesChanged,
                    onProgressChaptersChanged = onProgressChaptersChanged,
                    onSaveProgress = onSaveProgress,
                    onNoteChanged = onNoteChanged,
                    onSaveNote = onSaveNote,
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun BookDetailContent(
    book: Book,
    readingUiState: BookDetailReadingUiState,
    onProgressPercentChanged: (String) -> Unit,
    onProgressMinutesChanged: (String) -> Unit,
    onProgressChaptersChanged: (String) -> Unit,
    onSaveProgress: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var descriptionExpanded by rememberSaveable { mutableStateOf(false) }
    val relatedQueries = rememberSaveable(book.id) { buildRelatedQueries(book) }

    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        if (book.thumbnailUrl.isBlank()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "No image")
            }
        } else {
            AsyncImage(
                model = book.thumbnailUrl,
                contentDescription = "Book cover detail ${book.title}",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = book.title, style = MaterialTheme.typography.headlineSmall)

        val ratingLabel = book.averageRating?.let { String.format(Locale.US, "%.1f", it) } ?: "Unrated"
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Published: ${book.publishedDate}   Rating: $ratingLabel",
            style = MaterialTheme.typography.bodySmall,
        )

        if (book.authors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Authors",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(book.authors) { author ->
                    AssistChip(
                        onClick = { openBookSearch(context, author) },
                        label = {
                            Text(
                                text = author,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Description",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = book.description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 6,
            overflow = TextOverflow.Ellipsis,
        )
        if (book.description.length > 220) {
            TextButton(
                onClick = { descriptionExpanded = !descriptionExpanded },
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(if (descriptionExpanded) "Show less" else "Read more")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (book.previewLink.isNotBlank()) {
                        openInCustomTab(context, book.previewLink)
                    }
                },
                enabled = book.previewLink.isNotBlank(),
                modifier = Modifier.semantics { contentDescription = "Open preview link" },
            ) {
                Text("Preview")
            }
            OutlinedButton(
                onClick = { shareBook(context, book) },
                modifier = Modifier.semantics { contentDescription = "Share book" },
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        InlineReadingControlsCard(
            readingUiState = readingUiState,
            onProgressPercentChanged = onProgressPercentChanged,
            onProgressMinutesChanged = onProgressMinutesChanged,
            onProgressChaptersChanged = onProgressChaptersChanged,
            onSaveProgress = onSaveProgress,
            onNoteChanged = onNoteChanged,
            onSaveNote = onSaveNote,
        )

        if (relatedQueries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Related books",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(relatedQueries) { query ->
                    AssistChip(
                        onClick = { openBookSearch(context, query) },
                        label = { Text(query, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineReadingControlsCard(
    readingUiState: BookDetailReadingUiState,
    onProgressPercentChanged: (String) -> Unit,
    onProgressMinutesChanged: (String) -> Unit,
    onProgressChaptersChanged: (String) -> Unit,
    onSaveProgress: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveNote: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Reading progress",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    readingUiState.savedProgress?.let { progress ->
                        "Tracking ${progress.percentComplete}% • ${progress.minutesRead} minutes • ${progress.chaptersCompleted} chapters"
                    } ?: "Save progress locally first. Firestore and backend sync run after that.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = readingUiState.progressPercentInput,
                    onValueChange = onProgressPercentChanged,
                    modifier = Modifier.weight(1f),
                    label = { Text("%") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = readingUiState.progressMinutesInput,
                    onValueChange = onProgressMinutesChanged,
                    modifier = Modifier.weight(1f),
                    label = { Text("Minutes") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = readingUiState.progressChaptersInput,
                    onValueChange = onProgressChaptersChanged,
                    modifier = Modifier.weight(1f),
                    label = { Text("Ch.") },
                    singleLine = true,
                )
            }
            Button(
                onClick = onSaveProgress,
                enabled = !readingUiState.isSavingProgress,
            ) {
                Text(if (readingUiState.isSavingProgress) "Saving..." else "Save progress")
            }

            Text(
                text = "Quick note",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = readingUiState.noteInput,
                onValueChange = onNoteChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note or highlight") },
                minLines = 2,
            )
            Button(
                onClick = onSaveNote,
                enabled = !readingUiState.isSavingNote,
            ) {
                Text(if (readingUiState.isSavingNote) "Saving..." else "Save note")
            }

            if (readingUiState.annotations.isNotEmpty()) {
                Text(
                    text = "Recent notes",
                    style = MaterialTheme.typography.labelLarge,
                )
                readingUiState.annotations.take(3).forEach { annotation ->
                    Text(
                        text = "\u2022 ${annotation.content}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            readingUiState.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun openInCustomTab(
    context: android.content.Context,
    url: String,
) {
    runCatching {
        CustomTabsIntent.Builder().build().launchUrl(context, android.net.Uri.parse(url))
    }
}

private fun shareBook(
    context: android.content.Context,
    book: Book,
) {
    val shareText =
        buildString {
            append(book.title)
            if (book.previewLink.isNotBlank()) {
                append("\n")
                append(book.previewLink)
            }
        }

    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, book.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

    context.startActivity(Intent.createChooser(intent, "Share book"))
}

private fun openBookSearch(
    context: android.content.Context,
    query: String,
) {
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
    val url = "https://books.google.com/books?q=$encoded"
    openInCustomTab(context, url)
}

private fun buildRelatedQueries(book: Book): List<String> {
    val base = mutableListOf<String>()
    base += book.authors
    base +=
        book.title
            .split(" ")
            .map { it.trim() }
            .filter { it.length > 3 }

    return base.distinct().take(5)
}
