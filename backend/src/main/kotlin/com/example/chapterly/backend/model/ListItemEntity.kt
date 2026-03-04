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
    name = "list_items",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_list_items_list_book", columnNames = ["list_id", "book_id"]),
    ],
    indexes = [
        Index(name = "idx_list_items_list_id", columnList = "list_id"),
    ],
)
class ListItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "list_id", nullable = false)
    var listId: Long = 0,
    @Column(name = "book_id", nullable = false, length = 128)
    var bookId: String = "",
    @Column(name = "notes", length = 2000)
    var notes: String? = null,
    @Column(name = "position", nullable = false)
    var position: Int = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
