package com.example.chapterly.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chapterly.data.repository.ProductRepository
import com.example.chapterly.model.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val goalReminders: Boolean = true,
    val bookClubReplies: Boolean = true,
    val friendActivity: Boolean = true,
    val quietHoursInput: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class NotificationsViewModel
    @Inject
    constructor(
        private val productRepository: ProductRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NotificationsUiState())
        val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { current -> current.copy(isLoading = true, message = null) }
                runCatching { productRepository.getNotificationPreferences() }
                    .onSuccess { preferences ->
                        _uiState.value = preferences.toUiState()
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                message = error.localizedMessage ?: "Unable to load notification settings.",
                            )
                        }
                    }
            }
        }

        fun onGoalRemindersChanged(value: Boolean) {
            _uiState.update { current -> current.copy(goalReminders = value) }
        }

        fun onBookClubRepliesChanged(value: Boolean) {
            _uiState.update { current -> current.copy(bookClubReplies = value) }
        }

        fun onFriendActivityChanged(value: Boolean) {
            _uiState.update { current -> current.copy(friendActivity = value) }
        }

        fun onQuietHoursChanged(value: String) {
            _uiState.update { current -> current.copy(quietHoursInput = value) }
        }

        fun save() {
            val state = uiState.value
            val preferences =
                NotificationPreferences(
                    goalReminders = state.goalReminders,
                    bookClubReplies = state.bookClubReplies,
                    friendActivity = state.friendActivity,
                    quietHours = state.quietHoursInput,
                )
            viewModelScope.launch {
                _uiState.update { current -> current.copy(isSaving = true, message = null) }
                runCatching { productRepository.updateNotificationPreferences(preferences) }
                    .onSuccess {
                        _uiState.update { current ->
                            current.copy(
                                isSaving = false,
                                message = "Preferences saved.",
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isSaving = false,
                                message = error.localizedMessage ?: "Unable to save notification settings.",
                            )
                        }
                    }
            }
        }

        private fun NotificationPreferences.toUiState(): NotificationsUiState {
            return NotificationsUiState(
                goalReminders = goalReminders,
                bookClubReplies = bookClubReplies,
                friendActivity = friendActivity,
                quietHoursInput = quietHours,
                isLoading = false,
                isSaving = false,
                message = null,
            )
        }
    }
