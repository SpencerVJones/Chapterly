package com.example.chapterly.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReadingSummaryDto(
    val goals: List<ReadingGoalDto> = emptyList(),
    val streakDays: Int = 0,
    val totalMinutesRead: Int = 0,
    val currentlyReadingCount: Int = 0,
    val highlightsCount: Int = 0,
    val notesCount: Int = 0,
    val bookmarksCount: Int = 0,
    val activeProgress: List<BookProgressDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ReadingGoalDto(
    val cadence: String = "",
    val targetMinutes: Int = 0,
    val targetPages: Int = 0,
    val reminderEnabled: Boolean = false,
    val quietHours: String? = null,
    val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class BookProgressDto(
    val bookId: String = "",
    val percentComplete: Int = 0,
    val chaptersCompleted: Int = 0,
    val minutesRead: Int = 0,
    val currentShelf: String = "TO_READ",
    val lastReadAt: String? = null,
    val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class UpsertReadingGoalRequestDto(
    val cadence: String,
    val targetMinutes: Int,
    val targetPages: Int,
    val reminderEnabled: Boolean,
    val quietHours: String?,
)

@JsonClass(generateAdapter = true)
data class UpsertReadingProgressRequestDto(
    val bookId: String,
    val percentComplete: Int,
    val chaptersCompleted: Int,
    val minutesRead: Int,
    val currentShelf: String,
)

@JsonClass(generateAdapter = true)
data class CreateAnnotationRequestDto(
    val bookId: String,
    val annotationType: String,
    val content: String,
    val chapterLabel: String,
    val pageRef: String,
    val visibility: String,
)

@JsonClass(generateAdapter = true)
data class RecommendationDto(
    val bookId: String = "",
    val score: Int = 0,
    val reason: String = "",
    val explanation: String = "",
)

@JsonClass(generateAdapter = true)
data class BookClubDto(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val inviteCode: String = "",
    val moderationLevel: String = "",
    val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class DiscussionThreadDto(
    val id: Long = 0,
    val clubId: Long = 0,
    val bookId: String? = null,
    val chapterRef: String? = null,
    val title: String = "",
    val createdByUserId: Long = 0,
    val authorLabel: String = "",
    val createdAt: String = "",
)

@JsonClass(generateAdapter = true)
data class CreateDiscussionThreadRequestDto(
    val title: String,
    val bookId: String?,
    val chapterRef: String?,
)

@JsonClass(generateAdapter = true)
data class DiscussionPostDto(
    val id: Long = 0,
    val threadId: Long = 0,
    val userId: Long = 0,
    val authorLabel: String = "",
    val message: String = "",
    val createdAt: String = "",
)

@JsonClass(generateAdapter = true)
data class CreateDiscussionPostRequestDto(
    val message: String,
)

@JsonClass(generateAdapter = true)
data class CreateClubRequestDto(
    val name: String,
    val description: String,
    val moderationLevel: String,
)

@JsonClass(generateAdapter = true)
data class JoinClubRequestDto(
    val inviteCode: String,
)

@JsonClass(generateAdapter = true)
data class NotificationPreferencesDto(
    val goalReminders: Boolean = true,
    val bookClubReplies: Boolean = true,
    val friendActivity: Boolean = true,
    val quietHours: String? = null,
    val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class NotificationPreferencesRequestDto(
    val goalReminders: Boolean,
    val bookClubReplies: Boolean,
    val friendActivity: Boolean,
    val quietHours: String?,
)

@JsonClass(generateAdapter = true)
data class CreateCustomBookRequestDto(
    val sourceType: String,
    val title: String,
    val authors: String,
    val isbn: String,
    val description: String,
    val externalRef: String,
)

@JsonClass(generateAdapter = true)
data class CustomBookDto(
    val id: Long = 0,
    val deduplicated: Boolean = false,
    val sourceType: String = "",
    val title: String = "",
    val authors: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val externalRef: String? = null,
    val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class CreateImportJobRequestDto(
    val sourceType: String,
    val items: List<CreateCustomBookRequestDto>,
)

@JsonClass(generateAdapter = true)
data class ImportJobDto(
    val id: Long = 0,
    val sourceType: String = "",
    val status: String = "",
    val importedCount: Int = 0,
    val deduplicatedCount: Int = 0,
    val updatedAt: String = "",
)
