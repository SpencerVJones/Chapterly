package com.example.chapterly.backend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun recommendationsUseReadingSignals() {
        val debugUid = "integration-product-user-1"

        mockMvc.perform(
            post("/api/backend/favorites")
                .header("X-Debug-Uid", debugUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookId":"book-rec-1"}"""),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/backend/product/reading/progress")
                .header("X-Debug-Uid", debugUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"bookId":"book-rec-1","percentComplete":42,"chaptersCompleted":5,"minutesRead":120,"currentShelf":"CURRENTLY_READING"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.percentComplete", equalTo(42)))

        mockMvc.perform(
            get("/api/backend/product/recommendations")
                .header("X-Debug-Uid", debugUid),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].bookId", equalTo("book-rec-1")))
    }

    @Test
    fun bookClubsJoinAndDiscussionFlow() {
        val ownerUid = "integration-club-owner"
        val memberUid = "integration-club-member"

        val createClubResponse =
            mockMvc.perform(
                post("/api/backend/product/clubs")
                    .header("X-Debug-Uid", ownerUid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Compose Club","description":"Ship the reading product","moderationLevel":"OWNER_ONLY"}"""),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name", equalTo("Compose Club")))
                .andReturn()

        val createClubNode = objectMapper.readTree(createClubResponse.response.contentAsString)
        val clubId = createClubNode.get("id").asLong()
        val inviteCode = createClubNode.get("inviteCode").asText()

        mockMvc.perform(
            post("/api/backend/product/clubs/join")
                .header("X-Debug-Uid", memberUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"$inviteCode"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id", equalTo(clubId.toInt())))

        mockMvc.perform(
            get("/api/backend/product/clubs")
                .header("X-Debug-Uid", memberUid),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].name", equalTo("Compose Club")))

        val threadResponse =
            mockMvc.perform(
                post("/api/backend/product/clubs/$clubId/threads")
                    .header("X-Debug-Uid", memberUid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"Chapter 1 thoughts","bookId":"book-rec-1","chapterRef":"Chapter 1"}"""),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title", equalTo("Chapter 1 thoughts")))
                .andReturn()

        val threadId = extractId(threadResponse.response.contentAsString)

        mockMvc.perform(
            post("/api/backend/product/threads/$threadId/posts")
                .header("X-Debug-Uid", ownerUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":"Pinned for club discussion"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message", equalTo("Pinned for club discussion")))

        mockMvc.perform(
            get("/api/backend/product/clubs/$clubId/threads")
                .header("X-Debug-Uid", ownerUid),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].chapterRef", equalTo("Chapter 1")))
    }

    @Test
    fun importAndSearchFlowsPersistCustomLibraryItems() {
        val debugUid = "integration-import-user"

        mockMvc.perform(
            post("/api/backend/product/library/imports")
                .header("X-Debug-Uid", debugUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sourceType":"GOODREADS_CSV",
                      "items":[
                        {"title":"Deep Work","authors":"Cal Newport","isbn":"9781455586691","description":"Focus better"},
                        {"title":"Deep Work","authors":"Cal Newport","isbn":"9781455586691","description":"Duplicate row"}
                      ]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sourceType", equalTo("GOODREADS_CSV")))
            .andExpect(jsonPath("$.importedCount", equalTo(2)))
            .andExpect(jsonPath("$.deduplicatedCount", equalTo(1)))

        mockMvc.perform(
            get("/api/backend/product/library/search")
                .header("X-Debug-Uid", debugUid)
                .param("q", "deep work"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.query", equalTo("deep work")))
            .andExpect(jsonPath("$.localMatches", hasSize<Any>(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.localMatches[0].source", equalTo("custom_book")))
    }

    @Test
    fun readingGoalsSummaryIncludesOfflineReadingSignals() {
        val debugUid = "integration-reading-user"

        mockMvc.perform(
            put("/api/backend/product/reading/goals")
                .header("X-Debug-Uid", debugUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"cadence":"daily","targetMinutes":30,"targetPages":20,"reminderEnabled":true,"quietHours":"22:00-07:00"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cadence", equalTo("DAILY")))

        mockMvc.perform(
            post("/api/backend/product/reading/annotations")
                .header("X-Debug-Uid", debugUid)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookId":"book-rec-2","annotationType":"highlight","content":"Key insight","visibility":"PRIVATE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.annotationType", equalTo("HIGHLIGHT")))

        mockMvc.perform(
            get("/api/backend/product/reading")
                .header("X-Debug-Uid", debugUid),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.goals", hasSize<Any>(1)))
            .andExpect(jsonPath("$.highlightsCount", equalTo(1)))
    }

    private fun extractId(json: String): Long {
        val node: JsonNode = objectMapper.readTree(json)
        return node.get("id").asLong()
    }
}
