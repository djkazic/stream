package com.stream.torrent.domain.model

data class TorrentFileItem(
    val index: Int,
    val name: String,
    val path: String,
    val size: Long,
    val progress: Float,
    val priority: FilePriority,
    val isMedia: Boolean
)

enum class FilePriority {
    SKIP, LOW, NORMAL, HIGH, TOP
}
