package com.example.chapterly.backend.auth

import jakarta.servlet.http.HttpServletRequest

interface FirebaseTokenVerifier {
    fun verify(request: HttpServletRequest): AuthPrincipal?
}
