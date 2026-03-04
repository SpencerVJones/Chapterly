package com.example.jonesspencer_ce07.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.jonesspencer_ce07.data.local.BookEntity
import com.example.jonesspencer_ce07.data.local.BookRow
import com.example.jonesspencer_ce07.data.local.BooksDatabase
import com.example.jonesspencer_ce07.data.local.RemoteKeysEntity
import com.example.jonesspencer_ce07.data.repository.AUTHOR_SEPARATOR
import com.example.jonesspencer_ce07.data.repository.BOOKS_PAGE_SIZE
import com.example.jonesspencer_ce07.data.repository.DEFAULT_DESCRIPTION
import com.example.jonesspencer_ce07.data.repository.DEFAULT_PUBLISHED_DATE
import com.example.jonesspencer_ce07.data.repository.DEFAULT_TITLE
import com.example.jonesspencer_ce07.data.repository.buildImageUrl
import com.example.jonesspencer_ce07.data.repository.buildPreviewUrl
import com.example.jonesspencer_ce07.data.repository.normalizeQueryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class BooksRemoteMediator(
    private val query: String,
    private val api: GoogleBooksApi,
    private val database: BooksDatabase
) : RemoteMediator<Int, BookRow>() {

    private val queryKey = normalizeQueryKey(query)

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, BookRow>
    ): MediatorResult = withContext(Dispatchers.IO) {
        val booksDao = database.booksDao()
        val startIndex = when (loadType) {
            LoadType.REFRESH -> 0
            LoadType.PREPEND -> return@withContext MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = booksDao.remoteKey(queryKey)
                if (remoteKey?.endOfPaginationReached == true) {
                    return@withContext MediatorResult.Success(endOfPaginationReached = true)
                }
                remoteKey?.nextIndex ?: return@withContext MediatorResult.Success(
                    endOfPaginationReached = true
                )
            }
        }

        try {
            val response = api.searchVolumes(
                query = query,
                startIndex = startIndex,
                maxResults = state.config.pageSize.coerceAtMost(BOOKS_PAGE_SIZE)
            )
            val items = response.items
            val endOfPaginationReached = items.isEmpty()

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    booksDao.clearBooksForQuery(queryKey)
                    booksDao.clearRemoteKey(queryKey)
                }

                val now = System.currentTimeMillis()
                val entities = items.mapIndexed { index, volume ->
                    val info = volume.volumeInfo
                    BookEntity(
                        queryKey = queryKey,
                        bookId = volume.id,
                        title = info?.title?.ifBlank { DEFAULT_TITLE } ?: DEFAULT_TITLE,
                        authors = info?.authors
                            ?.takeIf { it.isNotEmpty() }
                            ?.joinToString(AUTHOR_SEPARATOR)
                            ?: "",
                        description = info?.description?.ifBlank { DEFAULT_DESCRIPTION },
                        thumbnailUrl = buildImageUrl(info?.imageLinks?.thumbnail, info?.imageLinks?.smallThumbnail),
                        previewLink = buildPreviewUrl(info?.previewLink),
                        publishedDate = info?.publishedDate?.ifBlank { DEFAULT_PUBLISHED_DATE },
                        listPosition = startIndex + index,
                        cachedAt = now
                    )
                }

                booksDao.upsertBooks(entities)
                booksDao.upsertRemoteKey(
                    remoteKey = RemoteKeysEntity(
                        queryKey = queryKey,
                        nextIndex = if (endOfPaginationReached) null else startIndex + items.size,
                        endOfPaginationReached = endOfPaginationReached
                    )
                )
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (ioException: IOException) {
            MediatorResult.Error(ioException)
        } catch (httpException: HttpException) {
            MediatorResult.Error(httpException)
        }
    }
}
