package com.example.chapterly.backend.repository

import com.example.chapterly.backend.model.HistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface HistoryRepository : JpaRepository<HistoryEntity, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<HistoryEntity>

    fun deleteByUserId(userId: Long)
}
