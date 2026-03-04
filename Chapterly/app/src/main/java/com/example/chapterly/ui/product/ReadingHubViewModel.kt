package com.example.chapterly.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.chapterly.data.repository.BookRepository
import com.example.chapterly.data.repository.DEFAULT_QUERY
import com.example.chapterly.data.repository.ProductRepository
import com.example.chapterly.model.BookAnnotation
import com.example.chapterly.model.Book
import com.example.chapterly.model.BookSortOption
import com.example.chapterly.model.ReadingProgress
import com.example.chapterly.model.ReadingSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private enum class ReadingHubBookPickerTarget {
    PROGRESS,
    NOTE,
}

data class CurrentlyReadingUiItem(
    val progress: ReadingProgress,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
)

data class ReadingHubUiState(
    val summary: ReadingSummary? = null,
    val currentlyReading: List<CurrentlyReadingUiItem> = emptyList(),
    val selectedProgressBook: Book? = null,
    val selectedNoteBook: Book? = null,
    val localAnnotationCount: Int = 0,
    val goalMinutesInput: String = "30",
    val goalPagesInput: String = "20",
    val pickerQueryInput: String = DEFAULT_QUERY,
    val activeBookPickerTarget: String? = null,
    val progressBookIdInput: String = "",
    val progressPercentInput: String = "10",
    val progressMinutesInput: String = "15",
    val progressChaptersInput: String = "1",
    val noteBookIdInput: String = "",
    val noteInput: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ReadingHubViewModel
    @Inject
    constructor(
        private val productRepository: ProductRepository,
        private val bookRepository: BookRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReadingHubUiState())
        val uiState: StateFlow<ReadingHubUiState> = _uiState.asStateFlow()
        private val pickerQuery = MutableStateFlow(DEFAULT_QUERY)
        private var activeBookPickerTarget: ReadingHubBookPickerTarget? = null

        val pickerBooks: Flow<PagingData<Book>> =
            pickerQuery.flatMapLatest { query ->
                bookRepository.pagedBooks(
                    query = query.ifBlank { DEFAULT_QUERY },
                    sortOption = BookSortOption.RELEVANCE,
                    favoritesOnly = false,
                )
            }.cachedIn(viewModelScope)

        init {
            observeLocalData()
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { current -> current.copy(isLoading = true, message = null) }
                runCatching { productRepository.getReadingSummary() }
                    .onSuccess { summary ->
                        _uiState.update { current ->
                            current.copy(
                                summary = summary,
                                isLoading = false,
                                message = null,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                message = error.localizedMessage ?: "Unable to load reading hub.",
                            )
                        }
                    }
            }
        }

        fun onGoalMinutesChanged(value: String) {
            _uiState.update { current -> current.copy(goalMinutesInput = value) }
        }

        fun onGoalPagesChanged(value: String) {
            _uiState.update { current -> current.copy(goalPagesInput = value) }
        }

        fun openProgressBookPicker() {
            activeBookPickerTarget = ReadingHubBookPickerTarget.PROGRESS
            _uiState.update { current ->
                current.copy(
                    activeBookPickerTarget = "progress",
                )
            }
        }

        fun openNoteBookPicker() {
            activeBookPickerTarget = ReadingHubBookPickerTarget.NOTE
            _uiState.update { current ->
                current.copy(
                    activeBookPickerTarget = "note",
                )
            }
        }

        fun dismissBookPicker() {
            activeBookPickerTarget = null
            _uiState.update { current ->
                current.copy(
                    activeBookPickerTarget = null,
                )
            }
        }

        fun onPickerQueryChanged(value: String) {
            _uiState.update { current -> current.copy(pickerQueryInput = value) }
            pickerQuery.value = value.trim().ifBlank { DEFAULT_QUERY }
        }

        fun onPickerBookSelected(book: Book) {
            when (activeBookPickerTarget) {
                ReadingHubBookPickerTarget.PROGRESS -> {
                    _uiState.update { current ->
                        current.copy(
                            selectedProgressBook = book,
                            progressBookIdInput = book.id,
                            activeBookPickerTarget = null,
                        )
                    }
                }
                ReadingHubBookPickerTarget.NOTE -> {
                    _uiState.update { current ->
                        current.copy(
                            selectedNoteBook = book,
                            noteBookIdInput = book.id,
                            activeBookPickerTarget = null,
                        )
                    }
                }
                null -> Unit
            }
            activeBookPickerTarget = null
        }

        fun saveGoal() {
            val minutes = uiState.value.goalMinutesInput.toIntOrNull() ?: 0
            val pages = uiState.value.goalPagesInput.toIntOrNull() ?: 0
            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSaving = true, message = null) }
                runCatching {
                    productRepository.upsertReadingGoal(
                        com.example.chapterly.model.ReadingGoal(
                            cadence = "DAILY",
                            targetMinutes = minutes.coerceAtLeast(0),
                            targetPages = pages.coerceAtLeast(0),
                            reminderEnabled = true,
                            quietHours = "22:00-07:00",
                        ),
                    )
                }.onSuccess {
                    refresh()
                    _uiState.update { current ->
                        current.copy(
                            isSaving = false,
                            message = "Reading goal updated.",
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isSaving = false,
                            message = error.localizedMessage ?: "Unable to save goal.",
                        )
                    }
                }
            }
        }

        fun onProgressBookIdChanged(value: String) {
            _uiState.update { current -> current.copy(progressBookIdInput = value) }
        }

        fun onProgressPercentChanged(value: String) {
            _uiState.update { current -> current.copy(progressPercentInput = value) }
        }

        fun onProgressMinutesChanged(value: String) {
            _uiState.update { current -> current.copy(progressMinutesInput = value) }
        }

        fun onProgressChaptersChanged(value: String) {
            _uiState.update { current -> current.copy(progressChaptersInput = value) }
        }

        fun saveProgress() {
            val state = uiState.value
            val bookId = state.selectedProgressBook?.id ?: state.progressBookIdInput.trim()
            if (bookId.isBlank()) {
                _uiState.update { current -> current.copy(message = "Choose a book to track progress.") }
                return
            }

            val progress =
                ReadingProgress(
                    bookId = bookId,
                    percentComplete = state.progressPercentInput.toIntOrNull() ?: 0,
                    chaptersCompleted = state.progressChaptersInput.toIntOrNull() ?: 0,
                    minutesRead = state.progressMinutesInput.toIntOrNull() ?: 0,
                    currentShelf = "CURRENTLY_READING",
                    lastReadAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )

            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSaving = true, message = null) }
                runCatching {
                    bookRepository.upsertReadingProgress(progress)
                    productRepository.syncReadingProgress(progress)
                }.onSuccess {
                    refresh()
                    _uiState.update { current ->
                        current.copy(
                            isSaving = false,
                            message = "Progress synced.",
                            progressBookIdInput = bookId,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isSaving = false,
                            message = error.localizedMessage ?: "Unable to sync progress.",
                        )
                    }
                }
            }
        }

        fun onNoteBookIdChanged(value: String) {
            _uiState.update { current -> current.copy(noteBookIdInput = value) }
        }

        fun onNoteChanged(value: String) {
            _uiState.update { current -> current.copy(noteInput = value) }
        }

        fun saveNote() {
            val state = uiState.value
            val bookId = state.selectedNoteBook?.id ?: state.noteBookIdInput.trim()
            val content = state.noteInput.trim()
            if (bookId.isBlank() || content.isBlank()) {
                _uiState.update { current -> current.copy(message = "Choose a book and enter note text.") }
                return
            }

            val annotation =
                BookAnnotation(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    annotationType = "NOTE",
                    content = content,
                    chapterLabel = "",
                    pageRef = "",
                    visibility = "PRIVATE",
                    updatedAt = System.currentTimeMillis(),
                )

            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSaving = true, message = null) }
                runCatching {
                    bookRepository.saveAnnotation(annotation)
                    productRepository.syncAnnotation(annotation)
                }.onSuccess {
                    refresh()
                    _uiState.update { current ->
                        current.copy(
                            isSaving = false,
                            message = "Note saved.",
                            noteBookIdInput = "",
                            noteInput = "",
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isSaving = false,
                            message = error.localizedMessage ?: "Unable to save note.",
                        )
                    }
                }
            }
        }

        private fun observeLocalData() {
            viewModelScope.launch {
                combine(
                    bookRepository.observeCurrentlyReading(),
                    bookRepository.observeAnnotationCount(),
                ) { currentlyReading, annotationCount ->
                    currentlyReading to annotationCount
                }.collect { (currentlyReading, annotationCount) ->
                    val displayItems = buildCurrentlyReadingItems(currentlyReading)
                    _uiState.update { current ->
                        current.copy(
                            currentlyReading = displayItems,
                            localAnnotationCount = annotationCount,
                        )
                    }
                }
            }
        }

        private suspend fun buildCurrentlyReadingItems(
            progressItems: List<ReadingProgress>,
        ): List<CurrentlyReadingUiItem> {
            return coroutineScope {
                progressItems.map { progress ->
                    async {
                        val book = resolveBook(progress.bookId)
                        CurrentlyReadingUiItem(
                            progress = progress,
                            title = book?.title ?: "Book ${progress.bookId}",
                            subtitle =
                                buildString {
                                    if (!book?.authors.isNullOrEmpty()) {
                                        append(book?.authors?.joinToString(", "))
                                    } else {
                                        append("Currently reading")
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
