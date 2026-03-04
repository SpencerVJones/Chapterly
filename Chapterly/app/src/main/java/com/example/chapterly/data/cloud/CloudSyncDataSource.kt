package com.example.chapterly.data.cloud

data class CloudSearchHistoryEntry(
    val query: String,
    val timestamp: Long,
)

data class CloudReadingProgressEntry(
    val bookId: String,
    val percentComplete: Int,
    val chaptersCompleted: Int,
    val minutesRead: Int,
    val currentShelf: String,
    val lastReadAt: Long,
    val updatedAt: Long,
)

data class CloudAnnotationEntry(
    val id: String,
    val bookId: String,
    val annotationType: String,
    val content: String,
    val chapterLabel: String,
    val pageRef: String,
    val visibility: String,
    val updatedAt: Long,
)

interface CloudSyncDataSource {
    suspend fun syncFavorite(
        bookId: String,
        isFavorite: Boolean,
    )

    suspend fun saveSearchQuery(
        query: String,
        timestamp: Long = System.currentTimeMillis(),
    )

    suspend fun clearSearchHistory()

    suspend fun fetchFavoriteBookIds(): Set<String>

    suspend fun fetchSearchHistory(): List<CloudSearchHistoryEntry>

    suspend fun upsertReadingProgress(entry: CloudReadingProgressEntry)

    suspend fun fetchReadingProgress(): List<CloudReadingProgressEntry>

    suspend fun upsertAnnotation(entry: CloudAnnotationEntry)

    suspend fun fetchAnnotations(): List<CloudAnnotationEntry>
}
