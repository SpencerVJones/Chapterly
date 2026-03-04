package com.example.chapterly.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "favorites",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_favorites_user_book", columnNames = ["user_id", "book_id"]),
    ],
    indexes = [
        Index(name = "idx_favorites_user_id", columnList = "user_id"),
    ],
)
class FavoriteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,
    @Column(name = "book_id", nullable = false, length = 128)
    var bookId: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
