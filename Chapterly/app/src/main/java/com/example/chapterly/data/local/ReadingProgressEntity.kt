package com.example.chapterly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val percentComplete: Int,
    val chaptersCompleted: Int,
    val minutesRead: Int,
    val currentShelf: String,
    val lastReadAt: Long,
    val updatedAt: Long,
)
