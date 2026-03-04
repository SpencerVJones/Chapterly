package com.example.chapterly.ui.auth

import com.example.chapterly.data.auth.AuthState

data class AccountStatsUi(
    val favorites: Int = 0,
    val lists: Int = 0,
    val reviews: Int = 0,
    val history: Int = 0,
    val cachedBooks: Int = 0,
)

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val authState: AuthState = AuthState.Unavailable,
    val stats: AccountStatsUi = AccountStatsUi(),
)
