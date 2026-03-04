package com.example.chapterly.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface ChapterlyBackendApi {
    @GET("api/backend/product/reading")
    suspend fun getReadingSummary(): ReadingSummaryDto

    @PUT("api/backend/product/reading/goals")
    suspend fun upsertReadingGoal(
        @Body payload: UpsertReadingGoalRequestDto,
    ): ReadingGoalDto

    @POST("api/backend/product/reading/progress")
    suspend fun upsertReadingProgress(
        @Body payload: UpsertReadingProgressRequestDto,
    ): BookProgressDto

    @POST("api/backend/product/reading/annotations")
    suspend fun createAnnotation(
        @Body payload: CreateAnnotationRequestDto,
    )

    @GET("api/backend/product/recommendations")
    suspend fun getRecommendations(): List<RecommendationDto>

    @GET("api/backend/product/clubs")
    suspend fun getClubs(): List<BookClubDto>

    @POST("api/backend/product/clubs")
    suspend fun createClub(
        @Body payload: CreateClubRequestDto,
    ): BookClubDto

    @POST("api/backend/product/clubs/join")
    suspend fun joinClub(
        @Body payload: JoinClubRequestDto,
    ): BookClubDto

    @GET("api/backend/product/clubs/{clubId}/threads")
    suspend fun getClubThreads(
        @Path("clubId") clubId: Long,
    ): List<DiscussionThreadDto>

    @POST("api/backend/product/clubs/{clubId}/threads")
    suspend fun createClubThread(
        @Path("clubId") clubId: Long,
        @Body payload: CreateDiscussionThreadRequestDto,
    ): DiscussionThreadDto

    @GET("api/backend/product/threads/{threadId}/posts")
    suspend fun getThreadPosts(
        @Path("threadId") threadId: Long,
    ): List<DiscussionPostDto>

    @POST("api/backend/product/threads/{threadId}/posts")
    suspend fun createThreadPost(
        @Path("threadId") threadId: Long,
        @Body payload: CreateDiscussionPostRequestDto,
    ): DiscussionPostDto

    @GET("api/backend/product/notifications/preferences")
    suspend fun getNotificationPreferences(): NotificationPreferencesDto

    @PUT("api/backend/product/notifications/preferences")
    suspend fun updateNotificationPreferences(
        @Body payload: NotificationPreferencesRequestDto,
    ): NotificationPreferencesDto

    @POST("api/backend/product/library/custom-books")
    suspend fun addCustomBook(
        @Body payload: CreateCustomBookRequestDto,
    ): CustomBookDto

    @POST("api/backend/product/library/imports")
    suspend fun createImportJob(
        @Body payload: CreateImportJobRequestDto,
    ): ImportJobDto
}
