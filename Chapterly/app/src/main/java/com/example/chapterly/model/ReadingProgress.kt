package com.example.chapterly.model

data class ReadingProgress(
    val bookId: String,
    val percentComplete: Int,
    val chaptersCompleted: Int,
    val minutesRead: Int,
    val currentShelf: String,
    val lastReadAt: Long,
    val updatedAt: Long,
)
