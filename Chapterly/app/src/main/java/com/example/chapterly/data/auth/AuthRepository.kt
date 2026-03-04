package com.example.chapterly.data.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<Unit>

    suspend fun signUp(
        email: String,
        password: String,
    ): Result<Unit>

    suspend fun signOut()

    fun currentUserUidOrNull(): String?

    suspend fun updateDisplayName(displayName: String): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
}
