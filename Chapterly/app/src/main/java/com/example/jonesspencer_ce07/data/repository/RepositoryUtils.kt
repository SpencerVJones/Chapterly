package com.example.jonesspencer_ce07.data.repository

import com.example.jonesspencer_ce07.data.local.BookRow
import com.example.jonesspencer_ce07.model.Book

const val AUTHOR_SEPARATOR = "||"
const val BOOKS_PAGE_SIZE = 20
const val DEFAULT_QUERY = "android"
const val SEARCH_HISTORY_LIMIT = 8
const val DEFAULT_TITLE = "Untitled"
const val DEFAULT_DESCRIPTION = "No description available."
const val DEFAULT_PUBLISHED_DATE = "Unknown publication date"

fun normalizeQuery(query: String): String = query.trim().ifBlank { DEFAULT_QUERY }

fun normalizeQueryKey(query: String): String = normalizeQuery(query).lowercase()

fun buildImageUrl(thumbnail: String?, fallback: String?): String {
    val candidate = thumbnail ?: fallback
    return candidate?.replace("http://", "https://").orEmpty()
}

fun buildPreviewUrl(previewLink: String?): String {
    return previewLink?.replace("http://", "https://").orEmpty()
}

fun BookRow.toDomain(): Book {
    val authorList = authors
        .split(AUTHOR_SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return Book(
        id = bookId,
        title = title.ifBlank { DEFAULT_TITLE },
        authors = authorList,
        description = description?.ifBlank { DEFAULT_DESCRIPTION } ?: DEFAULT_DESCRIPTION,
        thumbnailUrl = thumbnailUrl.orEmpty(),
        previewLink = previewLink.orEmpty(),
        publishedDate = publishedDate?.ifBlank { DEFAULT_PUBLISHED_DATE } ?: DEFAULT_PUBLISHED_DATE,
        isFavorite = isFavorite
    )
}
