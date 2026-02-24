package com.stream.torrent.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "torrents")
data class TorrentEntity(
    @PrimaryKey
    val infoHash: String,
    val name: String,
    val savePath: String,
    val torrentFilePath: String,
    val totalSize: Long,
    val isPrivate: Boolean,
    val addedTimestamp: Long,
    val isPaused: Boolean = false
)
