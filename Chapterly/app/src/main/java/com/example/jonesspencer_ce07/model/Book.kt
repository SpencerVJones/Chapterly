package com.example.jonesspencer_ce07.model

data class Book(
    val id: String,
    val title: String,
    val authors: List<String>,
    val description: String,
    val thumbnailUrl: String,
    val previewLink: String,
    val publishedDate: String,
    val isFavorite: Boolean
)
