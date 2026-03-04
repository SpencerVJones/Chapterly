package com.example.chapterly.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.chapterly.data.repository.BookRepository
import com.example.chapterly.data.repository.DEFAULT_QUERY
import com.example.chapterly.data.repository.ProductRepository
import com.example.chapterly.model.Book
import com.example.chapterly.model.BookClub
import com.example.chapterly.model.BookSortOption
import com.example.chapterly.model.DiscussionPost
import com.example.chapterly.model.DiscussionThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClubsUiState(
    val clubs: List<BookClub> = emptyList(),
    val selectedClubId: Long? = null,
    val selectedThreadBook: Book? = null,
    val threadsByClubId: Map<Long, List<DiscussionThread>> = emptyMap(),
    val postsByThreadId: Map<Long, List<DiscussionPost>> = emptyMap(),
    val expandedThreadIds: Set<Long> = emptySet(),
    val createNameInput: String = "",
    val createDescriptionInput: String = "",
    val inviteCodeInput: String = "",
    val pickerQueryInput: String = DEFAULT_QUERY,
    val isBookPickerVisible: Boolean = false,
    val threadTitleInput: String = "",
    val threadBookIdInput: String = "",
    val threadChapterInput: String = "",
    val selectedReferencePreset: String? = null,
    val replyInputs: Map<Long, String> = emptyMap(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ClubsViewModel
    @Inject
    constructor(
        private val productRepository: ProductRepository,
        private val bookRepository: BookRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ClubsUiState())
        val uiState: StateFlow<ClubsUiState> = _uiState.asStateFlow()
        private val pickerQuery = MutableStateFlow(DEFAULT_QUERY)

        val pickerBooks: Flow<PagingData<Book>> =
            pickerQuery.flatMapLatest { query ->
                bookRepository.pagedBooks(
                    query = query.ifBlank { DEFAULT_QUERY },
                    sortOption = BookSortOption.RELEVANCE,
                    favoritesOnly = false,
                )
            }.cachedIn(viewModelScope)

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { current -> current.copy(isLoading = true, message = null) }
                runCatching { productRepository.getClubs() }
                    .onSuccess { clubs ->
                        val selectedClubId = uiState.value.selectedClubId ?: clubs.firstOrNull()?.id
                        _uiState.update { current ->
                            current.copy(
                                clubs = clubs,
                                isLoading = false,
                                selectedClubId = selectedClubId,
                            )
                        }
                        if (selectedClubId != null && uiState.value.threadsByClubId[selectedClubId] == null) {
                            refreshThreads(selectedClubId)
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                message = error.localizedMessage ?: "Unable to load book clubs.",
                            )
                        }
                    }
            }
        }

        fun onCreateNameChanged(value: String) {
            _uiState.update { current -> current.copy(createNameInput = value) }
        }

        fun onCreateDescriptionChanged(value: String) {
            _uiState.update { current -> current.copy(createDescriptionInput = value) }
        }

        fun createClub() {
            val state = uiState.value
            val name = state.createNameInput.trim()
            if (name.isBlank()) {
                _uiState.update { current -> current.copy(message = "Enter a club name.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSubmitting = true, message = null) }
                runCatching {
                    productRepository.createClub(
                        name = name,
                        description = state.createDescriptionInput,
                    )
                }.onSuccess { created ->
                    _uiState.update { current ->
                        current.copy(
                            clubs = listOf(created) + current.clubs.filterNot { club -> club.id == created.id },
                            selectedClubId = created.id,
                            createNameInput = "",
                            createDescriptionInput = "",
                            isSubmitting = false,
                            message = "Club created.",
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isSubmitting = false,
                            message = error.localizedMessage ?: "Unable to create club.",
                        )
                    }
                }
            }
        }

        fun onInviteCodeChanged(value: String) {
            _uiState.update { current -> current.copy(inviteCodeInput = value) }
        }

        fun openBookPicker() {
            _uiState.update { current -> current.copy(isBookPickerVisible = true) }
        }

        fun dismissBookPicker() {
            _uiState.update { current -> current.copy(isBookPickerVisible = false) }
        }

        fun onPickerQueryChanged(value: String) {
            _uiState.update { current -> current.copy(pickerQueryInput = value) }
            pickerQuery.value = value.trim().ifBlank { DEFAULT_QUERY }
        }

        fun onPickerBookSelected(book: Book) {
            _uiState.update { current ->
                current.copy(
                    selectedThreadBook = book,
                    threadBookIdInput = book.id,
                    threadChapterInput = "",
                    selectedReferencePreset = null,
                    isBookPickerVisible = false,
                )
            }
        }

        fun onReferencePresetSelected(reference: String) {
            val normalized = reference.trim()
            if (normalized.isBlank()) return
            _uiState.update { current ->
                current.copy(
                    threadChapterInput = normalized,
                    selectedReferencePreset = normalized,
                    message = null,
                )
            }
        }

        fun joinClub() {
            val code = uiState.value.inviteCodeInput.trim()
            if (code.isBlank()) {
                _uiState.update { current -> current.copy(message = "Enter an invite code.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSubmitting = true, message = null) }
                runCatching { productRepository.joinClub(code) }
                    .onSuccess { joined ->
                        _uiState.update { current ->
                            current.copy(
                                clubs = listOf(joined) + current.clubs.filterNot { club -> club.id == joined.id },
                                selectedClubId = joined.id,
                                inviteCodeInput = "",
                                isSubmitting = false,
                                message = "Joined ${joined.name}.",
                            )
                        }
                        refreshThreads(joined.id)
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isSubmitting = false,
                                message = error.localizedMessage ?: "Unable to join club.",
                            )
                        }
                    }
            }
        }

        fun selectClub(clubId: Long) {
            val isAlreadySelected = uiState.value.selectedClubId == clubId
            _uiState.update { current ->
                current.copy(
                    selectedClubId = if (isAlreadySelected) null else clubId,
                    message = null,
                )
            }
            if (!isAlreadySelected) {
                refreshThreads(clubId)
            }
        }

        fun onThreadTitleChanged(value: String) {
            _uiState.update { current -> current.copy(threadTitleInput = value) }
        }

        fun onThreadBookIdChanged(value: String) {
            _uiState.update { current -> current.copy(threadBookIdInput = value) }
        }

        fun onThreadChapterChanged(value: String) {
            _uiState.update { current ->
                current.copy(
                    threadChapterInput = value,
                    selectedReferencePreset = null,
                )
            }
        }

        fun createThread() {
            val state = uiState.value
            val clubId = state.selectedClubId
            val title = state.threadTitleInput.trim()
            if (clubId == null) {
                _uiState.update { current -> current.copy(message = "Select a club first.") }
                return
            }
            if (title.isBlank()) {
                _uiState.update { current -> current.copy(message = "Enter a thread title.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSubmitting = true, message = null) }
                runCatching {
                    productRepository.createClubThread(
                        clubId = clubId,
                        title = title,
                        bookId = state.selectedThreadBook?.id ?: state.threadBookIdInput,
                        chapterRef = state.threadChapterInput,
                    )
                }.onSuccess { created ->
                    _uiState.update { current ->
                        val existingThreads = current.threadsByClubId[clubId].orEmpty()
                        current.copy(
                            threadsByClubId =
                                current.threadsByClubId +
                                    (clubId to (listOf(created) + existingThreads.filterNot { it.id == created.id })),
                            threadTitleInput = "",
                            threadChapterInput = "",
                            selectedReferencePreset = null,
                            isSubmitting = false,
                            message = "Thread created.",
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isSubmitting = false,
                            message = error.localizedMessage ?: "Unable to create thread.",
                        )
                    }
                }
            }
        }

        fun toggleThread(threadId: Long) {
            val expanded = uiState.value.expandedThreadIds.contains(threadId)
            _uiState.update { current ->
                current.copy(
                    expandedThreadIds =
                        if (expanded) {
                            current.expandedThreadIds - threadId
                        } else {
                            current.expandedThreadIds + threadId
                        },
                )
            }
            if (!expanded) {
                refreshPosts(threadId)
            }
        }

        fun onReplyChanged(threadId: Long, value: String) {
            _uiState.update { current ->
                current.copy(
                    replyInputs = current.replyInputs + (threadId to value),
                )
            }
        }

        fun sendReply(threadId: Long) {
            val message = uiState.value.replyInputs[threadId].orEmpty().trim()
            if (message.isBlank()) {
                _uiState.update { current -> current.copy(message = "Enter a reply before posting.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSubmitting = true, message = null) }
                runCatching { productRepository.createThreadPost(threadId, message) }
                    .onSuccess { created ->
                        _uiState.update { current ->
                            current.copy(
                                postsByThreadId =
                                    current.postsByThreadId +
                                        (threadId to current.postsByThreadId[threadId].orEmpty() + created),
                                replyInputs = current.replyInputs + (threadId to ""),
                                expandedThreadIds = current.expandedThreadIds + threadId,
                                isSubmitting = false,
                                message = "Reply posted.",
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isSubmitting = false,
                                message = error.localizedMessage ?: "Unable to post reply.",
                            )
                        }
                    }
            }
        }

        private fun refreshThreads(clubId: Long) {
            viewModelScope.launch {
                runCatching { productRepository.getClubThreads(clubId) }
                    .onSuccess { threads ->
                        _uiState.update { current ->
                            current.copy(
                                threadsByClubId = current.threadsByClubId + (clubId to threads),
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                message = error.localizedMessage ?: "Unable to load threads.",
                            )
                        }
                    }
            }
        }

        private fun refreshPosts(threadId: Long) {
            viewModelScope.launch {
                runCatching { productRepository.getThreadPosts(threadId) }
                    .onSuccess { posts ->
                        _uiState.update { current ->
                            current.copy(
                                postsByThreadId = current.postsByThreadId + (threadId to posts),
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                message = error.localizedMessage ?: "Unable to load replies.",
                            )
                        }
                    }
            }
        }
    }
