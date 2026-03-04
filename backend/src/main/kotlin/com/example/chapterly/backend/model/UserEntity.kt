package com.example.chapterly.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_firebase_uid", columnList = "firebase_uid", unique = true),
    ],
)
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "firebase_uid", nullable = false, length = 128)
    var firebaseUid: String = "",
    @Column(name = "email", length = 320)
    var email: String? = null,
    @Column(name = "display_name", length = 120)
    var displayName: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
