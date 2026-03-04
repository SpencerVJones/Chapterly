package com.example.chapterly.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chapterly.data.auth.AuthRepository
import com.example.chapterly.data.auth.AuthState
import com.example.chapterly.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val bookRepository: BookRepository,
    ) : ViewModel() {
        private var activeUserId: String? = null

        init {
            viewModelScope.launch {
                authRepository.authState.collect { authState ->
                    handleAuthState(authState)
                }
            }
        }

        private suspend fun handleAuthState(authState: AuthState) {
            val signedInUserId = (authState as? AuthState.SignedIn)?.uid

            if (signedInUserId == null) {
                if (activeUserId != null) {
                    bookRepository.clearUserLocalData()
                }
                activeUserId = null
                return
            }

            if (activeUserId == signedInUserId) {
                return
            }

            if (activeUserId != null && activeUserId != signedInUserId) {
                bookRepository.clearUserLocalData()
            }

            activeUserId = signedInUserId
            bookRepository.syncUserDataFromCloud()
        }
    }
