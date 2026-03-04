package com.example.jonesspencer_ce07.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, FavoriteEntity::class, RemoteKeysEntity::class, SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BooksDatabase : RoomDatabase() {
    abstract fun booksDao(): BooksDao
}
