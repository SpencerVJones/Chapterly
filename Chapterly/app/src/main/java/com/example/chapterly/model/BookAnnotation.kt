package com.example.chapterly.model

data class BookAnnotation(
    val id: String,
    val bookId: String,
    val annotationType: String,
    val content: String,
    val chapterLabel: String,
    val pageRef: String,
    val visibility: String,
    val updatedAt: Long,
)
