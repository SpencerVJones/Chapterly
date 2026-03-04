package com.example.chapterly.model

data class Book(
    val id: String,
    val title: String,
    val authors: List<String>,
    val description: String,
    val thumbnailUrl: String,
    val previewLink: String,
    val publishedDate: String,
    val pageCount: Int?,
    val averageRating: Double?,
    val isFavorite: Boolean,
)
