package com.example.chapterly.backend.controller

import com.example.chapterly.backend.model.ListItemEntity
import com.example.chapterly.backend.model.ReadingListEntity
import com.example.chapterly.backend.repository.ListItemRepository
import com.example.chapterly.backend.repository.ReadingListRepository
import com.example.chapterly.backend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Validated
@RestController
@RequestMapping("/api/backend/lists")
class ListsController(
    private val userService: UserService,
    private val listRepository: ReadingListRepository,
    private val listItemRepository: ListItemRepository,
) {
    @GetMapping
    fun list(request: HttpServletRequest): List<ReadingListResponse> {
        val user = userService.requireUser(request)
        return listRepository.findAllByUserIdOrderByUpdatedAtDesc(user.id!!).map { row ->
            ReadingListResponse(
                id = row.id ?: 0,
                name = row.name,
                description = row.description,
                isPublic = row.isPublic,
                updatedAt = row.updatedAt.toString(),
            )
        }
    }

    @PostMapping
    fun create(
        request: HttpServletRequest,
        @RequestBody @Valid payload: CreateListRequest,
    ): ReadingListResponse {
        val user = userService.requireUser(request)
        val now = Instant.now()
        val saved =
            listRepository.save(
                ReadingListEntity(
                    userId = user.id!!,
                    name = payload.name.trim(),
                    description = payload.description?.trim(),
                    isPublic = payload.isPublic,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

        return ReadingListResponse(
            id = saved.id ?: 0,
            name = saved.name,
            description = saved.description,
            isPublic = saved.isPublic,
            updatedAt = saved.updatedAt.toString(),
        )
    }

    @GetMapping("/{listId}/items")
    fun listItems(
        request: HttpServletRequest,
        @PathVariable listId: Long,
    ): List<ListItemResponse> {
        val user = userService.requireUser(request)
        val list = requireOwnedList(listId, user.id!!)

        return listItemRepository.findAllByListIdOrderByPositionAscCreatedAtAsc(list.id!!).map { row ->
            ListItemResponse(
                id = row.id ?: 0,
                bookId = row.bookId,
                notes = row.notes,
                position = row.position,
            )
        }
    }

    @PostMapping("/{listId}/items")
    @Transactional
    fun addItem(
        request: HttpServletRequest,
        @PathVariable listId: Long,
        @RequestBody @Valid payload: CreateListItemRequest,
    ): ListItemResponse {
        val user = userService.requireUser(request)
        val list = requireOwnedList(listId, user.id!!)

        val existing = listItemRepository.findByListIdAndBookId(list.id!!, payload.bookId.trim())
        val saved =
            if (existing != null) {
                existing.notes = payload.notes?.trim()
                existing.position = payload.position
                listItemRepository.save(existing)
            } else {
                listItemRepository.save(
                    ListItemEntity(
                        listId = list.id!!,
                        bookId = payload.bookId.trim(),
                        notes = payload.notes?.trim(),
                        position = payload.position,
                        createdAt = Instant.now(),
                    ),
                )
            }

        list.updatedAt = Instant.now()
        listRepository.save(list)

        return ListItemResponse(
            id = saved.id ?: 0,
            bookId = saved.bookId,
            notes = saved.notes,
            position = saved.position,
        )
    }

    private fun requireOwnedList(
        listId: Long,
        userId: Long,
    ): ReadingListEntity {
        val list =
            listRepository.findById(listId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "List not found")
            }

        if (list.userId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "List is not owned by authenticated user")
        }

        return list
    }
}

data class CreateListRequest(
    @field:NotBlank(message = "name is required")
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = false,
)

data class CreateListItemRequest(
    @field:NotBlank(message = "bookId is required")
    val bookId: String,
    val notes: String? = null,
    @field:Min(0)
    @field:Max(10000)
    val position: Int = 0,
)

data class ReadingListResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val updatedAt: String,
)

data class ListItemResponse(
    val id: Long,
    val bookId: String,
    val notes: String?,
    val position: Int,
)
