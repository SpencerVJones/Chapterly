package com.example.chapterly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val annotationType: String,
    val content: String,
    val chapterLabel: String,
    val pageRef: String,
    val visibility: String,
    val updatedAt: Long,
)
