package com.example.chapterly.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.chapterly.model.Book
import kotlin.math.roundToInt

@Composable
fun ReadingHubRoute(
    onBackClicked: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: ReadingHubViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerBooks = viewModel.pickerBooks.collectAsLazyPagingItems()
    ProductScaffold(
        title = "Reading Hub",
        onBackClicked = onBackClicked,
    ) {
        if (uiState.isLoading) {
            LoadingPanel()
        } else {
            ProductScrollColumn {
                SummaryCard(
                    title = "Reading snapshot",
                    subtitle =
                        "Streak ${uiState.summary?.streakDays ?: 0} days • " +
                            "${uiState.summary?.totalMinutesRead ?: 0} minutes read",
                ) {
                    Text("Currently reading: ${uiState.summary?.currentlyReadingCount ?: 0}")
                    Text("Highlights: ${uiState.summary?.highlightsCount ?: 0} • Local notes: ${uiState.localAnnotationCount}")
                    Text("Bookmarks: ${uiState.summary?.bookmarksCount ?: 0} • Notes: ${uiState.summary?.notesCount ?: 0}")
                }

                SummaryCard(
                    title = "Daily goal",
                    subtitle = "Syncs to the backend and powers reminders.",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = uiState.goalMinutesInput,
                            onValueChange = viewModel::onGoalMinutesChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text("Minutes") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.goalPagesInput,
                            onValueChange = viewModel::onGoalPagesChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text("Pages") },
                            singleLine = true,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::saveGoal,
                        enabled = !uiState.isSaving,
                    ) {
                        Text("Save goal")
                    }
                }

                SummaryCard(
                    title = "Quick progress sync",
                    subtitle = "Writes locally first, then syncs to Firestore and the backend.",
                ) {
                    SelectedBookPickerField(
                        label = "Book for progress",
                        selectedBook = uiState.selectedProgressBook,
                        fallbackBookId = uiState.progressBookIdInput,
                        onPickClick = viewModel::openProgressBookPicker,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = uiState.progressPercentInput,
                            onValueChange = viewModel::onProgressPercentChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text("%") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.progressMinutesInput,
                            onValueChange = viewModel::onProgressMinutesChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text("Minutes") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.progressChaptersInput,
                            onValueChange = viewModel::onProgressChaptersChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text("Ch.") },
                            singleLine = true,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::saveProgress,
                        enabled = !uiState.isSaving,
                    ) {
                        Text("Sync progress")
                    }
                }

                SummaryCard(
                    title = "Quick note",
                    subtitle = "Offline-first annotation, then backend sync.",
                ) {
                    SelectedBookPickerField(
                        label = "Book for note",
                        selectedBook = uiState.selectedNoteBook,
                        fallbackBookId = uiState.noteBookIdInput,
                        onPickClick = viewModel::openNoteBookPicker,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.noteInput,
                        onValueChange = viewModel::onNoteChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note") },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::saveNote,
                        enabled = !uiState.isSaving,
                    ) {
                        Text("Save note")
                    }
                }

                if (uiState.currentlyReading.isNotEmpty()) {
                    SummaryCard(
                        title = "Currently reading",
                        subtitle = "This list is powered by Room and available offline.",
                    ) {
                        uiState.currentlyReading.forEach { progress ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BookThumbnail(
                                    title = progress.title,
                                    thumbnailUrl = progress.thumbnailUrl,
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = progress.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = progress.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text =
                                            "${progress.progress.percentComplete}% • " +
                                                "${progress.progress.minutesRead} min • " +
                                                "${progress.progress.chaptersCompleted} chapters",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Spacer(modifier = Modifier.size(10.dp))
                                Button(
                                    onClick = { onBookClick(progress.progress.bookId) },
                                ) {
                                    Text("Details")
                                }
                            }
                        }
                    }
                }

                uiState.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
    if (uiState.activeBookPickerTarget != null) {
        ProductBookPickerSheet(
            query = uiState.pickerQueryInput,
            books = pickerBooks,
            onQueryChanged = viewModel::onPickerQueryChanged,
            onDismiss = viewModel::dismissBookPicker,
            onBookSelected = viewModel::onPickerBookSelected,
        )
    }
}

@Composable
fun RecommendationsRoute(
    onBackClicked: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: RecommendationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProductScaffold(
        title = "Recommendations",
        onBackClicked = onBackClicked,
    ) {
        when {
            uiState.isLoading -> LoadingPanel()
            else -> {
                ProductScrollColumn {
                    uiState.items.forEach { recommendation ->
                        SummaryCard(
                            title = recommendation.title,
                            subtitle = recommendation.recommendation.reason,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BookThumbnail(
                                    title = recommendation.title,
                                    thumbnailUrl = recommendation.thumbnailUrl,
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recommendation.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        recommendation.recommendation.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Score ${recommendation.recommendation.score}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onBookClick(recommendation.recommendation.bookId) }) {
                                Text("Open book")
                            }
                        }
                    }
                    if (uiState.items.isEmpty()) {
                        EmptyPanel("No recommendations yet. Interact with more books to build your profile.")
                    }
                    uiState.message?.let { EmptyPanel(it) }
                }
            }
        }
    }
}

@Composable
fun ClubsRoute(
    onBackClicked: () -> Unit,
    viewModel: ClubsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerBooks = viewModel.pickerBooks.collectAsLazyPagingItems()
    ProductScaffold(
        title = "Book Clubs",
        onBackClicked = onBackClicked,
    ) {
        if (uiState.isLoading) {
            LoadingPanel()
        } else {
            ProductScrollColumn {
                SummaryCard(
                    title = "Create a club",
                    subtitle = "Start a private reading circle and invite members.",
                ) {
                    OutlinedTextField(
                        value = uiState.createNameInput,
                        onValueChange = viewModel::onCreateNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Club name") },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.createDescriptionInput,
                        onValueChange = viewModel::onCreateDescriptionChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description") },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::createClub,
                        enabled = !uiState.isSubmitting,
                    ) {
                        Text("Create club")
                    }
                }

                SummaryCard(
                    title = "Join with invite code",
                    subtitle = "Paste a code from another reader.",
                ) {
                    OutlinedTextField(
                        value = uiState.inviteCodeInput,
                        onValueChange = viewModel::onInviteCodeChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Invite code") },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::joinClub,
                        enabled = !uiState.isSubmitting,
                    ) {
                        Text("Join club")
                    }
                }

                if (uiState.clubs.isEmpty()) {
                    EmptyPanel("No clubs yet. Create one or join with an invite code.")
                } else {
                    uiState.clubs.forEach { club ->
                        SummaryCard(
                            title = club.name,
                            subtitle = club.description.ifBlank { club.moderationLevel },
                        ) {
                            Text("Invite: ${club.inviteCode}")
                            Text("Moderation: ${club.moderationLevel}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.selectClub(club.id) },
                            ) {
                                Text(
                                    if (uiState.selectedClubId == club.id) {
                                        "Hide discussion"
                                    } else {
                                        "Open discussion"
                                    },
                                )
                            }
                        }
                    }
                }

                uiState.selectedClubId?.let { selectedClubId ->
                    SummaryCard(
                        title = "Club discussion",
                        subtitle = "Start a thread for a book or chapter in this club.",
                    ) {
                        OutlinedTextField(
                            value = uiState.threadTitleInput,
                            onValueChange = viewModel::onThreadTitleChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Thread title") },
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SelectedBookPickerField(
                            label = "Book",
                            selectedBook = uiState.selectedThreadBook,
                            fallbackBookId = uiState.threadBookIdInput,
                            onPickClick = viewModel::openBookPicker,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val selectedBookPageCount = uiState.selectedThreadBook?.pageCount ?: 0
                        if (selectedBookPageCount > 0) {
                            ChapterPagePresetSection(
                                pageCount = selectedBookPageCount,
                                selectedReference = uiState.selectedReferencePreset,
                                onReferenceSelected = viewModel::onReferencePresetSelected,
                            )
                        } else {
                            OutlinedTextField(
                                value = uiState.threadChapterInput,
                                onValueChange = viewModel::onThreadChapterChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Reference (chapter/page)") },
                                singleLine = true,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = viewModel::createThread,
                            enabled = !uiState.isSubmitting,
                        ) {
                            Text("Create thread")
                        }

                        val threads = uiState.threadsByClubId[selectedClubId].orEmpty()
                        if (threads.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            EmptyPanel("No threads yet in this club.")
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            threads.forEach { thread ->
                                SummaryCard(
                                    title = thread.title,
                                    subtitle = "${thread.authorLabel} • ${thread.createdAt}",
                                ) {
                                    if (thread.bookId.isNotBlank()) {
                                        Text("Book: ${thread.bookId}")
                                    }
                                    if (thread.chapterRef.isNotBlank()) {
                                        Text("Chapter: ${thread.chapterRef}")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewModel.toggleThread(thread.id) },
                                    ) {
                                        Text(
                                            if (uiState.expandedThreadIds.contains(thread.id)) {
                                                "Hide replies"
                                            } else {
                                                "Show replies"
                                            },
                                        )
                                    }
                                    if (uiState.expandedThreadIds.contains(thread.id)) {
                                        val posts = uiState.postsByThreadId[thread.id].orEmpty()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (posts.isEmpty()) {
                                            EmptyPanel("No replies yet.")
                                        } else {
                                            posts.forEach { post ->
                                                Card(
                                                    colors =
                                                        CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                        ),
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text(
                                                            post.authorLabel,
                                                            style = MaterialTheme.typography.labelLarge,
                                                        )
                                                        Text(
                                                            post.message,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                        )
                                                        Text(
                                                            post.createdAt,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = uiState.replyInputs[thread.id].orEmpty(),
                                            onValueChange = { value -> viewModel.onReplyChanged(thread.id, value) },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = { Text("Reply") },
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.sendReply(thread.id) },
                                            enabled = !uiState.isSubmitting,
                                        ) {
                                            Text("Post reply")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                uiState.message?.let { message -> EmptyPanel(message) }
            }
        }
    }
    if (uiState.isBookPickerVisible) {
        ProductBookPickerSheet(
            query = uiState.pickerQueryInput,
            books = pickerBooks,
            onQueryChanged = viewModel::onPickerQueryChanged,
            onDismiss = viewModel::dismissBookPicker,
            onBookSelected = viewModel::onPickerBookSelected,
        )
    }
}

@Composable
fun NotificationsRoute(
    onBackClicked: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProductScaffold(
        title = "Notifications",
        onBackClicked = onBackClicked,
    ) {
        if (uiState.isLoading) {
            LoadingPanel()
        } else {
            ProductScrollColumn {
                SummaryCard(
                    title = "Preference center",
                    subtitle = "Controls FCM reminder categories and quiet hours.",
                ) {
                    NotificationToggleRow(
                        title = "Goal reminders",
                        checked = uiState.goalReminders,
                        onCheckedChange = viewModel::onGoalRemindersChanged,
                    )
                    NotificationToggleRow(
                        title = "Book club replies",
                        checked = uiState.bookClubReplies,
                        onCheckedChange = viewModel::onBookClubRepliesChanged,
                    )
                    NotificationToggleRow(
                        title = "Friend activity",
                        checked = uiState.friendActivity,
                        onCheckedChange = viewModel::onFriendActivityChanged,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.quietHoursInput,
                        onValueChange = viewModel::onQuietHoursChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Quiet hours") },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::save,
                        enabled = !uiState.isSaving,
                    ) {
                        Text("Save preferences")
                    }
                }
                uiState.message?.let { EmptyPanel(it) }
            }
        }
    }
}

@Composable
fun CreatorModeRoute(
    onBackClicked: () -> Unit,
    viewModel: CreatorModeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProductScaffold(
        title = "Creator Mode",
        onBackClicked = onBackClicked,
    ) {
        ProductScrollColumn {
            SummaryCard(
                title = "Add a custom book",
                subtitle = "Manual entry for your personal library.",
            ) {
                OutlinedTextField(
                    value = uiState.titleInput,
                    onValueChange = viewModel::onTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.authorsInput,
                    onValueChange = viewModel::onAuthorsChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Authors") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.isbnInput,
                        onValueChange = viewModel::onIsbnChanged,
                        modifier = Modifier.weight(1f),
                        label = { Text("ISBN") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.externalRefInput,
                        onValueChange = viewModel::onExternalRefChanged,
                        modifier = Modifier.weight(1f),
                        label = { Text("External ref") },
                        singleLine = true,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.descriptionInput,
                    onValueChange = viewModel::onDescriptionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = viewModel::addCustomBook,
                    enabled = !uiState.isSubmitting,
                ) {
                    Text("Add custom book")
                }
            }

            SummaryCard(
                title = "Import library",
                subtitle = "One row per book: title|authors|isbn|description|externalRef",
            ) {
                OutlinedTextField(
                    value = uiState.importLinesInput,
                    onValueChange = viewModel::onImportLinesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Import rows") },
                    minLines = 5,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = viewModel::importBooks,
                    enabled = !uiState.isSubmitting,
                ) {
                    Text("Run import")
                }
                if (uiState.lastImportSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.lastImportSummary, color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.message?.let { EmptyPanel(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductScaffold(
    title: String,
    onBackClicked: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            content()
        }
    }
}

@Composable
private fun ProductScrollColumn(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun SummaryCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                content()
            },
        )
    }
}

@Composable
private fun LoadingPanel() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun BookThumbnail(
    title: String,
    thumbnailUrl: String,
) {
    if (thumbnailUrl.isBlank()) {
        Box(
            modifier =
                Modifier
                    .size(width = 52.dp, height = 72.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.take(1).ifBlank { "B" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = "$title cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 52.dp, height = 72.dp),
        )
    }
}

@Composable
private fun ChapterPagePresetSection(
    pageCount: Int,
    selectedReference: String?,
    onReferenceSelected: (String) -> Unit,
) {
    val presets = buildChapterPagePresets(pageCount)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Reference presets",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Based on metadata ($pageCount pages).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets.chapterPresets) { preset ->
                FilterChip(
                    selected = preset == selectedReference,
                    onClick = { onReferenceSelected(preset) },
                    label = { Text(preset) },
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets.pagePresets) { preset ->
                FilterChip(
                    selected = preset == selectedReference,
                    onClick = { onReferenceSelected(preset) },
                    label = { Text(preset) },
                )
            }
        }
        Text(
            text = selectedReference ?: "No chapter/page reference selected (optional).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class ChapterPagePresets(
    val chapterPresets: List<String>,
    val pagePresets: List<String>,
)

private fun buildChapterPagePresets(pageCount: Int): ChapterPagePresets {
    val normalizedPageCount = pageCount.coerceAtLeast(1)
    val chapterCount =
        when {
            normalizedPageCount >= 360 -> 12
            normalizedPageCount >= 240 -> 10
            normalizedPageCount >= 160 -> 8
            normalizedPageCount >= 90 -> 6
            normalizedPageCount >= 40 -> 4
            else -> 3
        }
    val chapterPresets = (1..chapterCount).map { chapter -> "Chapter $chapter" }
    val pageMilestones =
        linkedSetOf(
            1,
            (normalizedPageCount * 0.25f).roundToInt().coerceIn(1, normalizedPageCount),
            (normalizedPageCount * 0.5f).roundToInt().coerceIn(1, normalizedPageCount),
            (normalizedPageCount * 0.75f).roundToInt().coerceIn(1, normalizedPageCount),
            normalizedPageCount,
        )
    val pagePresets = pageMilestones.map { page -> "Page $page" }
    return ChapterPagePresets(
        chapterPresets = chapterPresets,
        pagePresets = pagePresets,
    )
}

@Composable
private fun SelectedBookPickerField(
    label: String,
    selectedBook: Book?,
    fallbackBookId: String,
    onPickClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookThumbnail(
                title = selectedBook?.title ?: label,
                thumbnailUrl = selectedBook?.thumbnailUrl.orEmpty(),
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = selectedBook?.title ?: fallbackBookId.ifBlank { "No book selected" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selectedBook != null) {
                    Text(
                        text =
                            buildString {
                                append(
                                    selectedBook.authors
                                        .takeIf { it.isNotEmpty() }
                                        ?.joinToString(", ")
                                        .orEmpty()
                                        .ifBlank { selectedBook.publishedDate },
                                )
                                selectedBook.pageCount?.let { pages ->
                                    if (pages > 0) {
                                        append(" • ")
                                        append("$pages pages")
                                    }
                                }
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Button(onClick = onPickClick) {
                Text("Choose")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductBookPickerSheet(
    query: String,
    books: LazyPagingItems<Book>,
    onQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onBookSelected: (Book) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pick a book",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search books") },
                singleLine = true,
            )
            when {
                books.loadState.refresh is LoadState.Loading && books.itemCount == 0 -> {
                    InlineLoadingIndicator()
                }
                books.itemCount == 0 -> {
                    EmptyPanel("No books found for this search.")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(count = books.itemCount) { index ->
                            val book = books[index] ?: return@items
                            Card(
                                onClick = { onBookSelected(book) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    BookThumbnail(
                                        title = book.title,
                                        thumbnailUrl = book.thumbnailUrl,
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text =
                                                buildString {
                                                    append(
                                                        book.authors
                                                            .takeIf { it.isNotEmpty() }
                                                            ?.joinToString(", ")
                                                            .orEmpty()
                                                            .ifBlank { book.publishedDate },
                                                    )
                                                    book.pageCount?.let { pages ->
                                                        if (pages > 0) {
                                                            append(" • ")
                                                            append("$pages pages")
                                                        }
                                                    }
                                                },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        if (books.loadState.append is LoadState.Loading) {
                            item {
                                InlineLoadingIndicator()
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InlineLoadingIndicator() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
