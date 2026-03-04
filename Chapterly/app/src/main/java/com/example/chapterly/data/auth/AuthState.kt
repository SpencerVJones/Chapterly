package com.example.chapterly.data.auth

sealed interface AuthState {
    data object Unavailable : AuthState

    data object SignedOut : AuthState

    data class SignedIn(
        val uid: String,
        val email: String?,
        val displayName: String?,
        val createdAtMillis: Long?,
    ) : AuthState
}
