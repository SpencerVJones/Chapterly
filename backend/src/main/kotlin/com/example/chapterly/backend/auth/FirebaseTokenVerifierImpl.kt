package com.example.chapterly.backend.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.FileInputStream

@Component
class FirebaseTokenVerifierImpl(
    @Value("\${firebase.auth.debug-header-enabled:true}")
    private val debugHeaderEnabled: Boolean,
    @Value("\${firebase.auth.debug-email-domain:debug.local}")
    private val debugEmailDomain: String,
) : FirebaseTokenVerifier {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val firebaseAuth: FirebaseAuth? = initializeFirebaseAuth()

    override fun verify(request: HttpServletRequest): AuthPrincipal? {
        val bearerToken = request.extractBearerToken()
        val auth = firebaseAuth

        if (!bearerToken.isNullOrBlank() && auth != null) {
            return runCatching {
                val decoded = auth.verifyIdToken(bearerToken)
                AuthPrincipal(
                    firebaseUid = decoded.uid,
                    email = decoded.email,
                    displayName = decoded.name,
                )
            }.getOrElse { error ->
                logger.warn("Firebase token verification failed: {}", error.message)
                null
            }
        }

        if (debugHeaderEnabled) {
            val uid = request.getHeader(DEBUG_UID_HEADER)?.trim().orEmpty()
            if (uid.isNotBlank()) {
                val debugEmail = request.getHeader(DEBUG_EMAIL_HEADER)?.trim().orEmpty().ifBlank { "$uid@$debugEmailDomain" }
                val debugName = request.getHeader(DEBUG_NAME_HEADER)?.trim().orEmpty().ifBlank { "Debug User" }
                return AuthPrincipal(
                    firebaseUid = uid,
                    email = debugEmail,
                    displayName = debugName,
                )
            }
        }

        return null
    }

    private fun initializeFirebaseAuth(): FirebaseAuth? {
        return runCatching {
            FirebaseApp.getApps().firstOrNull()?.let { existing ->
                logger.info("Using existing FirebaseApp: {}", existing.name)
                return@runCatching FirebaseAuth.getInstance(existing)
            }

            val serviceAccountPath = System.getenv(ENV_FIREBASE_SERVICE_ACCOUNT_PATH).orEmpty().trim()
            val serviceAccountJson = System.getenv(ENV_FIREBASE_SERVICE_ACCOUNT_JSON).orEmpty().trim()

            if (serviceAccountPath.isBlank() && serviceAccountJson.isBlank()) {
                logger.info("Firebase Admin not initialized. Set {} or {} for ID token verification.", ENV_FIREBASE_SERVICE_ACCOUNT_PATH, ENV_FIREBASE_SERVICE_ACCOUNT_JSON)
                return@runCatching null
            }

            val credentials =
                when {
                    serviceAccountPath.isNotBlank() -> {
                        FileInputStream(serviceAccountPath).use { GoogleCredentials.fromStream(it) }
                    }

                    else -> {
                        ByteArrayInputStream(serviceAccountJson.toByteArray()).use { GoogleCredentials.fromStream(it) }
                    }
                }

            val options =
                FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build()

            val app = FirebaseApp.initializeApp(options)
            logger.info("Initialized Firebase Admin app: {}", app.name)
            FirebaseAuth.getInstance(app)
        }.getOrElse { error ->
            logger.warn("Firebase Admin initialization failed: {}", error.message)
            null
        }
    }

    private fun HttpServletRequest.extractBearerToken(): String? {
        val headerValue = getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        if (!headerValue.startsWith(BEARER_PREFIX)) {
            return null
        }
        return headerValue.removePrefix(BEARER_PREFIX).trim().ifBlank { null }
    }

    private companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val ENV_FIREBASE_SERVICE_ACCOUNT_PATH = "FIREBASE_SERVICE_ACCOUNT_PATH"
        private const val ENV_FIREBASE_SERVICE_ACCOUNT_JSON = "FIREBASE_SERVICE_ACCOUNT_JSON"
        private const val DEBUG_UID_HEADER = "X-Debug-Uid"
        private const val DEBUG_EMAIL_HEADER = "X-Debug-Email"
        private const val DEBUG_NAME_HEADER = "X-Debug-Name"
    }
}
