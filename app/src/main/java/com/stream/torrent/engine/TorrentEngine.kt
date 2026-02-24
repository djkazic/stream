package com.stream.torrent.engine

import com.stream.torrent.domain.model.StreamingState
import org.libtorrent4j.TorrentHandle
import java.io.File

interface TorrentEngine {
    fun start()
    fun stop()
    fun addTorrent(torrentFile: File, saveDir: File)
    fun removeTorrent(infoHash: String, deleteFiles: Boolean)
    fun pauseTorrent(infoHash: String)
    fun resumeTorrent(infoHash: String)
    fun getHandle(infoHash: String): TorrentHandle?
    fun enableSequentialDownload(infoHash: String, fileIndex: Int)
    fun getStreamingState(infoHash: String, fileIndex: Int): StreamingState
    fun setDownloadSpeedLimit(bytesPerSecond: Int)
    fun setUploadSpeedLimit(bytesPerSecond: Int)
    fun setDhtEnabled(enabled: Boolean)
    fun restoreTorrents(torrents: List<Pair<File, File>>)
}
