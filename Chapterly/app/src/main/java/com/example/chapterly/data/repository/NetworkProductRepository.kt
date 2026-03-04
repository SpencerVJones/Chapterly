package com.example.chapterly.data.repository

import com.example.chapterly.data.remote.BookClubDto
import com.example.chapterly.data.remote.ChapterlyBackendApi
import com.example.chapterly.data.remote.CreateAnnotationRequestDto
import com.example.chapterly.data.remote.CreateClubRequestDto
import com.example.chapterly.data.remote.CreateCustomBookRequestDto
import com.example.chapterly.data.remote.CreateDiscussionPostRequestDto
import com.example.chapterly.data.remote.CreateDiscussionThreadRequestDto
import com.example.chapterly.data.remote.CreateImportJobRequestDto
import com.example.chapterly.data.remote.DiscussionPostDto
import com.example.chapterly.data.remote.DiscussionThreadDto
import com.example.chapterly.data.remote.JoinClubRequestDto
import com.example.chapterly.data.remote.NotificationPreferencesDto
import com.example.chapterly.data.remote.NotificationPreferencesRequestDto
import com.example.chapterly.data.remote.ReadingGoalDto
import com.example.chapterly.data.remote.ReadingSummaryDto
import com.example.chapterly.data.remote.RecommendationDto
import com.example.chapterly.data.remote.UpsertReadingGoalRequestDto
import com.example.chapterly.data.remote.UpsertReadingProgressRequestDto
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkProductRepository
    @Inject
    constructor(
        private val backendApi: ChapterlyBackendApi,
    ) : ProductRepository {
        override suspend fun getReadingSummary(): ReadingSummary {
            return backendApi.getReadingSummary().toDomain()
        }

        override suspend fun upsertReadingGoal(goal: ReadingGoal): ReadingGoal {
            return backendApi.upsertReadingGoal(goal.toRequest()).toDomain()
        }

        override suspend fun syncReadingProgress(progress: ReadingProgress) {
            backendApi.upsertReadingProgress(progress.toRequest())
        }

        override suspend fun syncAnnotation(annotation: BookAnnotation) {
            backendApi.createAnnotation(annotation.toRequest())
        }

        override suspend fun getRecommendations(): List<Recommendation> {
            return backendApi.getRecommendations().map { recommendation -> recommendation.toDomain() }
        }

        override suspend fun getClubs(): List<BookClub> {
            return backendApi.getClubs().map { club -> club.toDomain() }
        }

        override suspend fun createClub(
            name: String,
            description: String,
            moderationLevel: String,
        ): BookClub {
            return backendApi.createClub(
                CreateClubRequestDto(
                    name = name.trim(),
                    description = description.trim(),
                    moderationLevel = moderationLevel,
                ),
            ).toDomain()
        }

        override suspend fun joinClub(inviteCode: String): BookClub {
            return backendApi.joinClub(
                JoinClubRequestDto(
                    inviteCode = inviteCode.trim(),
                ),
            ).toDomain()
        }

        override suspend fun getClubThreads(clubId: Long): List<DiscussionThread> {
            return backendApi.getClubThreads(clubId).map { thread -> thread.toDomain() }
        }

        override suspend fun createClubThread(
            clubId: Long,
            title: String,
            bookId: String,
            chapterRef: String,
        ): DiscussionThread {
            return backendApi.createClubThread(
                clubId = clubId,
                payload =
                    CreateDiscussionThreadRequestDto(
                        title = title.trim(),
                        bookId = bookId.trim().ifBlank { null },
                        chapterRef = chapterRef.trim().ifBlank { null },
                    ),
            ).toDomain()
        }

        override suspend fun getThreadPosts(threadId: Long): List<DiscussionPost> {
            return backendApi.getThreadPosts(threadId).map { post -> post.toDomain() }
        }

        override suspend fun createThreadPost(
            threadId: Long,
            message: String,
        ): DiscussionPost {
            return backendApi.createThreadPost(
                threadId = threadId,
                payload = CreateDiscussionPostRequestDto(message = message.trim()),
            ).toDomain()
        }

        override suspend fun getNotificationPreferences(): NotificationPreferences {
            return backendApi.getNotificationPreferences().toDomain()
        }

        override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): NotificationPreferences {
            return backendApi.updateNotificationPreferences(preferences.toRequest()).toDomain()
        }

        override suspend fun addCustomBook(draft: CustomBookDraft) {
            backendApi.addCustomBook(draft.toRequest())
        }

        override suspend fun importBooks(books: List<CustomBookDraft>): ImportJobSummary {
            return backendApi.createImportJob(
                CreateImportJobRequestDto(
                    sourceType = "GOODREADS_CSV",
                    items = books.map { draft -> draft.toRequest() },
                ),
            ).let { response ->
                ImportJobSummary(
                    importedCount = response.importedCount,
                    deduplicatedCount = response.deduplicatedCount,
                    status = response.status,
                )
            }
        }
    }

private fun ReadingSummaryDto.toDomain(): ReadingSummary {
    return ReadingSummary(
        goals = goals.map { goal -> goal.toDomain() },
        streakDays = streakDays,
        totalMinutesRead = totalMinutesRead,
        currentlyReadingCount = currentlyReadingCount,
        highlightsCount = highlightsCount,
        notesCount = notesCount,
        bookmarksCount = bookmarksCount,
    )
}

private fun ReadingGoalDto.toDomain(): ReadingGoal {
    return ReadingGoal(
        cadence = cadence,
        targetMinutes = targetMinutes,
        targetPages = targetPages,
        reminderEnabled = reminderEnabled,
        quietHours = quietHours.orEmpty(),
    )
}

private fun ReadingGoal.toRequest(): UpsertReadingGoalRequestDto {
    return UpsertReadingGoalRequestDto(
        cadence = cadence,
        targetMinutes = targetMinutes,
        targetPages = targetPages,
        reminderEnabled = reminderEnabled,
        quietHours = quietHours.ifBlank { null },
    )
}

private fun ReadingProgress.toRequest(): UpsertReadingProgressRequestDto {
    return UpsertReadingProgressRequestDto(
        bookId = bookId,
        percentComplete = percentComplete,
        chaptersCompleted = chaptersCompleted,
        minutesRead = minutesRead,
        currentShelf = currentShelf,
    )
}

private fun BookAnnotation.toRequest(): CreateAnnotationRequestDto {
    return CreateAnnotationRequestDto(
        bookId = bookId,
        annotationType = annotationType,
        content = content,
        chapterLabel = chapterLabel,
        pageRef = pageRef,
        visibility = visibility,
    )
}

private fun RecommendationDto.toDomain(): Recommendation {
    return Recommendation(
        bookId = bookId,
        score = score,
        reason = reason,
        explanation = explanation,
    )
}

private fun BookClubDto.toDomain(): BookClub {
    return BookClub(
        id = id,
        name = name,
        description = description.orEmpty(),
        inviteCode = inviteCode,
        moderationLevel = moderationLevel,
    )
}

private fun DiscussionThreadDto.toDomain(): DiscussionThread {
    return DiscussionThread(
        id = id,
        clubId = clubId,
        bookId = bookId.orEmpty(),
        chapterRef = chapterRef.orEmpty(),
        title = title,
        authorLabel = authorLabel,
        createdAt = createdAt,
    )
}

private fun DiscussionPostDto.toDomain(): DiscussionPost {
    return DiscussionPost(
        id = id,
        threadId = threadId,
        authorLabel = authorLabel,
        message = message,
        createdAt = createdAt,
    )
}

private fun NotificationPreferencesDto.toDomain(): NotificationPreferences {
    return NotificationPreferences(
        goalReminders = goalReminders,
        bookClubReplies = bookClubReplies,
        friendActivity = friendActivity,
        quietHours = quietHours.orEmpty(),
    )
}

private fun NotificationPreferences.toRequest(): NotificationPreferencesRequestDto {
    return NotificationPreferencesRequestDto(
        goalReminders = goalReminders,
        bookClubReplies = bookClubReplies,
        friendActivity = friendActivity,
        quietHours = quietHours.ifBlank { null },
    )
}

private fun CustomBookDraft.toRequest(): CreateCustomBookRequestDto {
    return CreateCustomBookRequestDto(
        sourceType = "MANUAL",
        title = title.trim(),
        authors = authors.trim(),
        isbn = isbn.trim(),
        description = description.trim(),
        externalRef = externalRef.trim(),
    )
}
