package com.example.chapterly.data.local

import androidx.room.Entity

@Entity(tableName = "books", primaryKeys = ["queryKey", "bookId"])
data class BookEntity(
    val queryKey: String,
    val bookId: String,
    val title: String,
    val authors: String,
    val description: String?,
    val thumbnailUrl: String?,
    val previewLink: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val averageRating: Double?,
    val listPosition: Int,
    val cachedAt: Long,
)
