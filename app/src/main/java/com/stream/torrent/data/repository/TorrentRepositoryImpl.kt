package com.stream.torrent.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.stream.torrent.data.local.db.TorrentDao
import com.stream.torrent.data.local.db.TorrentEntity
import com.stream.torrent.domain.model.FilePriority
import com.stream.torrent.domain.model.StreamingState
import com.stream.torrent.domain.model.TrackerInfo
import com.stream.torrent.domain.model.TorrentFileItem
import com.stream.torrent.domain.model.TorrentModel
import com.stream.torrent.domain.model.TorrentStatus
import com.stream.torrent.engine.TorrentEngine
import com.stream.torrent.util.MimeTypeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.libtorrent4j.FileStorage
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.TorrentStatus as LtStatus
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val torrentDao: TorrentDao,
    private val engine: TorrentEngine
) : TorrentRepository {

    override fun getTorrentList(): Flow<List<TorrentModel>> = flow {
        while (true) {
            val entities = torrentDao.getAllTorrentsOnce()
            val models = entities.map { entity ->
                val handle = engine.getHandle(entity.infoHash)
                if (handle != null && handle.isValid) {
                    try {
                        mapToModel(entity, handle)
                    } catch (e: Exception) {
                        mapToOfflineModel(entity)
                    }
                } else {
                    mapToOfflineModel(entity)
                }
            }
            emit(models)
            delay(1_500)
        }
    }.flowOn(Dispatchers.IO)

    override fun getTorrentDetail(infoHash: String): Flow<TorrentModel?> = flow {
        while (true) {
            val entity = torrentDao.getTorrentByHash(infoHash)
            if (entity != null) {
                val handle = engine.getHandle(infoHash)
                if (handle != null && handle.isValid) {
                    try {
                        emit(mapToModel(entity, handle))
                    } catch (e: Exception) {
                        emit(mapToOfflineModel(entity))
                    }
                } else {
                    emit(mapToOfflineModel(entity))
                }
            } else {
                emit(null)
            }
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun addTorrent(torrentUri: Uri): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(torrentUri)
                ?: return Result.failure(Exception("Cannot open torrent file"))
            val bytes = inputStream.use { it.readBytes() }
            val torrentInfo = TorrentInfo(bytes)
            val infoHash = torrentInfo.infoHash().toHex()

            val torrentDir = File(context.getExternalFilesDir(null), "torrents")
            torrentDir.mkdirs()
            val torrentFile = File(torrentDir, "$infoHash.torrent")
            torrentFile.writeBytes(bytes)

            val downloadDir = File(context.getExternalFilesDir(null), "downloads")
            downloadDir.mkdirs()

            val entity = TorrentEntity(
                infoHash = infoHash,
                name = torrentInfo.name(),
                savePath = downloadDir.absolutePath,
                torrentFilePath = torrentFile.absolutePath,
                totalSize = torrentInfo.totalSize(),
                isPrivate = torrentInfo.isPrivate,
                addedTimestamp = System.currentTimeMillis()
            )
            torrentDao.insertTorrent(entity)
            engine.addTorrent(torrentFile, downloadDir)
            Result.success(infoHash)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        // Look up entity before deleting from DB (need name + savePath for file cleanup)
        val entity = torrentDao.getTorrentByHash(infoHash)

        engine.removeTorrent(infoHash, deleteFiles)
        torrentDao.deleteTorrent(infoHash)

        // Delete the saved .torrent file
        val torrentDir = File(context.getExternalFilesDir(null), "torrents")
        File(torrentDir, "$infoHash.torrent").delete()

        // Delete downloaded data files (fallback in case libtorrent couldn't do it)
        if (deleteFiles && entity != null) {
            val downloadPath = File(entity.savePath, entity.name)
            if (downloadPath.exists()) {
                downloadPath.deleteRecursively()
                Log.i("TorrentRepository", "Deleted download: ${downloadPath.absolutePath}")
            }
        }
    }

    override suspend fun pauseTorrent(infoHash: String) {
        engine.pauseTorrent(infoHash)
        val entity = torrentDao.getTorrentByHash(infoHash)
        entity?.let { torrentDao.updateTorrent(it.copy(isPaused = true)) }
    }

    override suspend fun resumeTorrent(infoHash: String) {
        engine.resumeTorrent(infoHash)
        val entity = torrentDao.getTorrentByHash(infoHash)
        entity?.let { torrentDao.updateTorrent(it.copy(isPaused = false)) }
    }

    override fun startStreaming(infoHash: String, fileIndex: Int): Flow<StreamingState> = flow {
        emit(StreamingState.Buffering(0f, 0, 0))
        try {
            engine.enableSequentialDownload(infoHash, fileIndex)
            while (true) {
                val handle = engine.getHandle(infoHash)
                if (handle == null || !handle.isValid) {
                    emit(StreamingState.Error("Torrent not found"))
                    return@flow
                }
                val ti = handle.torrentFile()
                if (ti == null) {
                    delay(500)
                } else {
                    val state = engine.getStreamingState(infoHash, fileIndex)
                    emit(state)
                    if (state is StreamingState.Ready) return@flow
                    delay(500)
                }
            }
        } catch (e: Exception) {
            emit(StreamingState.Error(e.message ?: "Streaming failed"))
        }
    }.flowOn(Dispatchers.IO)

    private fun mapToModel(entity: TorrentEntity, handle: TorrentHandle): TorrentModel {
        if (!handle.isValid) return mapToOfflineModel(entity)
        val status = handle.status()
        val ti = handle.torrentFile()
        val files = if (ti != null) {
            mapFiles(ti.files(), handle)
        } else {
            emptyList()
        }
        val trackers = mapTrackers(handle)

        return TorrentModel(
            id = entity.infoHash,
            name = entity.name,
            infoHash = entity.infoHash,
            savePath = entity.savePath,
            totalSize = entity.totalSize,
            downloadedBytes = status.totalDone(),
            uploadedBytes = status.allTimeUpload(),
            downloadSpeed = status.downloadPayloadRate().toLong(),
            uploadSpeed = status.uploadPayloadRate().toLong(),
            progress = status.progress(),
            status = mapStatus(status, entity.isPaused),
            numPeers = status.numPeers(),
            numSeeds = status.numSeeds(),
            isPrivate = entity.isPrivate,
            files = files,
            trackers = trackers,
            addedTimestamp = entity.addedTimestamp
        )
    }

    private fun mapToOfflineModel(entity: TorrentEntity): TorrentModel {
        return TorrentModel(
            id = entity.infoHash,
            name = entity.name,
            infoHash = entity.infoHash,
            savePath = entity.savePath,
            totalSize = entity.totalSize,
            downloadedBytes = 0,
            uploadedBytes = 0,
            downloadSpeed = 0,
            uploadSpeed = 0,
            progress = 0f,
            status = if (entity.isPaused) TorrentStatus.PAUSED else TorrentStatus.STOPPED,
            numPeers = 0,
            numSeeds = 0,
            isPrivate = entity.isPrivate,
            files = emptyList(),
            trackers = emptyList(),
            addedTimestamp = entity.addedTimestamp
        )
    }

    private fun mapFiles(fs: FileStorage, handle: TorrentHandle): List<TorrentFileItem> {
        val fileProgress = handle.fileProgress()
        return (0 until fs.numFiles()).map { i ->
            val path = fs.filePath(i)
            val name = fs.fileName(i)
            val size = fs.fileSize(i)
            val progress = if (size > 0) fileProgress[i].toFloat() / size else 0f
            TorrentFileItem(
                index = i,
                name = name,
                path = path,
                size = size,
                progress = progress,
                priority = FilePriority.NORMAL,
                isMedia = MimeTypeUtils.isMediaFile(name)
            )
        }
    }

    private fun mapTrackers(handle: TorrentHandle): List<TrackerInfo> {
        return try {
            handle.trackers().map { entry ->
                val endpoints = entry.endpoints()
                val (status, message) = if (endpoints.isNotEmpty()) {
                    val ih = endpoints.first().infohashV1()
                    val statusStr = if (ih.isWorking) "Working" else if (ih.fails() > 0) "Failed" else "Not contacted"
                    val msg = ih.message() ?: ""
                    statusStr to msg
                } else {
                    "Not contacted" to ""
                }
                TrackerInfo(
                    url = entry.url(),
                    status = status,
                    message = message
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapStatus(status: LtStatus, isPaused: Boolean): TorrentStatus {
        if (isPaused) return TorrentStatus.PAUSED
        return when (status.state()) {
            LtStatus.State.CHECKING_FILES, LtStatus.State.CHECKING_RESUME_DATA -> TorrentStatus.CHECKING
            LtStatus.State.DOWNLOADING_METADATA -> TorrentStatus.METADATA
            LtStatus.State.DOWNLOADING -> TorrentStatus.DOWNLOADING
            LtStatus.State.FINISHED, LtStatus.State.SEEDING -> TorrentStatus.SEEDING
            LtStatus.State.UNKNOWN -> TorrentStatus.DOWNLOADING // don't show as stopped for transient UNKNOWN state
            else -> TorrentStatus.DOWNLOADING
        }
    }
}
