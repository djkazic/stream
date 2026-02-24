package com.stream.torrent.domain.model

sealed class StreamingState {
    data object Idle : StreamingState()
    data class Buffering(val progress: Float, val piecesReady: Int, val piecesNeeded: Int) : StreamingState()
    data class Ready(val filePath: String, val fileIndex: Int) : StreamingState()
    data class Error(val message: String) : StreamingState()
}
