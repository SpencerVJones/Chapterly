package com.example.chapterly.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chapterly.data.auth.AuthRepository
import com.example.chapterly.data.auth.AuthState
import com.example.chapterly.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val bookRepository: BookRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                combine(
                    authRepository.authState,
                    bookRepository.observeFavoriteCount(),
                    bookRepository.observeSearchHistoryCount(),
                    bookRepository.observeCachedBookCount(),
                ) { authState, favoriteCount, historyCount, cachedBookCount ->
                    authState to
                        AccountStatsUi(
                            favorites = favoriteCount,
                            lists = 0,
                            reviews = 0,
                            history = historyCount,
                            cachedBooks = cachedBookCount,
                        )
                }.collect { (authState, stats) ->
                    _uiState.update { current ->
                        current.copy(
                            authState = authState,
                            stats = stats,
                            isSubmitting = false,
                        )
                    }
                }
            }
        }

        fun onEmailChanged(value: String) {
            _uiState.update { current ->
                current.copy(
                    email = value,
                    errorMessage = null,
                )
            }
        }

        fun onPasswordChanged(value: String) {
            _uiState.update { current ->
                current.copy(
                    password = value,
                    errorMessage = null,
                )
            }
        }

        fun signIn() {
            submitAuthAction { email, password ->
                authRepository.signIn(email, password)
            }
        }

        fun signUp() {
            submitAuthAction { email, password ->
                authRepository.signUp(email, password)
            }
        }

        fun signOut() {
            viewModelScope.launch {
                authRepository.signOut()
            }
        }

        fun updateDisplayName(displayName: String) {
            if (displayName.trim().isBlank()) {
                _uiState.update { current ->
                    current.copy(errorMessage = "Display name cannot be empty.")
                }
                return
            }

            _uiState.update { current ->
                current.copy(isSubmitting = true, errorMessage = null)
            }

            viewModelScope.launch {
                val result = authRepository.updateDisplayName(displayName)
                _uiState.update { current ->
                    current.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage,
                    )
                }
            }
        }

        fun clearOfflineCache() {
            viewModelScope.launch {
                bookRepository.clearOfflineCache()
            }
        }

        fun deleteAccount() {
            _uiState.update { current ->
                current.copy(isSubmitting = true, errorMessage = null)
            }

            viewModelScope.launch {
                val result = authRepository.deleteAccount()
                _uiState.update { current ->
                    current.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage,
                    )
                }
            }
        }

        private fun submitAuthAction(block: suspend (String, String) -> Result<Unit>) {
            val email = uiState.value.email.trim()
            val password = uiState.value.password

            if (email.isBlank() || password.length < MIN_PASSWORD_LENGTH) {
                _uiState.update { current ->
                    current.copy(
                        errorMessage = "Enter a valid email and a password with at least 6 characters.",
                    )
                }
                return
            }

            if (uiState.value.authState == AuthState.Unavailable) {
                _uiState.update { current ->
                    current.copy(errorMessage = "Firebase is not configured for this build.")
                }
                return
            }

            _uiState.update { current ->
                current.copy(
                    isSubmitting = true,
                    errorMessage = null,
                )
            }

            viewModelScope.launch {
                val result = block(email, password)
                _uiState.update { current ->
                    current.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage,
                    )
                }
            }
        }

        private companion object {
            private const val MIN_PASSWORD_LENGTH = 6
        }
    }
