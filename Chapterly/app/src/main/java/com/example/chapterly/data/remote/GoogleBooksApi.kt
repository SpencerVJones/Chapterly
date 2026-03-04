package com.example.chapterly.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchVolumes(
        @Query("q") query: String,
        @Query("startIndex") startIndex: Int,
        @Query("maxResults") maxResults: Int,
        @Query("printType") printType: String = "books",
    ): VolumesResponse

    @GET("volumes/{bookId}")
    suspend fun getVolume(
        @Path("bookId") bookId: String,
    ): VolumeDto
}
