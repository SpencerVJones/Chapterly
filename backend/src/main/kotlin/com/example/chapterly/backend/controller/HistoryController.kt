package com.example.chapterly.backend.controller

import com.example.chapterly.backend.model.HistoryEntity
import com.example.chapterly.backend.repository.HistoryRepository
import com.example.chapterly.backend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@Validated
@RestController
@RequestMapping("/api/backend/history")
class HistoryController(
    private val userService: UserService,
    private val historyRepository: HistoryRepository,
) {
    @GetMapping
    fun list(request: HttpServletRequest): List<HistoryResponse> {
        val user = userService.requireUser(request)
        return historyRepository.findAllByUserIdOrderByCreatedAtDesc(user.id!!).map { row ->
            HistoryResponse(
                id = row.id ?: 0,
                query = row.query,
                createdAt = row.createdAt.toString(),
            )
        }
    }

    @PostMapping
    fun create(
        request: HttpServletRequest,
        @RequestBody @Valid payload: HistoryRequest,
    ): HistoryResponse {
        val user = userService.requireUser(request)
        val saved =
            historyRepository.save(
                HistoryEntity(
                    userId = user.id!!,
                    query = payload.query.trim(),
                    createdAt = Instant.now(),
                ),
            )

        return HistoryResponse(
            id = saved.id ?: 0,
            query = saved.query,
            createdAt = saved.createdAt.toString(),
        )
    }

    @DeleteMapping
    @Transactional
    fun clear(request: HttpServletRequest): Map<String, String> {
        val user = userService.requireUser(request)
        historyRepository.deleteByUserId(user.id!!)
        return mapOf("status" to "cleared")
    }
}

data class HistoryRequest(
    @field:NotBlank(message = "query is required")
    val query: String,
)

data class HistoryResponse(
    val id: Long,
    val query: String,
    val createdAt: String,
)
