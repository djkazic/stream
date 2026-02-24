package com.stream.torrent.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TorrentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun torrentDao(): TorrentDao
}
