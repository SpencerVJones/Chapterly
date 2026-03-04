package com.example.chapterly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        FavoriteEntity::class,
        RemoteKeysEntity::class,
        SearchHistoryEntity::class,
        ReadingProgressEntity::class,
        AnnotationEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
