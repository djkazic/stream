package com.stream.torrent.player

import androidx.media3.datasource.DataSource
import com.stream.torrent.engine.PiecePriorityManager
import com.stream.torrent.engine.TorrentEngine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class TorrentDataSourceFactory(
    private val engine: TorrentEngine,
    private val piecePriorityManager: PiecePriorityManager,
    private val infoHash: String,
    private val fileIndex: Int,
    val pieceLength: Int,
    val fileOffset: Long
) : DataSource.Factory {

    // Dedicated thread for JNI calls — avoids ForkJoinPool work-stealing deadlocks
    val jniExecutor: ExecutorService = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "torrent-jni").apply { isDaemon = true }
    }

    override fun createDataSource(): DataSource {
        return TorrentDataSource(engine, piecePriorityManager, infoHash, fileIndex, this)
    }
}
