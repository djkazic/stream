package com.stream.torrent.player

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.stream.torrent.engine.PiecePriorityManager
import com.stream.torrent.engine.TorrentEngine
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class TorrentDataSource(
    private val engine: TorrentEngine,
    private val piecePriorityManager: PiecePriorityManager,
    private val infoHash: String,
    private val fileIndex: Int,
    factory: TorrentDataSourceFactory
) : BaseDataSource(/* isNetwork= */ false) {

    companion object {
        private const val TAG = "TorrentDataSource"
        private const val ADVANCE_WINDOW_INTERVAL = 512 * 1024L
        private const val PIECE_CHECK_INTERVAL = 1048576L // check every 1MB during reads
    }

    private var randomAccessFile: RandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    private val pieceLength: Int = factory.pieceLength
    private val fileOffset: Long = factory.fileOffset
    private val jniExecutor: ExecutorService = factory.jniExecutor
    private var lastAdvancePosition: Long = -1
    private var lastCheckedPiece: Int = -1

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val filePath = dataSpec.uri.path ?: throw IllegalArgumentException("No file path in URI: ${dataSpec.uri}")
        val file = File(filePath)
        Log.i(TAG, "open: position=${dataSpec.position}")

        transferInitializing(dataSpec)
        waitForFile(file)

        randomAccessFile = RandomAccessFile(file, "r")
        val fileLength = randomAccessFile!!.length()

        val position = dataSpec.position
        randomAccessFile!!.seek(position)

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            fileLength - position
        }

        // Wait for the piece at the open position before returning
        waitForPiece(position)

        opened = true
        transferStarted(dataSpec)

        advanceWindow(position)

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val raf = randomAccessFile ?: return C.RESULT_END_OF_INPUT
        val currentPos = raf.filePointer

        // Only check pieces when crossing a piece boundary (not on every read)
        if (pieceLength > 0) {
            val currentPiece = ((fileOffset + currentPos) / pieceLength).toInt()
            if (currentPiece != lastCheckedPiece) {
                lastCheckedPiece = currentPiece
                waitForPiece(currentPos)
            }
        }

        val bytesToRead = length.toLong().coerceAtMost(bytesRemaining).toInt()
        val bytesRead = raf.read(buffer, offset, bytesToRead)

        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)

        // Advance the priority window periodically
        val newPos = raf.filePointer
        if (lastAdvancePosition < 0 || newPos - lastAdvancePosition >= ADVANCE_WINDOW_INTERVAL) {
            lastAdvancePosition = newPos
            advanceWindow(newPos)
        }

        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try {
            randomAccessFile?.close()
        } finally {
            randomAccessFile = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    private fun waitForFile(file: File) {
        var attempts = 0
        while (!file.exists() && attempts < 600) {
            if (attempts % 20 == 0) {
                Log.d(TAG, "Waiting for file: ${file.path} (attempt $attempts)")
            }
            Thread.sleep(500)
            attempts++
        }
        if (!file.exists()) {
            throw FileNotFoundException("Torrent file not available: ${file.path}")
        }
    }

    /**
     * Runs a JNI call on the dedicated executor with a timeout.
     * Returns null if the call blocks (session busy) or fails.
     */
    private fun <T> jniCall(timeoutMs: Long = 300, block: () -> T): T? {
        return try {
            jniExecutor.submit(Callable { block() }).get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            null // session busy
        } catch (e: Exception) {
            null
        }
    }

    private fun waitForPiece(position: Long) {
        if (pieceLength <= 0) return
        val pieceIndex = ((fileOffset + position) / pieceLength).toInt()

        var attempts = 0
        while (attempts < 600) {
            val result = jniCall {
                val handle = engine.getHandle(infoHash) ?: return@jniCall null
                if (handle.havePiece(pieceIndex)) return@jniCall true
                handle.setPieceDeadline(pieceIndex, 0)
                false
            }

            when (result) {
                true -> return
                null -> return // JNI unavailable — read from disk
                false -> { /* piece not ready, wait */ }
            }

            if (attempts % 100 == 0) {
                Log.i(TAG, "Waiting for piece $pieceIndex (attempt $attempts)")
            }
            Thread.sleep(100)
            attempts++
        }
        throw IOException("Timed out waiting for piece $pieceIndex")
    }

    private fun advanceWindow(position: Long) {
        jniCall(timeoutMs = 300) {
            val handle = engine.getHandle(infoHash) ?: return@jniCall
            piecePriorityManager.advanceWindow(handle, fileIndex, position)
        }
    }
}
