package com.example.jonesspencer_ce07.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeysEntity(
    @PrimaryKey val queryKey: String,
    val nextIndex: Int?,
    val endOfPaginationReached: Boolean
)
