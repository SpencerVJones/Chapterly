package com.example.chapterly.backend.controller

import com.example.chapterly.backend.model.FavoriteEntity
import com.example.chapterly.backend.repository.FavoriteRepository
import com.example.chapterly.backend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@Validated
@RestController
@RequestMapping("/api/backend/favorites")
class FavoritesController(
    private val userService: UserService,
    private val favoriteRepository: FavoriteRepository,
) {
    @GetMapping
    fun list(request: HttpServletRequest): List<FavoriteResponse> {
        val user = userService.requireUser(request)
        return favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(user.id!!).map { favorite ->
            FavoriteResponse(
                id = favorite.id ?: 0,
                bookId = favorite.bookId,
                createdAt = favorite.createdAt.toString(),
            )
        }
    }

    @PostMapping
    @Transactional
    fun create(
        request: HttpServletRequest,
        @RequestBody @Valid payload: FavoriteRequest,
    ): FavoriteResponse {
        val user = userService.requireUser(request)
        val existing = favoriteRepository.findByUserIdAndBookId(user.id!!, payload.bookId)

        val saved =
            existing
                ?: favoriteRepository.save(
                    FavoriteEntity(
                        userId = user.id!!,
                        bookId = payload.bookId.trim(),
                        createdAt = Instant.now(),
                    ),
                )

        return FavoriteResponse(
            id = saved.id ?: 0,
            bookId = saved.bookId,
            createdAt = saved.createdAt.toString(),
        )
    }

    @DeleteMapping("/{bookId}")
    @Transactional
    fun delete(
        request: HttpServletRequest,
        @PathVariable bookId: String,
    ): Map<String, String> {
        val user = userService.requireUser(request)
        favoriteRepository.deleteByUserIdAndBookId(user.id!!, bookId)
        return mapOf("status" to "deleted")
    }
}

data class FavoriteRequest(
    @field:NotBlank(message = "bookId is required")
    val bookId: String,
)

data class FavoriteResponse(
    val id: Long,
    val bookId: String,
    val createdAt: String,
)
