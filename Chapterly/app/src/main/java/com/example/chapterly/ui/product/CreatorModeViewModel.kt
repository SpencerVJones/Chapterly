package com.example.chapterly.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chapterly.data.repository.ProductRepository
import com.example.chapterly.model.CustomBookDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatorModeUiState(
    val titleInput: String = "",
    val authorsInput: String = "",
    val isbnInput: String = "",
    val descriptionInput: String = "",
    val externalRefInput: String = "",
    val importLinesInput: String = "",
    val lastImportSummary: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class CreatorModeViewModel
    @Inject
    constructor(
        private val productRepository: ProductRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CreatorModeUiState())
        val uiState: StateFlow<CreatorModeUiState> = _uiState.asStateFlow()

        fun onTitleChanged(value: String) {
            _uiState.update { current -> current.copy(titleInput = value) }
        }

        fun onAuthorsChanged(value: String) {
            _uiState.update { current -> current.copy(authorsInput = value) }
        }

        fun onIsbnChanged(value: String) {
            _uiState.update { current -> current.copy(isbnInput = value) }
        }

        fun onDescriptionChanged(value: String) {
            _uiState.update { current -> current.copy(descriptionInput = value) }
        }

        fun onExternalRefChanged(value: String) {
            _uiState.update { current -> current.copy(externalRefInput = value) }
        }

        fun onImportLinesChanged(value: String) {
            _uiState.update { current -> current.copy(importLinesInput = value) }
        }

        fun addCustomBook() {
            val draft = uiState.value.toDraft()
            if (draft.title.isBlank()) {
                _uiState.update { current -> current.copy(message = "Enter a book title.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSubmitting = true, message = null) }
                runCatching { productRepository.addCustomBook(draft) }
                    .onSuccess {
                        _uiState.update { current ->
                            current.copy(
                                titleInput = "",
                                authorsInput = "",
                                isbnInput = "",
                                descriptionInput = "",
                                externalRefInput = "",
                                isSubmitting = false,
                                message = "Custom book added to your library.",
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isSubmitting = false,
                                message = error.localizedMessage ?: "Unable to add custom book.",
                            )
                        }
                    }
            }
        }

        fun importBooks() {
            val items =
                uiState.value.importLinesInput
                    .lineSequence()
                    .map { line -> line.trim() }
                    .filter { line -> line.isNotBlank() }
                    .map { line ->
                        val parts = line.split("|").map { segment -> segment.trim() }
                        CustomBookDraft(
                            title = parts.getOrNull(0).orEmpty(),
                            authors = parts.getOrNull(1).orEmpty(),
                            isbn = parts.getOrNull(2).orEmpty(),
                            description = parts.getOrNull(3).orEmpty(),
                            externalRef = parts.getOrNull(4).orEmpty(),
                        )
                    }.filter { draft -> draft.title.isNotBlank() }
                    .toList()

            if (items.isEmpty()) {
                _uiState.update { current ->
                    current.copy(message = "Add import rows in the format title|authors|isbn|description|externalRef.")
                }
                return
            }

            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSubmitting = true, message = null) }
                runCatching { productRepository.importBooks(items) }
                    .onSuccess { summary ->
                        _uiState.update { current ->
                            current.copy(
                                importLinesInput = "",
                                lastImportSummary =
                                    "${summary.importedCount} rows imported • ${summary.deduplicatedCount} deduplicated • ${summary.status}",
                                isSubmitting = false,
                                message = "Import completed.",
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isSubmitting = false,
                                message = error.localizedMessage ?: "Unable to import books.",
                            )
                        }
                    }
            }
        }

        private fun CreatorModeUiState.toDraft(): CustomBookDraft {
            return CustomBookDraft(
                title = titleInput,
                authors = authorsInput,
                isbn = isbnInput,
                description = descriptionInput,
                externalRef = externalRefInput,
            )
        }
    }
