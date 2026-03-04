package com.example.chapterly.backend.controller

import com.example.chapterly.backend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/backend/product")
class ProductController(
    private val userService: UserService,
    private val jdbcTemplate: JdbcTemplate,
) {
    @GetMapping("/reading")
    fun readingSummary(request: HttpServletRequest): ReadingSummaryResponse {
        val user = userService.requireUser(request)
        val userId = user.id!!
        val goals =
            jdbcTemplate.queryForList(
                """
                SELECT cadence, target_minutes, target_pages, reminder_enabled, quiet_hours, updated_at
                FROM reading_goals
                WHERE user_id = ?
                ORDER BY cadence ASC
                """.trimIndent(),
                userId,
            ).map { row ->
                ReadingGoalResponse(
                    cadence = row["cadence"].asString(),
                    targetMinutes = row["target_minutes"].asInt(),
                    targetPages = row["target_pages"].asInt(),
                    reminderEnabled = row["reminder_enabled"].asBoolean(),
                    quietHours = row["quiet_hours"]?.toString(),
                    updatedAt = row["updated_at"].asInstantString(),
                )
            }

        val currentlyReading =
            count(
                """
                SELECT COUNT(*) FROM book_progress
                WHERE user_id = ? AND current_shelf = 'CURRENTLY_READING'
                """.trimIndent(),
                userId,
            )

        val totalMinutesRead =
            count(
                """
                SELECT COALESCE(SUM(minutes_read), 0) FROM book_progress
                WHERE user_id = ?
                """.trimIndent(),
                userId,
            )

        val historyDays =
            jdbcTemplate.queryForList(
                """
                SELECT DISTINCT CAST(last_read_at AS DATE) AS read_day
                FROM book_progress
                WHERE user_id = ? AND last_read_at IS NOT NULL
                ORDER BY read_day DESC
                """.trimIndent(),
                userId,
            ).mapNotNull { row -> row["read_day"].toLocalDateOrNull() }

        val annotationBreakdown =
            jdbcTemplate.queryForList(
                """
                SELECT annotation_type, COUNT(*) AS total
                FROM annotations
                WHERE user_id = ?
                GROUP BY annotation_type
                """.trimIndent(),
                userId,
            ).associate { row ->
                row["annotation_type"].asString() to row["total"].asInt()
            }

        val activeProgress =
            jdbcTemplate.queryForList(
                """
                SELECT book_id, percent_complete, chapters_completed, minutes_read, current_shelf, last_read_at, updated_at
                FROM book_progress
                WHERE user_id = ?
                ORDER BY updated_at DESC
                LIMIT 5
                """.trimIndent(),
                userId,
            ).map { row ->
                BookProgressResponse(
                    bookId = row["book_id"].asString(),
                    percentComplete = row["percent_complete"].asInt(),
                    chaptersCompleted = row["chapters_completed"].asInt(),
                    minutesRead = row["minutes_read"].asInt(),
                    currentShelf = row["current_shelf"].asString(),
                    lastReadAt = row["last_read_at"].asInstantString(),
                    updatedAt = row["updated_at"].asInstantString(),
                )
            }

        return ReadingSummaryResponse(
            goals = goals,
            streakDays = calculateStreakDays(historyDays),
            totalMinutesRead = totalMinutesRead,
            currentlyReadingCount = currentlyReading,
            highlightsCount = annotationBreakdown["HIGHLIGHT"] ?: 0,
            notesCount = annotationBreakdown["NOTE"] ?: 0,
            bookmarksCount = annotationBreakdown["BOOKMARK"] ?: 0,
            activeProgress = activeProgress,
        )
    }

    @PutMapping("/reading/goals")
    @Transactional
    fun upsertReadingGoal(
        request: HttpServletRequest,
        @RequestBody @Valid payload: UpsertReadingGoalRequest,
    ): ReadingGoalResponse {
        val user = userService.requireUser(request)
        val userId = user.id!!
        val cadence = payload.cadence.trim().uppercase()
        val now = Timestamp.from(Instant.now())
        val existingId =
            jdbcTemplate.query(
                "SELECT id FROM reading_goals WHERE user_id = ? AND cadence = ?",
                { rs, _ -> rs.getLong("id") },
                userId,
                cadence,
            ).firstOrNull()

        if (existingId == null) {
            jdbcTemplate.update(
                """
                INSERT INTO reading_goals (user_id, cadence, target_minutes, target_pages, reminder_enabled, quiet_hours, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                userId,
                cadence,
                payload.targetMinutes,
                payload.targetPages,
                payload.reminderEnabled,
                payload.quietHours?.trim(),
                now,
            )
        } else {
            jdbcTemplate.update(
                """
                UPDATE reading_goals
                SET target_minutes = ?, target_pages = ?, reminder_enabled = ?, quiet_hours = ?, updated_at = ?
                WHERE id = ?
                """.trimIndent(),
                payload.targetMinutes,
                payload.targetPages,
                payload.reminderEnabled,
                payload.quietHours?.trim(),
                now,
                existingId,
            )
        }

        return ReadingGoalResponse(
            cadence = cadence,
            targetMinutes = payload.targetMinutes,
            targetPages = payload.targetPages,
            reminderEnabled = payload.reminderEnabled,
            quietHours = payload.quietHours?.trim(),
            updatedAt = now.toInstant().toString(),
        )
    }

    @PostMapping("/reading/progress")
    @Transactional
    fun upsertProgress(
        request: HttpServletRequest,
        @RequestBody @Valid payload: UpsertBookProgressRequest,
    ): BookProgressResponse {
        val user = userService.requireUser(request)
        val userId = user.id!!
        val now = Instant.now()
        val bookId = payload.bookId.trim()
        val shelf = payload.currentShelf.trim().uppercase()
        val existingId =
            jdbcTemplate.query(
                "SELECT id FROM book_progress WHERE user_id = ? AND book_id = ?",
                { rs, _ -> rs.getLong("id") },
                userId,
                bookId,
            ).firstOrNull()

        if (existingId == null) {
            jdbcTemplate.update(
                """
                INSERT INTO book_progress (
                    user_id, book_id, percent_complete, chapters_completed, minutes_read, current_shelf, last_read_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                userId,
                bookId,
                payload.percentComplete,
                payload.chaptersCompleted,
                payload.minutesRead,
                shelf,
                Timestamp.from(now),
                Timestamp.from(now),
            )
        } else {
            jdbcTemplate.update(
                """
                UPDATE book_progress
                SET percent_complete = ?, chapters_completed = ?, minutes_read = ?, current_shelf = ?, last_read_at = ?, updated_at = ?
                WHERE id = ?
                """.trimIndent(),
                payload.percentComplete,
                payload.chaptersCompleted,
                payload.minutesRead,
                shelf,
                Timestamp.from(now),
                Timestamp.from(now),
                existingId,
            )
        }

        val eventType =
            when {
                payload.percentComplete >= 100 -> "FINISHED_READING"
                shelf == "CURRENTLY_READING" -> "STARTED_READING"
                else -> "READING_PROGRESS"
            }
        insertActivityEvent(
            userId = userId,
            eventType = eventType,
            bookId = bookId,
            detail = payload.progressLabel().take(500),
            visibility = "FRIENDS",
        )

        return BookProgressResponse(
            bookId = bookId,
            percentComplete = payload.percentComplete,
            chaptersCompleted = payload.chaptersCompleted,
            minutesRead = payload.minutesRead,
            currentShelf = shelf,
            lastReadAt = now.toString(),
            updatedAt = now.toString(),
        )
    }

    @PostMapping("/reading/annotations")
    @Transactional
    fun addAnnotation(
        request: HttpServletRequest,
        @RequestBody @Valid payload: CreateAnnotationRequest,
    ): AnnotationResponse {
        val user = userService.requireUser(request)
        val userId = user.id!!
        val now = Instant.now()
        jdbcTemplate.update(
            """
            INSERT INTO annotations (user_id, book_id, annotation_type, content, chapter_label, page_ref, visibility, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            userId,
            payload.bookId.trim(),
            payload.annotationType.trim().uppercase(),
            payload.content?.trim(),
            payload.chapterLabel?.trim(),
            payload.pageRef?.trim(),
            payload.visibility.trim().uppercase(),
            Timestamp.from(now),
        )

        if (!payload.content.isNullOrBlank()) {
            insertActivityEvent(
                userId = userId,
                eventType = "ANNOTATED",
                bookId = payload.bookId.trim(),
                detail = payload.content.trim().take(240),
                visibility = payload.visibility.trim().uppercase(),
            )
        }

        val annotationId = count("SELECT MAX(id) FROM annotations WHERE user_id = ?", userId).toLong()
        return AnnotationResponse(
            id = annotationId,
            bookId = payload.bookId.trim(),
            annotationType = payload.annotationType.trim().uppercase(),
            content = payload.content?.trim(),
            chapterLabel = payload.chapterLabel?.trim(),
            pageRef = payload.pageRef?.trim(),
            visibility = payload.visibility.trim().uppercase(),
            updatedAt = now.toString(),
        )
    }

    @GetMapping("/recommendations")
    fun recommendations(
        request: HttpServletRequest,
        @RequestParam(required = false) seedBookId: String?,
    ): List<RecommendationResponse> {
        val user = userService.requireUser(request)
        val userId = user.id!!
        val scoredBooks =
            jdbcTemplate.queryForList(
                """
                SELECT book_id, SUM(weight) AS score
                FROM (
                    SELECT book_id, 4 AS weight FROM favorites WHERE user_id = ?
                    UNION ALL
                    SELECT book_id, rating AS weight FROM reviews WHERE user_id = ?
                    UNION ALL
                    SELECT book_id, CASE WHEN minutes_read > 0 THEN 3 ELSE 1 END AS weight FROM book_progress WHERE user_id = ?
                    UNION ALL
                    SELECT li.book_id, 2 AS weight
                    FROM list_items li
                    INNER JOIN lists l ON l.id = li.list_id
                    WHERE l.user_id = ?
                ) ranked
                GROUP BY book_id
                ORDER BY score DESC, book_id ASC
                LIMIT 8
                """.trimIndent(),
                userId,
                userId,
                userId,
                userId,
            ).map { row ->
                RecommendationResponse(
                    bookId = row["book_id"].asString(),
                    score = row["score"].asInt(),
                    reason =
                        if (!seedBookId.isNullOrBlank()) {
                            "Because you liked ${seedBookId.trim()}"
                        } else {
                            "Ranked from favorites, reading time, lists, and ratings"
                        },
                    explanation = "More like the books you have engaged with the most.",
                )
            }

        if (scoredBooks.isNotEmpty()) {
            return scoredBooks
        }

        val fallback =
            jdbcTemplate.queryForList(
                """
                SELECT DISTINCT book_id
                FROM reviews
                ORDER BY book_id ASC
                LIMIT 5
                """.trimIndent(),
            )

        return fallback.map { row ->
            val bookId = row["book_id"].asString()
            RecommendationResponse(
                bookId = bookId,
                score = 1,
                reason = "Popular in your review graph",
                explanation = "Fallback recommendation while your personal taste profile warms up.",
            )
        }
    }

    @GetMapping("/social/friends")
    fun friends(request: HttpServletRequest): List<SocialConnectionResponse> {
        val user = userService.requireUser(request)
        return jdbcTemplate.queryForList(
            """
            SELECT sc.id, sc.friend_user_id, COALESCE(u.display_name, u.email, CONCAT('Reader ', u.id)) AS friend_label,
                   sc.status, sc.visibility, sc.created_at
            FROM social_connections sc
            INNER JOIN users u ON u.id = sc.friend_user_id
            WHERE sc.user_id = ?
            ORDER BY sc.created_at DESC
            """.trimIndent(),
            user.id!!,
        ).map { row ->
            SocialConnectionResponse(
                id = row["id"].asLong(),
                friendUserId = row["friend_user_id"].asLong(),
                friendLabel = row["friend_label"].asString(),
                status = row["status"].asString(),
                visibility = row["visibility"].asString(),
                createdAt = row["created_at"].asInstantString(),
            )
        }
    }

    @PostMapping("/social/friends")
    @Transactional
    fun followFriend(
        request: HttpServletRequest,
        @RequestBody @Valid payload: CreateFriendRequest,
    ): SocialConnectionResponse {
        val user = userService.requireUser(request)
        if (payload.friendUserId == user.id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot follow yourself")
        }

        requireUserExists(payload.friendUserId)
        val now = Instant.now()
        val existingId =
            jdbcTemplate.query(
                "SELECT id FROM social_connections WHERE user_id = ? AND friend_user_id = ?",
                { rs, _ -> rs.getLong("id") },
                user.id!!,
                payload.friendUserId,
            ).firstOrNull()

        if (existingId == null) {
            jdbcTemplate.update(
                """
                INSERT INTO social_connections (user_id, friend_user_id, status, visibility, created_at)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                user.id!!,
                payload.friendUserId,
                "FOLLOWING",
                payload.visibility.trim().uppercase(),
                Timestamp.from(now),
            )
        } else {
            jdbcTemplate.update(
                "UPDATE social_connections SET visibility = ? WHERE id = ?",
                payload.visibility.trim().uppercase(),
                existingId,
            )
        }

        insertActivityEvent(
            userId = user.id!!,
            eventType = "FOLLOWED_FRIEND",
            detail = "Following user ${payload.friendUserId}",
            visibility = "PRIVATE",
        )

        return SocialConnectionResponse(
            id = existingId ?: count("SELECT MAX(id) FROM social_connections WHERE user_id = ?", user.id!!).toLong(),
            friendUserId = payload.friendUserId,
            friendLabel = lookupUserLabel(payload.friendUserId),
            status = "FOLLOWING",
            visibility = payload.visibility.trim().uppercase(),
            createdAt = now.toString(),
        )
    }

    @GetMapping("/social/feed")
    fun socialFeed(request: HttpServletRequest): List<ActivityFeedResponse> {
        val user = userService.requireUser(request)
        val followedIds =
            jdbcTemplate.query(
                "SELECT friend_user_id FROM social_connections WHERE user_id = ?",
                { rs, _ -> rs.getLong("friend_user_id") },
                user.id!!,
            )
        val ids = (followedIds + user.id!!).distinct()
        if (ids.isEmpty()) {
            return emptyList()
        }
        val placeholders = ids.joinToString(separator = ",") { "?" }
        val args = ids.toTypedArray()
        return jdbcTemplate.queryForList(
            """
            SELECT ae.id, ae.user_id, COALESCE(u.display_name, u.email, CONCAT('Reader ', u.id)) AS actor_label,
                   ae.event_type, ae.book_id, ae.detail, ae.visibility, ae.created_at
            FROM activity_events ae
            INNER JOIN users u ON u.id = ae.user_id
            WHERE ae.user_id IN ($placeholders)
            ORDER BY ae.created_at DESC
            LIMIT 25
            """.trimIndent(),
            *args,
        ).map { row ->
            ActivityFeedResponse(
                id = row["id"].asLong(),
                actorUserId = row["user_id"].asLong(),
                actorLabel = row["actor_label"].asString(),
                eventType = row["event_type"].asString(),
                bookId = row["book_id"]?.toString(),
                detail = row["detail"]?.toString(),
                visibility = row["visibility"].asString(),
                createdAt = row["created_at"].asInstantString(),
            )
        }
    }

    @PostMapping("/social/activity")
    @Transactional
    fun publishActivity(
        request: HttpServletRequest,
        @RequestBody @Valid payload: PublishActivityRequest,
    ): ActivityFeedResponse {
        val user = userService.requireUser(request)
        val recentCount =
            count(
                """
                SELECT COUNT(*) FROM activity_events
                WHERE user_id = ? AND created_at >= ?
                """.trimIndent(),
                user.id!!,
                Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)),
            )
        if (recentCount >= 50) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Activity rate limit exceeded")
        }

        val now = Instant.now()
        jdbcTemplate.update(
            """
            INSERT INTO activity_events (user_id, event_type, book_id, detail, visibility, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            user.id!!,
            payload.eventType.trim().uppercase(),
            payload.bookId?.trim(),
            payload.detail?.trim(),
            payload.visibility.trim().uppercase(),
            Timestamp.from(now),
        )

        return ActivityFeedResponse(
            id = count("SELECT MAX(id) FROM activity_events WHERE user_id = ?", user.id!!).toLong(),
            actorUserId = user.id!!,
            actorLabel = user.displayName ?: user.email ?: "Reader ${user.id}",
            eventType = payload.eventType.trim().uppercase(),
            bookId = payload.bookId?.trim(),
            detail = payload.detail?.trim(),
            visibility = payload.visibility.trim().uppercase(),
            createdAt = now.toString(),
        )
    }

    @GetMapping("/clubs")
    fun clubs(request: HttpServletRequest): List<BookClubResponse> {
        val user = userService.requireUser(request)
        return jdbcTemplate.queryForList(
            """
            SELECT bc.id, bc.name, bc.description, bc.invite_code, bc.moderation_level, bc.updated_at
            FROM book_clubs bc
            INNER JOIN book_club_memberships bcm ON bcm.club_id = bc.id
            WHERE bcm.user_id = ?
            ORDER BY bc.updated_at DESC
            """.trimIndent(),
            user.id!!,
        ).map { row ->
            BookClubResponse(
                id = row["id"].asLong(),
                name = row["name"].asString(),
                description = row["description"]?.toString(),
                inviteCode = row["invite_code"].asString(),
                moderationLevel = row["moderation_level"].asString(),
                updatedAt = row["updated_at"].asInstantString(),
            )
        }
    }

    @PostMapping("/clubs")
    @Transactional
    fun createClub(
        request: HttpServletRequest,
        @RequestBody @Valid payload: CreateClubRequest,
    ): BookClubResponse {
        val user = userService.requireUser(request)
        val now = Instant.now()
        val inviteCode = UUID.randomUUID().toString().replace("-", "").take(12)
        jdbcTemplate.update(
            """
            INSERT INTO book_clubs (owner_user_id, name, description, invite_code, moderation_level, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            user.id!!,
            payload.name.trim(),
            payload.description?.trim(),
            inviteCode,
            payload.moderationLevel.trim().uppercase(),
            Timestamp.from(now),
            Timestamp.from(now),
        )
        val clubId = count("SELECT MAX(id) FROM book_clubs WHERE owner_user_id = ?", user.id!!).toLong()
        jdbcTemplate.update(
            """
            INSERT INTO book_club_memberships (club_id, user_id, role, created_at)
            VALUES (?, ?, 'OWNER', ?)
            """.trimIndent(),
            clubId,
            user.id!!,
            Timestamp.from(now),
        )
        return BookClubResponse(
            id = clubId,
            name = payload.name.trim(),
            description = payload.description?.trim(),
            inviteCode = inviteCode,
            moderationLevel = payload.moderationLevel.trim().uppercase(),
            updatedAt = now.toString(),
        )
    }

    @PostMapping("/clubs/join")
    @Transactional
    fun joinClub(
        request: HttpServletRequest,
        @RequestBody @Valid payload: JoinClubRequest,
    ): BookClubResponse {
        val user = userService.requireUser(request)
        val clubRow =
            jdbcTemplate.queryForList(
                """
                SELECT id, name, description, invite_code, moderation_level, updated_at
                FROM book_clubs
                WHERE invite_code = ?
                """.trimIndent(),
                payload.inviteCode.trim(),
            ).firstOrNull()
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invite code not found")

        val clubId = clubRow["id"].asLong()
        val existingMembership =
            count(
                """
                SELECT COUNT(*) FROM book_club_memberships
                WHERE club_id = ? AND user_id = ?
                """.trimIndent(),
                clubId,
                user.id!!,
            )
        if (existingMembership == 0) {
            jdbcTemplate.update(
                """
                INSERT INTO book_club_memberships (club_id, user_id, role, created_at)
                VALUES (?, ?, 'MEMBER', ?)
                """.trimIndent(),
                clubId,
                user.id!!,
                Timestamp.from(Instant.now()),
            )
        }

        return BookClubResponse(
            id = clubId,
            name = clubRow["name"].asString(),
            description = clubRow["description"]?.toString(),
            inviteCode = clubRow["invite_code"].asString(),
            moderationLevel = clubRow["moderation_level"].asString(),
            updatedAt = clubRow["updated_at"].asInstantString(),
        )
    }

    @GetMapping("/clubs/{clubId}/threads")
    fun threads(
        request: HttpServletRequest,
        @PathVariable clubId: Long,
    ): List<DiscussionThreadResponse> {
        val user = userService.requireUser(request)
        requireClubMembership(clubId, user.id!!)
        return jdbcTemplate.queryForList(
            """
            SELECT dt.id, dt.book_id, dt.chapter_ref, dt.title, dt.created_by_user_id, dt.created_at,
                   COALESCE(u.display_name, u.email, CONCAT('Reader ', u.id)) AS author_label
            FROM discussion_threads dt
            INNER JOIN users u ON u.id = dt.created_by_user_id
            WHERE dt.club_id = ?
            ORDER BY dt.created_at DESC
            """.trimIndent(),
            clubId,
        ).map { row ->
            DiscussionThreadResponse(
                id = row["id"].asLong(),
                clubId = clubId,
                bookId = row["book_id"]?.toString(),
                chapterRef = row["chapter_ref"]?.toString(),
                title = row["title"].asString(),
                createdByUserId = row["created_by_user_id"].asLong(),
                authorLabel = row["author_label"].asString(),
                createdAt = row["created_at"].asInstantString(),
            )
        }
    }

    @PostMapping("/clubs/{clubId}/threads")
    @Transactional
    fun createThread(
        request: HttpServletRequest,
        @PathVariable clubId: Long,
        @RequestBody @Valid payload: CreateDiscussionThreadRequest,
    ): DiscussionThreadResponse {
        val user = userService.requireUser(request)
        requireClubMembership(clubId, user.id!!)
        val now = Instant.now()
        jdbcTemplate.update(
            """
            INSERT INTO discussion_threads (club_id, book_id, chapter_ref, title, created_by_user_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            clubId,
            payload.bookId?.trim(),
            payload.chapterRef?.trim(),
            payload.title.trim(),
            user.id!!,
            Timestamp.from(now),
        )
        val threadId = count("SELECT MAX(id) FROM discussion_threads WHERE club_id = ?", clubId).toLong()
        return DiscussionThreadResponse(
            id = threadId,
            clubId = clubId,
            bookId = payload.bookId?.trim(),
            chapterRef = payload.chapterRef?.trim(),
            title = payload.title.trim(),
            createdByUserId = user.id!!,
            authorLabel = user.displayName ?: user.email ?: "Reader ${user.id}",
            createdAt = now.toString(),
        )
    }

    @GetMapping("/threads/{threadId}/posts")
    fun threadPosts(
        request: HttpServletRequest,
        @PathVariable threadId: Long,
    ): List<DiscussionPostResponse> {
        val user = userService.requireUser(request)
        val clubId =
            jdbcTemplate.query(
                "SELECT club_id FROM discussion_threads WHERE id = ?",
                { rs, _ -> rs.getLong("club_id") },
                threadId,
            ).firstOrNull()
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found")
        requireClubMembership(clubId, user.id!!)
        return jdbcTemplate.queryForList(
            """
            SELECT dp.id, dp.thread_id, dp.user_id, dp.message, dp.created_at,
                   COALESCE(u.display_name, u.email, CONCAT('Reader ', u.id)) AS author_label
            FROM discussion_posts dp
            INNER JOIN users u ON u.id = dp.user_id
            WHERE dp.thread_id = ?
            ORDER BY dp.created_at ASC
            """.trimIndent(),
            threadId,
        ).map { row ->
            DiscussionPostResponse(
                id = row["id"].asLong(),
                threadId = row["thread_id"].asLong(),
                userId = row["user_id"].asLong(),
                authorLabel = row["author_label"].asString(),
                message = row["message"].asString(),
                createdAt = row["created_at"].asInstantString(),
            )
        }
    }

    @PostMapping("/threads/{threadId}/posts")
    @Transactional
    fun createThreadPost(
        request: HttpServletRequest,
        @PathVariable threadId: Long,
        @RequestBody @Valid payload: CreateDiscussionPostRequest,
    ): DiscussionPostResponse {
        val user = userService.requireUser(request)
        val clubId =
            jdbcTemplate.query(
                "SELECT club_id FROM discussion_threads WHERE id = ?",
                { rs, _ -> rs.getLong("club_id") },
                threadId,
            ).firstOrNull()
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found")
        requireClubMembership(clubId, user.id!!)
        val now = Instant.now()
        jdbcTemplate.update(
            """
            INSERT INTO discussion_posts (thread_id, user_id, message, created_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            threadId,
            user.id!!,
            payload.message.trim(),
            Timestamp.from(now),
        )
        return DiscussionPostResponse(
            id = count("SELECT MAX(id) FROM discussion_posts WHERE thread_id = ?", threadId).toLong(),
            threadId = threadId,
            userId = user.id!!,
            authorLabel = user.displayName ?: user.email ?: "Reader ${user.id}",
            message = payload.message.trim(),
            createdAt = now.toString(),
        )
    }

    @GetMapping("/lists/{listId}/collaborators")
    fun listCollaborators(
        request: HttpServletRequest,
        @PathVariable listId: Long,
    ): List<ListCollaboratorResponse> {
        val user = userService.requireUser(request)
        requireListAccess(listId, user.id!!)
        return jdbcTemplate.queryForList(
            """
            SELECT lc.id, lc.user_id, COALESCE(u.display_name, u.email, CONCAT('Reader ', u.id)) AS user_label,
                   lc.permission, lc.created_at
            FROM list_collaborators lc
            INNER JOIN users u ON u.id = lc.user_id
            WHERE lc.list_id = ?
            ORDER BY lc.created_at ASC
            """.trimIndent(),
            listId,
        ).map { row ->
            ListCollaboratorResponse(
                id = row["id"].asLong(),
                userId = row["user_id"].asLong(),
                userLabel = row["user_label"].asString(),
                permission = row["permission"].asString(),
                createdAt = row["created_at"].asInstantString(),
            )
        }
    }

    @PostMapping("/lists/{listId}/collaborators")
    @Transactional
    fun addListCollaborator(
        request: HttpServletRequest,
        @PathVariable listId: Long,
        @RequestBody @Valid payload: AddListCollaboratorRequest,
    ): ListCollaboratorResponse {
        val user = userService.requireUser(request)
        requireListOwner(listId, user.id!!)
        requireUserExists(payload.userId)

        val existingId =
            jdbcTemplate.query(
                "SELECT id FROM list_collaborators WHERE list_id = ? AND user_id = ?",
                { rs, _ -> rs.getLong("id") },
                listId,
                payload.userId,
            ).firstOrNull()
        val now = Instant.now()
        if (existingId == null) {
            jdbcTemplate.update(
                """
                INSERT INTO list_collaborators (list_id, user_id, permission, created_at)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                listId,
                payload.userId,
                payload.permission.trim().uppercase(),
                Timestamp.from(now),
            )
        } else {
            jdbcTemplate.update(
                "UPDATE list_collaborators SET permission = ? WHERE id = ?",
                payload.permission.trim().uppercase(),
                existingId,
            )
        }
        return ListCollaboratorResponse(
            id = existingId ?: count("SELECT MAX(id) FROM list_collaborators WHERE list_id = ?", listId).toLong(),
            userId = payload.userId,
            userLabel = lookupUserLabel(payload.userId),
            permission = payload.permission.trim().uppercase(),
            createdAt = now.toString(),
        )
    }

    @GetMapping("/lists/{listId}/export")
    fun exportList(
        request: HttpServletRequest,
        @PathVariable listId: Long,
    ): ListExportResponse {
        val user = userService.requireUser(request)
        requireListAccess(listId, user.id!!)
        val items =
            jdbcTemplate.queryForList(
                """
                SELECT book_id, notes, position
                FROM list_items
                WHERE list_id = ?
                ORDER BY position ASC, created_at ASC
                """.trimIndent(),
                listId,
            ).map { row ->
                ExportedListItemResponse(
                    bookId = row["book_id"].asString(),
                    notes = row["notes"]?.toString(),
                    position = row["position"].asInt(),
                )
            }

        val listRow =
            jdbcTemplate.queryForList(
                "SELECT name, is_public FROM lists WHERE id = ?",
                listId,
            ).firstOrNull()
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "List not found")

        return ListExportResponse(
            listId = listId,
            name = listRow["name"].asString(),
            sharePath =
                if (listRow["is_public"].asBoolean()) {
                    "/lists/public/$listId"
                } else {
                    null
                },
            items = items,
        )
    }

    @PostMapping("/reviews/{reviewId}/votes")
    @Transactional
    fun voteReview(
        request: HttpServletRequest,
        @PathVariable reviewId: Long,
        @RequestBody @Valid payload: VoteReviewRequest,
    ): ReviewIntegrityResponse {
        val user = userService.requireUser(request)
        requireReviewExists(reviewId)

        val existingId =
            jdbcTemplate.query(
                "SELECT id FROM review_votes WHERE review_id = ? AND user_id = ?",
                { rs, _ -> rs.getLong("id") },
                reviewId,
                user.id!!,
            ).firstOrNull()
        if (existingId == null) {
            jdbcTemplate.update(
                """
                INSERT INTO review_votes (review_id, user_id, is_helpful, created_at)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                reviewId,
                user.id!!,
                payload.isHelpful,
                Timestamp.from(Instant.now()),
            )
        } else {
            jdbcTemplate.update(
                "UPDATE review_votes SET is_helpful = ? WHERE id = ?",
                payload.isHelpful,
                existingId,
            )
        }

        return reviewIntegrity(reviewId)
    }

    @PostMapping("/reviews/{reviewId}/reports")
    @Transactional
    fun reportReview(
        request: HttpServletRequest,
        @PathVariable reviewId: Long,
        @RequestBody @Valid payload: ReportReviewRequest,
    ): ReviewIntegrityResponse {
        val user = userService.requireUser(request)
        requireReviewExists(reviewId)
        val recentReports =
            count(
                """
                SELECT COUNT(*) FROM review_reports
                WHERE user_id = ? AND created_at >= ?
                """.trimIndent(),
                user.id!!,
                Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)),
            )
        if (recentReports >= 10) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Report rate limit exceeded")
        }

        jdbcTemplate.update(
            """
            INSERT INTO review_reports (review_id, user_id, reason, created_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            reviewId,
            user.id!!,
            payload.reason.trim(),
            Timestamp.from(Instant.now()),
        )

        return reviewIntegrity(reviewId)
    }

    @GetMapping("/reviews/eligibility")
    fun reviewEligibility(
        request: HttpServletRequest,
        @RequestParam bookId: String,
    ): ReviewEligibilityResponse {
        val user = userService.requireUser(request)
        val verified =
            count(
                """
                SELECT COUNT(*) FROM book_progress
                WHERE user_id = ? AND book_id = ?
                """.trimIndent(),
                user.id!!,
                bookId.trim(),
            ) > 0
        return ReviewEligibilityResponse(
            bookId = bookId.trim(),
            verified = verified,
            reason =
                if (verified) {
                    "User has opened or tracked reading progress for this book"
                } else {
                    "Open the book or track progress before leaving a verified review"
                },
        )
    }

    @GetMapping("/notifications/preferences")
    fun notificationPreferences(request: HttpServletRequest): NotificationPreferencesResponse {
        val user = userService.requireUser(request)
        val row =
            jdbcTemplate.queryForList(
                """
                SELECT goal_reminders, book_club_replies, friend_activity, quiet_hours, updated_at
                FROM notification_preferences
                WHERE user_id = ?
                """.trimIndent(),
                user.id!!,
            ).firstOrNull()

        return NotificationPreferencesResponse(
            goalReminders = row?.get("goal_reminders")?.asBoolean() ?: true,
            bookClubReplies = row?.get("book_club_replies")?.asBoolean() ?: true,
            friendActivity = row?.get("friend_activity")?.asBoolean() ?: true,
            quietHours = row?.get("quiet_hours")?.toString(),
            updatedAt = row?.get("updated_at")?.asInstantString() ?: Instant.now().toString(),
        )
    }

    @PutMapping("/notifications/preferences")
    @Transactional
    fun upsertNotificationPreferences(
        request: HttpServletRequest,
        @RequestBody payload: NotificationPreferencesRequest,
    ): NotificationPreferencesResponse {
        val user = userService.requireUser(request)
        val userId = user.id!!
        val now = Timestamp.from(Instant.now())
        val existingId =
            jdbcTemplate.query(
                "SELECT id FROM notification_preferences WHERE user_id = ?",
                { rs, _ -> rs.getLong("id") },
                userId,
            ).firstOrNull()
        if (existingId == null) {
            jdbcTemplate.update(
                """
                INSERT INTO notification_preferences (
                    user_id, goal_reminders, book_club_replies, friend_activity, quiet_hours, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                userId,
                payload.goalReminders,
                payload.bookClubReplies,
                payload.friendActivity,
                payload.quietHours?.trim(),
                now,
            )
        } else {
            jdbcTemplate.update(
                """
                UPDATE notification_preferences
                SET goal_reminders = ?, book_club_replies = ?, friend_activity = ?, quiet_hours = ?, updated_at = ?
                WHERE id = ?
                """.trimIndent(),
                payload.goalReminders,
                payload.bookClubReplies,
                payload.friendActivity,
                payload.quietHours?.trim(),
                now,
                existingId,
            )
        }
        return NotificationPreferencesResponse(
            goalReminders = payload.goalReminders,
            bookClubReplies = payload.bookClubReplies,
            friendActivity = payload.friendActivity,
            quietHours = payload.quietHours?.trim(),
            updatedAt = now.toInstant().toString(),
        )
    }

    @GetMapping("/library/search")
    fun searchOwnLibrary(
        request: HttpServletRequest,
        @RequestParam q: String,
    ): LibrarySearchResponse {
        val user = userService.requireUser(request)
        val normalized = q.trim().lowercase()
        if (normalized.isBlank()) {
            return LibrarySearchResponse(query = q, localMatches = emptyList(), socialMatches = emptyList())
        }

        val matcher = "%$normalized%"
        val localMatches =
            jdbcTemplate.queryForList(
                """
                SELECT DISTINCT source, item_id, label, secondary_label
                FROM (
                    SELECT 'favorite' AS source, f.id AS item_id, f.book_id AS label, CAST(NULL AS VARCHAR(240)) AS secondary_label
                    FROM favorites f
                    WHERE f.user_id = ?
                    UNION ALL
                    SELECT 'annotation' AS source, a.id AS item_id, COALESCE(a.content, a.annotation_type) AS label, a.book_id AS secondary_label
                    FROM annotations a
                    WHERE a.user_id = ?
                    UNION ALL
                    SELECT 'list' AS source, l.id AS item_id, l.name AS label, l.description AS secondary_label
                    FROM lists l
                    WHERE l.user_id = ?
                    UNION ALL
                    SELECT 'custom_book' AS source, cb.id AS item_id, cb.title AS label, cb.authors AS secondary_label
                    FROM custom_books cb
                    WHERE cb.user_id = ?
                ) library_index
                WHERE lower(COALESCE(label, '')) LIKE ? OR lower(COALESCE(secondary_label, '')) LIKE ?
                ORDER BY source ASC, label ASC
                LIMIT 20
                """.trimIndent(),
                user.id!!,
                user.id!!,
                user.id!!,
                user.id!!,
                matcher,
                matcher,
            ).map { row ->
                LibraryMatchResponse(
                    source = row["source"].asString(),
                    itemId = row["item_id"].asLong(),
                    label = row["label"]?.toString().orEmpty(),
                    secondaryLabel = row["secondary_label"]?.toString(),
                )
            }

        val socialMatches =
            jdbcTemplate.queryForList(
                """
                SELECT l.id AS item_id, l.name AS label, COALESCE(u.display_name, u.email, CONCAT('Reader ', u.id)) AS secondary_label
                FROM lists l
                INNER JOIN users u ON u.id = l.user_id
                WHERE l.is_public = TRUE AND lower(l.name) LIKE ?
                ORDER BY l.updated_at DESC
                LIMIT 10
                """.trimIndent(),
                matcher,
            ).map { row ->
                LibraryMatchResponse(
                    source = "public_list",
                    itemId = row["item_id"].asLong(),
                    label = row["label"].asString(),
                    secondaryLabel = row["secondary_label"]?.toString(),
                )
            }

        return LibrarySearchResponse(
            query = q,
            localMatches = localMatches,
            socialMatches = socialMatches,
        )
    }

    @PostMapping("/library/custom-books")
    @Transactional
    fun addCustomBook(
        request: HttpServletRequest,
        @RequestBody @Valid payload: CreateCustomBookRequest,
    ): CustomBookResponse {
        val user = userService.requireUser(request)
        val userId = user.id!!
        val now = Instant.now()
        val duplicateId =
            jdbcTemplate.query(
                """
                SELECT id FROM custom_books
                WHERE user_id = ? AND (
                    (isbn IS NOT NULL AND isbn = ?)
                    OR lower(title) = ?
                )
                """.trimIndent(),
                { rs, _ -> rs.getLong("id") },
                userId,
                payload.isbn?.trim(),
                payload.title.trim().lowercase(),
            ).firstOrNull()

        if (duplicateId == null) {
            jdbcTemplate.update(
                """
                INSERT INTO custom_books (
                    user_id, source_type, title, authors, isbn, description, external_ref, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                userId,
                payload.sourceType.trim().uppercase(),
                payload.title.trim(),
                payload.authors?.trim(),
                payload.isbn?.trim(),
                payload.description?.trim(),
                payload.externalRef?.trim(),
                Timestamp.from(now),
                Timestamp.from(now),
            )
        }

        val bookId = duplicateId ?: count("SELECT MAX(id) FROM custom_books WHERE user_id = ?", userId).toLong()
        return CustomBookResponse(
            id = bookId,
            deduplicated = duplicateId != null,
            sourceType = payload.sourceType.trim().uppercase(),
            title = payload.title.trim(),
            authors = payload.authors?.trim(),
            isbn = payload.isbn?.trim(),
            description = payload.description?.trim(),
            externalRef = payload.externalRef?.trim(),
            updatedAt = now.toString(),
        )
    }

    @PostMapping("/library/imports")
    @Transactional
    fun createImportJob(
        request: HttpServletRequest,
        @RequestBody @Valid payload: CreateImportJobRequest,
    ): ImportJobResponse {
        val user = userService.requireUser(request)
        val userId = user.id!!
        val importedCount = payload.items.size
        var deduplicatedCount = 0
        payload.items.forEach { item ->
            val result =
                addCustomBook(
                    request = request,
                    payload =
                        CreateCustomBookRequest(
                            sourceType = payload.sourceType,
                            title = item.title,
                            authors = item.authors,
                            isbn = item.isbn,
                            description = item.description,
                            externalRef = item.externalRef,
                        ),
                )
            if (result.deduplicated) {
                deduplicatedCount += 1
            }
        }

        val now = Instant.now()
        jdbcTemplate.update(
            """
            INSERT INTO import_jobs (user_id, source_type, status, imported_count, deduplicated_count, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            userId,
            payload.sourceType.trim().uppercase(),
            "COMPLETED",
            importedCount,
            deduplicatedCount,
            Timestamp.from(now),
            Timestamp.from(now),
        )

        return ImportJobResponse(
            id = count("SELECT MAX(id) FROM import_jobs WHERE user_id = ?", userId).toLong(),
            sourceType = payload.sourceType.trim().uppercase(),
            status = "COMPLETED",
            importedCount = importedCount,
            deduplicatedCount = deduplicatedCount,
            updatedAt = now.toString(),
        )
    }

    private fun reviewIntegrity(reviewId: Long): ReviewIntegrityResponse {
        val helpfulVotes =
            count(
                "SELECT COUNT(*) FROM review_votes WHERE review_id = ? AND is_helpful = TRUE",
                reviewId,
            )
        val reports = count("SELECT COUNT(*) FROM review_reports WHERE review_id = ?", reviewId)
        return ReviewIntegrityResponse(
            reviewId = reviewId,
            helpfulVotes = helpfulVotes,
            reports = reports,
            autoFlagged = reports >= 3,
        )
    }

    private fun calculateStreakDays(readingDays: List<LocalDate>): Int {
        if (readingDays.isEmpty()) {
            return 0
        }

        var streak = 0
        var expectedDay = LocalDate.now(ZoneOffset.UTC)

        for (day in readingDays) {
            if (day == expectedDay) {
                streak += 1
                expectedDay = expectedDay.minusDays(1)
            } else if (day == expectedDay.minusDays(1) && streak == 0) {
                streak = 1
                expectedDay = day.minusDays(1)
            } else if (day.isBefore(expectedDay)) {
                break
            }
        }

        return streak
    }

    private fun count(
        sql: String,
        vararg args: Any?,
    ): Int {
        return jdbcTemplate.query(sql, { rs, _ -> rs.getInt(1) }, *args).firstOrNull() ?: 0
    }

    private fun requireUserExists(userId: Long) {
        val exists = count("SELECT COUNT(*) FROM users WHERE id = ?", userId) > 0
        if (!exists) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
    }

    private fun lookupUserLabel(userId: Long): String {
        return jdbcTemplate.queryForList(
            "SELECT COALESCE(display_name, email, CONCAT('Reader ', id)) AS label FROM users WHERE id = ?",
            userId,
        ).firstOrNull()?.get("label")?.toString()
            ?: "Reader $userId"
    }

    private fun requireClubMembership(
        clubId: Long,
        userId: Long,
    ) {
        val count =
            count(
                """
                SELECT COUNT(*) FROM book_club_memberships
                WHERE club_id = ? AND user_id = ?
                """.trimIndent(),
                clubId,
                userId,
            )
        if (count == 0) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this club")
        }
    }

    private fun requireListOwner(
        listId: Long,
        userId: Long,
    ) {
        val count = count("SELECT COUNT(*) FROM lists WHERE id = ? AND user_id = ?", listId, userId)
        if (count == 0) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "List is not owned by authenticated user")
        }
    }

    private fun requireListAccess(
        listId: Long,
        userId: Long,
    ) {
        val ownerCount = count("SELECT COUNT(*) FROM lists WHERE id = ? AND user_id = ?", listId, userId)
        if (ownerCount > 0) {
            return
        }
        val collaboratorCount =
            count(
                "SELECT COUNT(*) FROM list_collaborators WHERE list_id = ? AND user_id = ?",
                listId,
                userId,
            )
        if (collaboratorCount == 0) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this list")
        }
    }

    private fun requireReviewExists(reviewId: Long) {
        val exists = count("SELECT COUNT(*) FROM reviews WHERE id = ?", reviewId) > 0
        if (!exists) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found")
        }
    }

    private fun insertActivityEvent(
        userId: Long,
        eventType: String,
        bookId: String? = null,
        detail: String? = null,
        visibility: String,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO activity_events (user_id, event_type, book_id, detail, visibility, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            userId,
            eventType,
            bookId,
            detail,
            visibility,
            Timestamp.from(Instant.now()),
        )
    }
}

data class UpsertReadingGoalRequest(
    @field:NotBlank(message = "cadence is required")
    val cadence: String,
    @field:Min(0)
    @field:Max(10000)
    val targetMinutes: Int = 0,
    @field:Min(0)
    @field:Max(100000)
    val targetPages: Int = 0,
    val reminderEnabled: Boolean = false,
    val quietHours: String? = null,
)

data class ReadingGoalResponse(
    val cadence: String,
    val targetMinutes: Int,
    val targetPages: Int,
    val reminderEnabled: Boolean,
    val quietHours: String?,
    val updatedAt: String,
)

data class UpsertBookProgressRequest(
    @field:NotBlank(message = "bookId is required")
    val bookId: String,
    @field:Min(0)
    @field:Max(100)
    val percentComplete: Int = 0,
    @field:Min(0)
    @field:Max(10000)
    val chaptersCompleted: Int = 0,
    @field:Min(0)
    @field:Max(100000)
    val minutesRead: Int = 0,
    @field:NotBlank(message = "currentShelf is required")
    val currentShelf: String = "TO_READ",
) {
    fun progressLabel(): String {
        return "$percentComplete% complete • $minutesRead minutes • $chaptersCompleted chapters"
    }
}

data class BookProgressResponse(
    val bookId: String,
    val percentComplete: Int,
    val chaptersCompleted: Int,
    val minutesRead: Int,
    val currentShelf: String,
    val lastReadAt: String?,
    val updatedAt: String,
)

data class CreateAnnotationRequest(
    @field:NotBlank(message = "bookId is required")
    val bookId: String,
    @field:NotBlank(message = "annotationType is required")
    val annotationType: String,
    val content: String? = null,
    val chapterLabel: String? = null,
    val pageRef: String? = null,
    val visibility: String = "PRIVATE",
)

data class AnnotationResponse(
    val id: Long,
    val bookId: String,
    val annotationType: String,
    val content: String?,
    val chapterLabel: String?,
    val pageRef: String?,
    val visibility: String,
    val updatedAt: String,
)

data class ReadingSummaryResponse(
    val goals: List<ReadingGoalResponse>,
    val streakDays: Int,
    val totalMinutesRead: Int,
    val currentlyReadingCount: Int,
    val highlightsCount: Int,
    val notesCount: Int,
    val bookmarksCount: Int,
    val activeProgress: List<BookProgressResponse>,
)

data class RecommendationResponse(
    val bookId: String,
    val score: Int,
    val reason: String,
    val explanation: String,
)

data class CreateFriendRequest(
    val friendUserId: Long,
    val visibility: String = "FRIENDS",
)

data class SocialConnectionResponse(
    val id: Long,
    val friendUserId: Long,
    val friendLabel: String,
    val status: String,
    val visibility: String,
    val createdAt: String,
)

data class PublishActivityRequest(
    @field:NotBlank(message = "eventType is required")
    val eventType: String,
    val bookId: String? = null,
    val detail: String? = null,
    val visibility: String = "FRIENDS",
)

data class ActivityFeedResponse(
    val id: Long,
    val actorUserId: Long,
    val actorLabel: String,
    val eventType: String,
    val bookId: String?,
    val detail: String?,
    val visibility: String,
    val createdAt: String,
)

data class CreateClubRequest(
    @field:NotBlank(message = "name is required")
    val name: String,
    val description: String? = null,
    val moderationLevel: String = "OWNER_ONLY",
)

data class JoinClubRequest(
    @field:NotBlank(message = "inviteCode is required")
    val inviteCode: String,
)

data class BookClubResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val inviteCode: String,
    val moderationLevel: String,
    val updatedAt: String,
)

data class CreateDiscussionThreadRequest(
    @field:NotBlank(message = "title is required")
    val title: String,
    val bookId: String? = null,
    val chapterRef: String? = null,
)

data class DiscussionThreadResponse(
    val id: Long,
    val clubId: Long,
    val bookId: String?,
    val chapterRef: String?,
    val title: String,
    val createdByUserId: Long,
    val authorLabel: String,
    val createdAt: String,
)

data class CreateDiscussionPostRequest(
    @field:NotBlank(message = "message is required")
    val message: String,
)

data class DiscussionPostResponse(
    val id: Long,
    val threadId: Long,
    val userId: Long,
    val authorLabel: String,
    val message: String,
    val createdAt: String,
)

data class AddListCollaboratorRequest(
    val userId: Long,
    val permission: String = "EDITOR",
)

data class ListCollaboratorResponse(
    val id: Long,
    val userId: Long,
    val userLabel: String,
    val permission: String,
    val createdAt: String,
)

data class ExportedListItemResponse(
    val bookId: String,
    val notes: String?,
    val position: Int,
)

data class ListExportResponse(
    val listId: Long,
    val name: String,
    val sharePath: String?,
    val items: List<ExportedListItemResponse>,
)

data class VoteReviewRequest(
    val isHelpful: Boolean = true,
)

data class ReportReviewRequest(
    @field:NotBlank(message = "reason is required")
    val reason: String,
)

data class ReviewIntegrityResponse(
    val reviewId: Long,
    val helpfulVotes: Int,
    val reports: Int,
    val autoFlagged: Boolean,
)

data class ReviewEligibilityResponse(
    val bookId: String,
    val verified: Boolean,
    val reason: String,
)

data class NotificationPreferencesRequest(
    val goalReminders: Boolean = true,
    val bookClubReplies: Boolean = true,
    val friendActivity: Boolean = true,
    val quietHours: String? = null,
)

data class NotificationPreferencesResponse(
    val goalReminders: Boolean,
    val bookClubReplies: Boolean,
    val friendActivity: Boolean,
    val quietHours: String?,
    val updatedAt: String,
)

data class LibraryMatchResponse(
    val source: String,
    val itemId: Long,
    val label: String,
    val secondaryLabel: String?,
)

data class LibrarySearchResponse(
    val query: String,
    val localMatches: List<LibraryMatchResponse>,
    val socialMatches: List<LibraryMatchResponse>,
)

data class CreateCustomBookRequest(
    val sourceType: String = "MANUAL",
    @field:NotBlank(message = "title is required")
    val title: String,
    val authors: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val externalRef: String? = null,
)

data class CustomBookResponse(
    val id: Long,
    val deduplicated: Boolean,
    val sourceType: String,
    val title: String,
    val authors: String?,
    val isbn: String?,
    val description: String?,
    val externalRef: String?,
    val updatedAt: String,
)

data class CreateImportJobRequest(
    val sourceType: String = "GOODREADS_CSV",
    val items: List<CreateCustomBookRequest>,
)

data class ImportJobResponse(
    val id: Long,
    val sourceType: String,
    val status: String,
    val importedCount: Int,
    val deduplicatedCount: Int,
    val updatedAt: String,
)

private fun Any?.asString(): String = this?.toString().orEmpty()

private fun Any?.asInt(): Int {
    return when (this) {
        is Number -> this.toInt()
        is Boolean -> if (this) 1 else 0
        else -> this?.toString()?.toIntOrNull() ?: 0
    }
}

private fun Any?.asLong(): Long {
    return when (this) {
        is Number -> this.toLong()
        else -> this?.toString()?.toLongOrNull() ?: 0L
    }
}

private fun Any?.asBoolean(): Boolean {
    return when (this) {
        is Boolean -> this
        is Number -> this.toInt() != 0
        else -> this?.toString()?.equals("true", ignoreCase = true) == true
    }
}

private fun Any?.asInstantString(): String {
    return when (this) {
        is Timestamp -> this.toInstant().toString()
        is java.util.Date -> this.toInstant().toString()
        is Instant -> this.toString()
        else -> this?.toString() ?: Instant.now().toString()
    }
}

private fun Any?.toLocalDateOrNull(): LocalDate? {
    return when (this) {
        is java.sql.Date -> this.toLocalDate()
        is Timestamp -> this.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
        is LocalDate -> this
        else -> this?.toString()?.let(LocalDate::parse)
    }
}
