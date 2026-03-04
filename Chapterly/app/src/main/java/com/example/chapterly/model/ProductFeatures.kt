package com.example.chapterly.model

data class ReadingGoal(
    val cadence: String,
    val targetMinutes: Int,
    val targetPages: Int,
    val reminderEnabled: Boolean,
    val quietHours: String,
)

data class ReadingSummary(
    val goals: List<ReadingGoal>,
    val streakDays: Int,
    val totalMinutesRead: Int,
    val currentlyReadingCount: Int,
    val highlightsCount: Int,
    val notesCount: Int,
    val bookmarksCount: Int,
)

data class Recommendation(
    val bookId: String,
    val score: Int,
    val reason: String,
    val explanation: String,
)

data class BookClub(
    val id: Long,
    val name: String,
    val description: String,
    val inviteCode: String,
    val moderationLevel: String,
)

data class DiscussionThread(
    val id: Long,
    val clubId: Long,
    val bookId: String,
    val chapterRef: String,
    val title: String,
    val authorLabel: String,
    val createdAt: String,
)

data class DiscussionPost(
    val id: Long,
    val threadId: Long,
    val authorLabel: String,
    val message: String,
    val createdAt: String,
)

data class NotificationPreferences(
    val goalReminders: Boolean,
    val bookClubReplies: Boolean,
    val friendActivity: Boolean,
    val quietHours: String,
)

data class CustomBookDraft(
    val title: String,
    val authors: String,
    val isbn: String,
    val description: String,
    val externalRef: String,
)

data class ImportJobSummary(
    val importedCount: Int,
    val deduplicatedCount: Int,
    val status: String,
)
