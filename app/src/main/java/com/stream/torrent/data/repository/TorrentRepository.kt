package com.stream.torrent.data.repository

import android.net.Uri
import com.stream.torrent.domain.model.StreamingState
import com.stream.torrent.domain.model.TorrentModel
import kotlinx.coroutines.flow.Flow

interface TorrentRepository {
    fun getTorrentList(): Flow<List<TorrentModel>>
    fun getTorrentDetail(infoHash: String): Flow<TorrentModel?>
    suspend fun addTorrent(torrentUri: Uri): Result<String>
    suspend fun removeTorrent(infoHash: String, deleteFiles: Boolean)
    suspend fun pauseTorrent(infoHash: String)
    suspend fun resumeTorrent(infoHash: String)
    fun startStreaming(infoHash: String, fileIndex: Int): Flow<StreamingState>
}
