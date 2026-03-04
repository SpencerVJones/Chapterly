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
    name = "lists",
    indexes = [
        Index(name = "idx_lists_user_id", columnList = "user_id"),
    ],
)
class ReadingListEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,
    @Column(name = "name", nullable = false, length = 120)
    var name: String = "",
    @Column(name = "description", length = 1024)
    var description: String? = null,
    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = false,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
