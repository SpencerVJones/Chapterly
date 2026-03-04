package com.example.chapterly.backend.repository

import com.example.chapterly.backend.model.ReviewEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<ReviewEntity, Long> {
    fun findAllByBookIdOrderByCreatedAtDesc(bookId: String): List<ReviewEntity>

    fun findAllByUserIdOrderByUpdatedAtDesc(userId: Long): List<ReviewEntity>

    fun findByUserIdAndBookId(
        userId: Long,
        bookId: String,
    ): ReviewEntity?
}
