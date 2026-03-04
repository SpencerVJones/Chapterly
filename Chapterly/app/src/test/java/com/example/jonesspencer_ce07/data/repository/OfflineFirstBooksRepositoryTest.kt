package com.example.jonesspencer_ce07.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.paging.testing.asSnapshot
import com.example.jonesspencer_ce07.MainDispatcherRule
import com.example.jonesspencer_ce07.data.local.BooksDao
import com.example.jonesspencer_ce07.data.local.BooksDatabase
import com.example.jonesspencer_ce07.data.remote.GoogleBooksApi
import com.example.jonesspencer_ce07.data.remote.ImageLinksDto
import com.example.jonesspencer_ce07.data.remote.VolumeDto
import com.example.jonesspencer_ce07.data.remote.VolumeInfoDto
import com.example.jonesspencer_ce07.data.remote.VolumesResponse
import com.example.jonesspencer_ce07.model.SortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OfflineFirstBooksRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeApi = FakeGoogleBooksApi()

    private val database: BooksDatabase by lazy {
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            BooksDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    private val booksDao: BooksDao by lazy { database.booksDao() }

    private val repository: OfflineFirstBooksRepository by lazy {
        OfflineFirstBooksRepository(
            api = fakeApi,
            database = database,
            booksDao = booksDao
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pagedBooks_fetchesAndCachesFromNetwork() = runTest {
        fakeApi.seedQuery("android", count = 45)

        val snapshot = repository.pagedBooks(
            query = "android",
            sortOption = SortOption.RELEVANCE,
            favoritesOnly = false
        ).asSnapshot {
            scrollTo(index = 25)
        }

        assertEquals("Book 0", snapshot.first().title)
        assertTrue(snapshot.size >= 26)
        assertNotNull(booksDao.observeBook("android-0").first())
    }

    @Test
    fun favoritesFilter_returnsOnlyFavoritedBooks() = runTest {
        fakeApi.seedQuery("android", count = 15)

        val initial = repository.pagedBooks(
            query = "android",
            sortOption = SortOption.RELEVANCE,
            favoritesOnly = false
        ).asSnapshot()
        val bookToFavorite = initial.first()

        repository.toggleFavorite(bookId = bookToFavorite.id, currentlyFavorite = false)

        val favoritesOnly = repository.pagedBooks(
            query = "android",
            sortOption = SortOption.RELEVANCE,
            favoritesOnly = true
        ).asSnapshot()

        assertEquals(listOf(bookToFavorite.id), favoritesOnly.map { it.id })
    }

    @Test
    fun saveSearchQuery_updatesHistory() = runTest {
        repository.saveSearchQuery("compose")
        repository.saveSearchQuery("kotlin")

        val history = repository.observeSearchHistory().first()
        assertEquals(listOf("kotlin", "compose"), history.take(2))
    }
}

private class FakeGoogleBooksApi : GoogleBooksApi {
    private val booksByQuery = mutableMapOf<String, List<VolumeDto>>()
    private val booksById = mutableMapOf<String, VolumeDto>()

    fun seedQuery(query: String, count: Int) {
        val items = List(count) { index ->
            val id = "$query-$index"
            VolumeDto(
                id = id,
                volumeInfo = VolumeInfoDto(
                    title = "Book $index",
                    authors = listOf("Author $index"),
                    description = "Description for book $index",
                    imageLinks = ImageLinksDto(
                        thumbnail = "http://images.example.com/$id.jpg",
                        smallThumbnail = "http://images.example.com/$id-small.jpg"
                    ),
                    previewLink = "https://preview.example.com/$id",
                    publishedDate = "2024-01-01"
                )
            ).also { volume ->
                booksById[id] = volume
            }
        }
        booksByQuery[query] = items
    }

    override suspend fun searchVolumes(
        query: String,
        startIndex: Int,
        maxResults: Int,
        printType: String
    ): VolumesResponse {
        val allItems = booksByQuery[query].orEmpty()
        val endIndex = (startIndex + maxResults).coerceAtMost(allItems.size)
        val page = if (startIndex >= endIndex) emptyList() else allItems.subList(startIndex, endIndex)
        return VolumesResponse(
            totalItems = allItems.size,
            items = page
        )
    }

    override suspend fun getVolume(bookId: String): VolumeDto {
        return booksById.getValue(bookId)
    }
}
