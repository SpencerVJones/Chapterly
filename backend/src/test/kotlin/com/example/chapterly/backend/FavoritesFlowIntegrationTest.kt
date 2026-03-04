package com.example.chapterly.backend

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoritesFlowIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun favoritesCreateListAndDeleteFlow() {
        val debugUid = "integration-user-1"

        mockMvc.perform(
            post("/api/backend/favorites")
                .header("X-Debug-Uid", debugUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookId":"book-123"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bookId", equalTo("book-123")))

        mockMvc.perform(
            get("/api/backend/favorites")
                .header("X-Debug-Uid", debugUid),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].bookId", equalTo("book-123")))

        mockMvc.perform(
            delete("/api/backend/favorites/book-123")
                .header("X-Debug-Uid", debugUid),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            get("/api/backend/favorites")
                .header("X-Debug-Uid", debugUid),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }
}
