package com.example.chapterly.backend.controller

import com.example.chapterly.backend.model.ReviewEntity
import com.example.chapterly.backend.repository.ReviewRepository
import com.example.chapterly.backend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/backend/reviews")
class ReviewsController(
    private val userService: UserService,
    private val reviewRepository: ReviewRepository,
    @Suppress("unused")
    private val jdbcTemplate: JdbcTemplate,
) {
    @GetMapping
    fun byBook(
        @RequestParam bookId: String,
    ): List<ReviewResponse> {
        return reviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId).map { row ->
            ReviewResponse(
                id = row.id ?: 0,
                userId = row.userId,
                bookId = row.bookId,
                rating = row.rating,
                comment = row.comment,
                updatedAt = row.updatedAt.toString(),
            )
        }
    }

    @GetMapping("/mine")
    fun mine(request: HttpServletRequest): List<ReviewResponse> {
        val user = userService.requireUser(request)
        return reviewRepository.findAllByUserIdOrderByUpdatedAtDesc(user.id!!).map { row ->
            ReviewResponse(
                id = row.id ?: 0,
                userId = row.userId,
                bookId = row.bookId,
                rating = row.rating,
                comment = row.comment,
                updatedAt = row.updatedAt.toString(),
            )
        }
    }

    @PostMapping
    fun upsert(
        request: HttpServletRequest,
        @RequestBody @Valid payload: UpsertReviewRequest,
    ): ReviewResponse {
        val user = userService.requireUser(request)
        val existing = reviewRepository.findByUserIdAndBookId(user.id!!, payload.bookId.trim())
        val now = Instant.now()

        val saved =
            if (existing != null) {
                existing.rating = payload.rating
                existing.comment = payload.comment?.trim()
                existing.updatedAt = now
                reviewRepository.save(existing)
            } else {
                reviewRepository.save(
                    ReviewEntity(
                        userId = user.id!!,
                        bookId = payload.bookId.trim(),
                        rating = payload.rating,
                        comment = payload.comment?.trim(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }

        return ReviewResponse(
            id = saved.id ?: 0,
            userId = saved.userId,
            bookId = saved.bookId,
            rating = saved.rating,
            comment = saved.comment,
            updatedAt = saved.updatedAt.toString(),
        )
    }
}

data class UpsertReviewRequest(
    @field:NotBlank(message = "bookId is required")
    val bookId: String,
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    val comment: String? = null,
)

data class ReviewResponse(
    val id: Long,
    val userId: Long,
    val bookId: String,
    val rating: Int,
    val comment: String?,
    val updatedAt: String,
)
