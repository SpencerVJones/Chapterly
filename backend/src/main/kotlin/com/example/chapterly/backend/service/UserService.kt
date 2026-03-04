package com.example.chapterly.backend.service

import com.example.chapterly.backend.auth.FirebaseTokenVerifier
import com.example.chapterly.backend.model.UserEntity
import com.example.chapterly.backend.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class UserService(
    private val tokenVerifier: FirebaseTokenVerifier,
    private val userRepository: UserRepository,
) {
    fun requireUser(request: HttpServletRequest): UserEntity {
        val principal =
            tokenVerifier.verify(request)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Firebase auth")

        val existing = userRepository.findByFirebaseUid(principal.firebaseUid)
        val now = Instant.now()

        if (existing != null) {
            existing.email = principal.email
            existing.displayName = principal.displayName
            existing.updatedAt = now
            return userRepository.save(existing)
        }

        return userRepository.save(
            UserEntity(
                firebaseUid = principal.firebaseUid,
                email = principal.email,
                displayName = principal.displayName,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
