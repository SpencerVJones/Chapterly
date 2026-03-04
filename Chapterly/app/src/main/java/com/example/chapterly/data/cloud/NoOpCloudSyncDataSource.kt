package com.example.chapterly.data.cloud

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpCloudSyncDataSource
    @Inject
    constructor() : CloudSyncDataSource {
        override suspend fun syncFavorite(
            bookId: String,
            isFavorite: Boolean,
        ) {
            // Intentionally no-op when cloud sync is unavailable.
        }

        override suspend fun saveSearchQuery(
            query: String,
            timestamp: Long,
        ) {
            // Intentionally no-op when cloud sync is unavailable.
        }

    override suspend fun clearSearchHistory() {
        // Intentionally no-op when cloud sync is unavailable.
    }

    override suspend fun fetchFavoriteBookIds(): Set<String> = emptySet()

    override suspend fun fetchSearchHistory(): List<CloudSearchHistoryEntry> = emptyList()

    override suspend fun upsertReadingProgress(entry: CloudReadingProgressEntry) {
        // Intentionally no-op when cloud sync is unavailable.
    }

    override suspend fun fetchReadingProgress(): List<CloudReadingProgressEntry> = emptyList()

    override suspend fun upsertAnnotation(entry: CloudAnnotationEntry) {
        // Intentionally no-op when cloud sync is unavailable.
    }

    override suspend fun fetchAnnotations(): List<CloudAnnotationEntry> = emptyList()
}
