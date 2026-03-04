package com.example.chapterly.backend.repository

import com.example.chapterly.backend.model.FavoriteEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FavoriteRepository : JpaRepository<FavoriteEntity, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<FavoriteEntity>

    fun findByUserIdAndBookId(
        userId: Long,
        bookId: String,
    ): FavoriteEntity?

    fun deleteByUserIdAndBookId(
        userId: Long,
        bookId: String,
    )
}
