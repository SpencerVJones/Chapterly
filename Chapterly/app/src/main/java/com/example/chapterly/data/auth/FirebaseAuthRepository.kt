package com.example.chapterly.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth?,
    ) : AuthRepository {
        private val _authState = MutableStateFlow(initialAuthState())
        override val authState: StateFlow<AuthState> = _authState.asStateFlow()

        private val authStateListener =
            FirebaseAuth.AuthStateListener { auth ->
                _authState.value = auth.currentUser.toAuthState()
            }

        init {
            firebaseAuth?.addAuthStateListener(authStateListener)
        }

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<Unit> {
            val auth = firebaseAuth ?: return Result.failure(IllegalStateException(FIREBASE_UNAVAILABLE_ERROR))
            return runCatching {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                Unit
            }
        }

        override suspend fun signUp(
            email: String,
            password: String,
        ): Result<Unit> {
            val auth = firebaseAuth ?: return Result.failure(IllegalStateException(FIREBASE_UNAVAILABLE_ERROR))
            return runCatching {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
                Unit
            }
        }

        override suspend fun signOut() {
            firebaseAuth?.signOut()
            _authState.value = firebaseAuth?.currentUser.toAuthState()
        }

        override fun currentUserUidOrNull(): String? {
            return firebaseAuth?.currentUser?.uid
        }

        override suspend fun updateDisplayName(displayName: String): Result<Unit> {
            val auth = firebaseAuth ?: return Result.failure(IllegalStateException(FIREBASE_UNAVAILABLE_ERROR))
            val trimmedDisplayName = displayName.trim()
            if (trimmedDisplayName.isBlank()) {
                return Result.failure(IllegalArgumentException("Display name cannot be empty."))
            }
            return runCatching {
                auth.currentUser
                    ?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedDisplayName)
                            .build(),
                    )?.await()
                    ?: throw IllegalStateException("You must be signed in to update your profile.")
                _authState.value = auth.currentUser.toAuthState()
                Unit
            }
        }

        override suspend fun deleteAccount(): Result<Unit> {
            val auth = firebaseAuth ?: return Result.failure(IllegalStateException(FIREBASE_UNAVAILABLE_ERROR))
            return runCatching {
                auth.currentUser?.delete()?.await()
                    ?: throw IllegalStateException("You must be signed in to delete your account.")
                _authState.value = AuthState.SignedOut
                Unit
            }
        }

        private fun initialAuthState(): AuthState {
            return firebaseAuth?.currentUser.toAuthState()
        }

        private fun com.google.firebase.auth.FirebaseUser?.toAuthState(): AuthState {
            return when {
                firebaseAuth == null -> AuthState.Unavailable
                this == null -> AuthState.SignedOut
                else ->
                    AuthState.SignedIn(
                        uid = uid,
                        email = email,
                        displayName = displayName,
                        createdAtMillis = metadata?.creationTimestamp,
                    )
            }
        }

        private companion object {
            private const val FIREBASE_UNAVAILABLE_ERROR =
                "Firebase is not configured. Add google-services.json to app/ and restart the app."
        }
    }
