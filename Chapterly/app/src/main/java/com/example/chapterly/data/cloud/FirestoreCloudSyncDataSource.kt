package com.example.chapterly.data.cloud

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.text.Charsets.UTF_8
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCloudSyncDataSource
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth?,
        private val firestore: FirebaseFirestore?,
    ) : CloudSyncDataSource {
        override suspend fun syncFavorite(
            bookId: String,
            isFavorite: Boolean,
        ) {
            val (db, uid) = requireDependencies() ?: return

            runCatching {
                val docRef =
                    db.collection(USERS_COLLECTION)
                        .document(uid)
                        .collection(FAVORITES_COLLECTION)
                        .document(bookId)
                if (isFavorite) {
                    docRef
                        .set(
                            mapOf(
                                BOOK_ID_FIELD to bookId,
                                UPDATED_AT_FIELD to Date(),
                            ),
                        ).await()
                } else {
                    docRef.delete().await()
                }
            }.onFailure { error ->
                Log.w(TAG, "Failed to sync favorite for $bookId", error)
            }
        }

        override suspend fun saveSearchQuery(
            query: String,
            timestamp: Long,
        ) {
            val (db, uid) = requireDependencies() ?: return
            runCatching {
                db.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(HISTORY_COLLECTION)
                    .document(historyDocumentId(query))
                    .set(
                        mapOf(
                            QUERY_FIELD to query,
                            CREATED_AT_FIELD to Date(timestamp),
                        ),
                    ).await()
            }.onFailure { error ->
                Log.w(TAG, "Failed to sync search query", error)
            }
        }

        override suspend fun clearSearchHistory() {
            val (db, uid) = requireDependencies() ?: return
            runCatching {
                val historyCollection =
                    db.collection(USERS_COLLECTION)
                        .document(uid)
                        .collection(HISTORY_COLLECTION)
                val snapshot = historyCollection.get().await()
                val batch = db.batch()
                snapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit().await()
            }.onFailure { error ->
                Log.w(TAG, "Failed to clear cloud search history", error)
            }
        }

        override suspend fun fetchFavoriteBookIds(): Set<String> {
            val (db, uid) = requireDependencies() ?: return emptySet()
            return runCatching {
                db.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(FAVORITES_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document -> document.getString(BOOK_ID_FIELD) ?: document.id }
                    .toSet()
            }.getOrElse { error ->
                Log.w(TAG, "Failed to fetch cloud favorites", error)
                emptySet()
            }
        }

        override suspend fun fetchSearchHistory(): List<CloudSearchHistoryEntry> {
            val (db, uid) = requireDependencies() ?: return emptyList()
            return runCatching {
                db.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(HISTORY_COLLECTION)
                    .orderBy(CREATED_AT_FIELD, Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        val query = document.getString(QUERY_FIELD)?.trim().orEmpty()
                        if (query.isBlank()) {
                            null
                        } else {
                            CloudSearchHistoryEntry(
                                query = query,
                                timestamp = document.getDate(CREATED_AT_FIELD)?.time ?: 0L,
                            )
                        }
                    }
            }.getOrElse { error ->
                Log.w(TAG, "Failed to fetch cloud search history", error)
                emptyList()
            }
        }

        override suspend fun upsertReadingProgress(entry: CloudReadingProgressEntry) {
            val (db, uid) = requireDependencies() ?: return
            runCatching {
                db.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(PROGRESS_COLLECTION)
                    .document(entry.bookId)
                    .set(
                        mapOf(
                            BOOK_ID_FIELD to entry.bookId,
                            PERCENT_COMPLETE_FIELD to entry.percentComplete,
                            CHAPTERS_COMPLETED_FIELD to entry.chaptersCompleted,
                            MINUTES_READ_FIELD to entry.minutesRead,
                            CURRENT_SHELF_FIELD to entry.currentShelf,
                            LAST_READ_AT_FIELD to Date(entry.lastReadAt),
                            UPDATED_AT_FIELD to Date(entry.updatedAt),
                        ),
                    ).await()
            }.onFailure { error ->
                Log.w(TAG, "Failed to sync reading progress for ${entry.bookId}", error)
            }
        }

        override suspend fun fetchReadingProgress(): List<CloudReadingProgressEntry> {
            val (db, uid) = requireDependencies() ?: return emptyList()
            return runCatching {
                db.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(PROGRESS_COLLECTION)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        val bookId = document.getString(BOOK_ID_FIELD)?.trim().orEmpty()
                        if (bookId.isBlank()) {
                            null
                        } else {
                            CloudReadingProgressEntry(
                                bookId = bookId,
                                percentComplete = document.getLong(PERCENT_COMPLETE_FIELD)?.toInt() ?: 0,
                                chaptersCompleted = document.getLong(CHAPTERS_COMPLETED_FIELD)?.toInt() ?: 0,
                                minutesRead = document.getLong(MINUTES_READ_FIELD)?.toInt() ?: 0,
                                currentShelf = document.getString(CURRENT_SHELF_FIELD)?.trim().orEmpty().ifBlank { DEFAULT_SHELF },
                                lastReadAt = document.getDate(LAST_READ_AT_FIELD)?.time ?: 0L,
                                updatedAt = document.getDate(UPDATED_AT_FIELD)?.time ?: 0L,
                            )
                        }
                    }
            }.getOrElse { error ->
                Log.w(TAG, "Failed to fetch reading progress", error)
                emptyList()
            }
        }

        override suspend fun upsertAnnotation(entry: CloudAnnotationEntry) {
            val (db, uid) = requireDependencies() ?: return
            runCatching {
                db.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(ANNOTATIONS_COLLECTION)
                    .document(entry.id)
                    .set(
                        mapOf(
                            ANNOTATION_ID_FIELD to entry.id,
                            BOOK_ID_FIELD to entry.bookId,
                            ANNOTATION_TYPE_FIELD to entry.annotationType,
                            CONTENT_FIELD to entry.content,
                            CHAPTER_LABEL_FIELD to entry.chapterLabel,
                            PAGE_REF_FIELD to entry.pageRef,
                            VISIBILITY_FIELD to entry.visibility,
                            UPDATED_AT_FIELD to Date(entry.updatedAt),
                        ),
                    ).await()
            }.onFailure { error ->
                Log.w(TAG, "Failed to sync annotation ${entry.id}", error)
            }
        }

        override suspend fun fetchAnnotations(): List<CloudAnnotationEntry> {
            val (db, uid) = requireDependencies() ?: return emptyList()
            return runCatching {
                db.collection(USERS_COLLECTION)
                    .document(uid)
                    .collection(ANNOTATIONS_COLLECTION)
                    .orderBy(UPDATED_AT_FIELD, Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        val id = document.getString(ANNOTATION_ID_FIELD)?.trim().orEmpty().ifBlank { document.id }
                        val bookId = document.getString(BOOK_ID_FIELD)?.trim().orEmpty()
                        if (id.isBlank() || bookId.isBlank()) {
                            null
                        } else {
                            CloudAnnotationEntry(
                                id = id,
                                bookId = bookId,
                                annotationType = document.getString(ANNOTATION_TYPE_FIELD)?.trim().orEmpty().ifBlank { DEFAULT_ANNOTATION_TYPE },
                                content = document.getString(CONTENT_FIELD)?.trim().orEmpty(),
                                chapterLabel = document.getString(CHAPTER_LABEL_FIELD)?.trim().orEmpty(),
                                pageRef = document.getString(PAGE_REF_FIELD)?.trim().orEmpty(),
                                visibility = document.getString(VISIBILITY_FIELD)?.trim().orEmpty().ifBlank { DEFAULT_VISIBILITY },
                                updatedAt = document.getDate(UPDATED_AT_FIELD)?.time ?: 0L,
                            )
                        }
                    }
            }.getOrElse { error ->
                Log.w(TAG, "Failed to fetch annotations", error)
                emptyList()
            }
        }

        private fun requireDependencies(): Pair<FirebaseFirestore, String>? {
            val auth = firebaseAuth
            val db = firestore
            val uid = auth?.currentUser?.uid

            if (auth == null || db == null || uid.isNullOrBlank()) {
                return null
            }

            return db to uid
        }

        private companion object {
            private const val TAG = "FirestoreCloudSync"
            private const val USERS_COLLECTION = "users"
            private const val FAVORITES_COLLECTION = "favorites"
            private const val HISTORY_COLLECTION = "history"
            private const val PROGRESS_COLLECTION = "progress"
            private const val ANNOTATIONS_COLLECTION = "annotations"
            private const val BOOK_ID_FIELD = "bookId"
            private const val QUERY_FIELD = "query"
            private const val CREATED_AT_FIELD = "createdAt"
            private const val UPDATED_AT_FIELD = "updatedAt"
            private const val PERCENT_COMPLETE_FIELD = "percentComplete"
            private const val CHAPTERS_COMPLETED_FIELD = "chaptersCompleted"
            private const val MINUTES_READ_FIELD = "minutesRead"
            private const val CURRENT_SHELF_FIELD = "currentShelf"
            private const val LAST_READ_AT_FIELD = "lastReadAt"
            private const val ANNOTATION_ID_FIELD = "annotationId"
            private const val ANNOTATION_TYPE_FIELD = "annotationType"
            private const val CONTENT_FIELD = "content"
            private const val CHAPTER_LABEL_FIELD = "chapterLabel"
            private const val PAGE_REF_FIELD = "pageRef"
            private const val VISIBILITY_FIELD = "visibility"
            private const val DEFAULT_SHELF = "TO_READ"
            private const val DEFAULT_ANNOTATION_TYPE = "NOTE"
            private const val DEFAULT_VISIBILITY = "PRIVATE"
        }
    }

private fun historyDocumentId(query: String): String {
    return query.toByteArray(UTF_8).joinToString(separator = "") { byte ->
        "%02x".format(byte)
    }
}
