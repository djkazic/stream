package com.stream.torrent.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TorrentDao {
    @Query("SELECT * FROM torrents ORDER BY addedTimestamp DESC")
    fun getAllTorrents(): Flow<List<TorrentEntity>>

    @Query("SELECT * FROM torrents WHERE infoHash = :infoHash")
    suspend fun getTorrentByHash(infoHash: String): TorrentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorrent(torrent: TorrentEntity)

    @Update
    suspend fun updateTorrent(torrent: TorrentEntity)

    @Query("DELETE FROM torrents WHERE infoHash = :infoHash")
    suspend fun deleteTorrent(infoHash: String)

    @Query("SELECT * FROM torrents")
    suspend fun getAllTorrentsOnce(): List<TorrentEntity>
}
