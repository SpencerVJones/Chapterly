package com.example.chapterly.data.repository

import com.example.chapterly.model.BookAnnotation
import com.example.chapterly.model.BookClub
import com.example.chapterly.model.CustomBookDraft
import com.example.chapterly.model.DiscussionPost
import com.example.chapterly.model.DiscussionThread
import com.example.chapterly.model.ImportJobSummary
import com.example.chapterly.model.NotificationPreferences
import com.example.chapterly.model.ReadingGoal
import com.example.chapterly.model.ReadingProgress
import com.example.chapterly.model.ReadingSummary
import com.example.chapterly.model.Recommendation

interface ProductRepository {
    suspend fun getReadingSummary(): ReadingSummary

    suspend fun upsertReadingGoal(goal: ReadingGoal): ReadingGoal

    suspend fun syncReadingProgress(progress: ReadingProgress)

    suspend fun syncAnnotation(annotation: BookAnnotation)

    suspend fun getRecommendations(): List<Recommendation>

    suspend fun getClubs(): List<BookClub>

    suspend fun createClub(
        name: String,
        description: String,
        moderationLevel: String = "OWNER_ONLY",
    ): BookClub

    suspend fun joinClub(inviteCode: String): BookClub

    suspend fun getClubThreads(clubId: Long): List<DiscussionThread>

    suspend fun createClubThread(
        clubId: Long,
        title: String,
        bookId: String = "",
        chapterRef: String = "",
    ): DiscussionThread

    suspend fun getThreadPosts(threadId: Long): List<DiscussionPost>

    suspend fun createThreadPost(
        threadId: Long,
        message: String,
    ): DiscussionPost

    suspend fun getNotificationPreferences(): NotificationPreferences

    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): NotificationPreferences

    suspend fun addCustomBook(draft: CustomBookDraft)

    suspend fun importBooks(books: List<CustomBookDraft>): ImportJobSummary
}
