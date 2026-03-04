package com.example.chapterly.backend.auth

data class AuthPrincipal(
    val firebaseUid: String,
    val email: String?,
    val displayName: String?,
)
