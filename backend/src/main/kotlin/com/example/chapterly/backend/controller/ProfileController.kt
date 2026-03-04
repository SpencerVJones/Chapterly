package com.example.chapterly.backend.controller

import com.example.chapterly.backend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/backend")
class ProfileController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun me(request: HttpServletRequest): Map<String, Any?> {
        val user = userService.requireUser(request)
        return mapOf(
            "id" to user.id,
            "firebaseUid" to user.firebaseUid,
            "email" to user.email,
            "displayName" to user.displayName,
        )
    }
}
