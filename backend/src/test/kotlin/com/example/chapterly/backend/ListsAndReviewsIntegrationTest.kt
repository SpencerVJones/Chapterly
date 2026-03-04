package com.example.chapterly.backend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListsAndReviewsIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun listsItemsAndReviewsEndpointsPersistData() {
        val debugUid = "integration-user-2"

        val createListResponse =
            mockMvc.perform(
                post("/api/backend/lists")
                    .header("X-Debug-Uid", debugUid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Android Roadmap","description":"Books to read","isPublic":false}"""),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name", equalTo("Android Roadmap")))
                .andReturn()

        val listId = extractId(createListResponse.response.contentAsString)

        mockMvc.perform(
            post("/api/backend/lists/$listId/items")
                .header("X-Debug-Uid", debugUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookId":"book-xyz","notes":"Priority read","position":1}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bookId", equalTo("book-xyz")))

        mockMvc.perform(
            get("/api/backend/lists/$listId/items")
                .header("X-Debug-Uid", debugUid),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].bookId", equalTo("book-xyz")))

        mockMvc.perform(
            post("/api/backend/reviews")
                .header("X-Debug-Uid", debugUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookId":"book-xyz","rating":5,"comment":"Excellent"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rating", equalTo(5)))

        mockMvc.perform(
            get("/api/backend/reviews")
                .param("bookId", "book-xyz"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].bookId", equalTo("book-xyz")))
    }

    private fun extractId(json: String): Long {
        val node: JsonNode = objectMapper.readTree(json)
        return node.get("id").asLong()
    }
}
