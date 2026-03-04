package com.example.chapterly.backend.repository

import com.example.chapterly.backend.model.ReadingListEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReadingListRepository : JpaRepository<ReadingListEntity, Long> {
    fun findAllByUserIdOrderByUpdatedAtDesc(userId: Long): List<ReadingListEntity>
}
