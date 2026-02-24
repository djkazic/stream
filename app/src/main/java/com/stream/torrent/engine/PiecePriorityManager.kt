package com.stream.torrent.engine

import com.stream.torrent.domain.model.StreamingState
import org.libtorrent4j.Priority
import org.libtorrent4j.TorrentHandle
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PiecePriorityManager @Inject constructor() {

    companion object {
        private const val BUFFER_PIECES = 15
        private const val TAIL_BYTES = 2 * 1024 * 1024L // 2MB from end of file
    }

    /** Calculate how many tail pieces are needed to cover at least TAIL_BYTES. */
    private fun tailPieces(pieceLength: Int): Int {
        return ((TAIL_BYTES + pieceLength - 1) / pieceLength).toInt().coerceAtLeast(3)
    }

    fun setupPriorities(handle: TorrentHandle, fileIndex: Int) {
        val ti = handle.torrentFile() ?: return
        val fs = ti.files()
        val numPieces = ti.numPieces()
        val pieceLength = ti.pieceLength()

        val fileOffset = fs.fileOffset(fileIndex)
        val fileSize = fs.fileSize(fileIndex)
        val firstPiece = (fileOffset / pieceLength).toInt()
        val lastPiece = ((fileOffset + fileSize - 1) / pieceLength).toInt().coerceAtMost(numPieces - 1)

        val priorities = Priority.array(Priority.IGNORE, numPieces)
        for (i in firstPiece..lastPiece) {
            priorities[i] = Priority.DEFAULT
        }

        // Prioritize first N pieces for playback start
        val bufferEnd = (firstPiece + BUFFER_PIECES).coerceAtMost(lastPiece)
        for (i in firstPiece..bufferEnd) {
            priorities[i] = Priority.TOP_PRIORITY
        }

        // Prioritize tail pieces for container metadata (moov atom)
        val tailStart = (lastPiece - tailPieces(pieceLength) + 1).coerceAtLeast(firstPiece)
        for (i in tailStart..lastPiece) {
            priorities[i] = Priority.TOP_PRIORITY
        }

        handle.prioritizePieces(priorities)
    }

    fun advanceWindow(handle: TorrentHandle, fileIndex: Int, currentPosition: Long) {
        val ti = handle.torrentFile() ?: return
        val fs = ti.files()
        val pieceLength = ti.pieceLength()
        val fileOffset = fs.fileOffset(fileIndex)
        val currentPiece = ((fileOffset + currentPosition) / pieceLength).toInt()
        val numPieces = ti.numPieces()

        val windowEnd = (currentPiece + BUFFER_PIECES).coerceAtMost(numPieces - 1)
        for (i in currentPiece..windowEnd) {
            handle.piecePriority(i, Priority.TOP_PRIORITY)
        }
        handle.setPieceDeadline(currentPiece, 0)
    }

    fun getStreamingState(handle: TorrentHandle, fileIndex: Int): StreamingState {
        val ti = handle.torrentFile() ?: return StreamingState.Buffering(0f, 0, 0)
        val fs = ti.files()
        val pieceLength = ti.pieceLength()

        val fileOffset = fs.fileOffset(fileIndex)
        val fileSize = fs.fileSize(fileIndex)
        val firstPiece = (fileOffset / pieceLength).toInt()
        val lastPiece = ((fileOffset + fileSize - 1) / pieceLength).toInt()
            .coerceAtMost(ti.numPieces() - 1)

        val bufferEnd = (firstPiece + BUFFER_PIECES).coerceAtMost(lastPiece)
        val tailStart = (lastPiece - tailPieces(pieceLength) + 1).coerceAtLeast(firstPiece)

        var piecesReady = 0
        var piecesNeeded = 0

        for (i in firstPiece..bufferEnd) {
            piecesNeeded++
            if (handle.havePiece(i)) piecesReady++
        }

        for (i in tailStart..lastPiece) {
            if (i > bufferEnd) {
                piecesNeeded++
                if (handle.havePiece(i)) piecesReady++
            }
        }

        return if (piecesReady >= piecesNeeded) {
            val filePath = File(handle.savePath(), fs.filePath(fileIndex)).absolutePath
            StreamingState.Ready(filePath, fileIndex)
        } else {
            val progress = if (piecesNeeded > 0) piecesReady.toFloat() / piecesNeeded else 0f
            StreamingState.Buffering(progress, piecesReady, piecesNeeded)
        }
    }
}
