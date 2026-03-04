package com.example.chapterly.backend.repository

import com.example.chapterly.backend.model.ListItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ListItemRepository : JpaRepository<ListItemEntity, Long> {
    fun findAllByListIdOrderByPositionAscCreatedAtAsc(listId: Long): List<ListItemEntity>

    fun findByListIdAndBookId(
        listId: Long,
        bookId: String,
    ): ListItemEntity?
}
