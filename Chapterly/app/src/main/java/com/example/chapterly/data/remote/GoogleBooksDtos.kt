package com.example.chapterly.data.remote

import com.squareup.moshi.Json

data class VolumesResponse(
    @Json(name = "totalItems") val totalItems: Int = 0,
    @Json(name = "items") val items: List<VolumeDto> = emptyList(),
)

data class VolumeDto(
    @Json(name = "id") val id: String,
    @Json(name = "volumeInfo") val volumeInfo: VolumeInfoDto? = null,
)

data class VolumeInfoDto(
    @Json(name = "title") val title: String? = null,
    @Json(name = "authors") val authors: List<String>? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "imageLinks") val imageLinks: ImageLinksDto? = null,
    @Json(name = "previewLink") val previewLink: String? = null,
    @Json(name = "publishedDate") val publishedDate: String? = null,
    @Json(name = "pageCount") val pageCount: Int? = null,
    @Json(name = "averageRating") val averageRating: Double? = null,
)

data class ImageLinksDto(
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "smallThumbnail") val smallThumbnail: String? = null,
)
