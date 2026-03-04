package com.example.chapterly.ui.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chapterly.data.repository.BookRepository
import com.example.chapterly.data.repository.ProductRepository
import com.example.chapterly.model.BookAnnotation
import com.example.chapterly.model.ReadingProgress
import com.example.chapterly.ui.navigation.BOOK_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

data class BookDetailReadingUiState(
    val savedProgress: ReadingProgress? = null,
    val annotations: List<BookAnnotation> = emptyList(),
    val progressPercentInput: String = "",
    val progressMinutesInput: String = "",
    val progressChaptersInput: String = "",
    val noteInput: String = "",
    val isSavingProgress: Boolean = false,
    val isSavingNote: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class BookDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: BookRepository,
        private val productRepository: ProductRepository,
    ) : ViewModel() {
        private val bookId: String =
            decodeBookId(
                checkNotNull(savedStateHandle[BOOK_ID_ARG]),
            )

        private val _uiState = MutableStateFlow<BookDetailUiState>(BookDetailUiState.Loading)
        val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()
        private val _readingUiState = MutableStateFlow(BookDetailReadingUiState())
        val readingUiState: StateFlow<BookDetailReadingUiState> = _readingUiState.asStateFlow()
        private var hydratedProgressInputs = false

        init {
            loadBook()
            observeBook()
            observeReadingProgress()
            observeAnnotations()
        }

        fun loadBook() {
            viewModelScope.launch {
                _uiState.value = BookDetailUiState.Loading
                runCatching {
                    repository.refreshBookDetail(bookId)
                }

                val cachedBook = repository.observeBook(bookId).firstOrNull()
                if (cachedBook == null) {
                    _uiState.value = BookDetailUiState.Error("Unable to load book details.")
                }
            }
        }

        fun onFavoriteClicked() {
            val currentBook = (_uiState.value as? BookDetailUiState.Success)?.book ?: return
            viewModelScope.launch {
                repository.toggleFavorite(
                    bookId = currentBook.id,
                    currentlyFavorite = currentBook.isFavorite,
                )
            }
        }

        fun onProgressPercentChanged(value: String) {
            _readingUiState.update { current ->
                current.copy(
                    progressPercentInput = value,
                    message = null,
                )
            }
        }

        fun onProgressMinutesChanged(value: String) {
            _readingUiState.update { current ->
                current.copy(
                    progressMinutesInput = value,
                    message = null,
                )
            }
        }

        fun onProgressChaptersChanged(value: String) {
            _readingUiState.update { current ->
                current.copy(
                    progressChaptersInput = value,
                    message = null,
                )
            }
        }

        fun onNoteChanged(value: String) {
            _readingUiState.update { current ->
                current.copy(
                    noteInput = value,
                    message = null,
                )
            }
        }

        fun saveProgress() {
            val now = System.currentTimeMillis()
            val percent = readingUiState.value.progressPercentInput.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val minutes = readingUiState.value.progressMinutesInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val chapters = readingUiState.value.progressChaptersInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val progress =
                ReadingProgress(
                    bookId = bookId,
                    percentComplete = percent,
                    chaptersCompleted = chapters,
                    minutesRead = minutes,
                    currentShelf = if (percent >= 100) "FINISHED" else "CURRENTLY_READING",
                    lastReadAt = now,
                    updatedAt = now,
                )

            viewModelScope.launch {
                _readingUiState.update { current ->
                    current.copy(
                        isSavingProgress = true,
                        message = null,
                    )
                }
                runCatching {
                    repository.upsertReadingProgress(progress)
                }.onSuccess {
                    val syncResult = runCatching { productRepository.syncReadingProgress(progress) }
                    _readingUiState.update { current ->
                        current.copy(
                            savedProgress = progress,
                            progressPercentInput = progress.percentComplete.toString(),
                            progressMinutesInput = progress.minutesRead.toString(),
                            progressChaptersInput = progress.chaptersCompleted.toString(),
                            isSavingProgress = false,
                            message =
                                syncResult.exceptionOrNull()?.let {
                                    "Progress saved locally. Backend sync is pending."
                                } ?: "Progress updated.",
                        )
                    }
                    hydratedProgressInputs = true
                }.onFailure { error ->
                    _readingUiState.update { current ->
                        current.copy(
                            isSavingProgress = false,
                            message = error.localizedMessage ?: "Unable to save progress.",
                        )
                    }
                }
            }
        }

        fun saveNote() {
            val content = readingUiState.value.noteInput.trim()
            if (content.isBlank()) {
                _readingUiState.update { current -> current.copy(message = "Enter a note before saving.") }
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
                _readingUiState.update { current ->
                    current.copy(
                        isSavingNote = true,
                        message = null,
                    )
                }
                runCatching {
                    repository.saveAnnotation(annotation)
                }.onSuccess {
                    val syncResult = runCatching { productRepository.syncAnnotation(annotation) }
                    _readingUiState.update { current ->
                        current.copy(
                            isSavingNote = false,
                            noteInput = "",
                            message =
                                syncResult.exceptionOrNull()?.let {
                                    "Note saved locally. Backend sync is pending."
                                } ?: "Note saved.",
                        )
                    }
                }.onFailure { error ->
                    _readingUiState.update { current ->
                        current.copy(
                            isSavingNote = false,
                            message = error.localizedMessage ?: "Unable to save note.",
                        )
                    }
                }
            }
        }

        private fun observeBook() {
            viewModelScope.launch {
                repository.observeBook(bookId).collect { book ->
                    if (book != null) {
                        _uiState.value = BookDetailUiState.Success(book)
                    }
                }
            }
        }

        private fun observeReadingProgress() {
            viewModelScope.launch {
                repository.observeReadingProgress(bookId).collect { progress ->
                    _readingUiState.update { current ->
                        val base = current.copy(savedProgress = progress)
                        if (!hydratedProgressInputs) {
                            hydratedProgressInputs = true
                            base.copy(
                                progressPercentInput = progress?.percentComplete?.toString().orEmpty(),
                                progressMinutesInput = progress?.minutesRead?.toString().orEmpty(),
                                progressChaptersInput = progress?.chaptersCompleted?.toString().orEmpty(),
                            )
                        } else {
                            base
                        }
                    }
                }
            }
        }

        private fun observeAnnotations() {
            viewModelScope.launch {
                repository.observeAnnotations(bookId).collect { annotations ->
                    _readingUiState.update { current ->
                        current.copy(
                            annotations = annotations.sortedByDescending { annotation -> annotation.updatedAt },
                        )
                    }
                }
            }
        }

        private fun decodeBookId(encoded: String): String {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        }
    }
