package com.stream.torrent.domain.model

data class TrackerInfo(
    val url: String,
    val status: String,
    val message: String
)

data class TorrentModel(
    val id: String,
    val name: String,
    val infoHash: String,
    val savePath: String,
    val totalSize: Long,
    val downloadedBytes: Long,
    val uploadedBytes: Long,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val progress: Float,
    val status: TorrentStatus,
    val numPeers: Int,
    val numSeeds: Int,
    val isPrivate: Boolean,
    val files: List<TorrentFileItem>,
    val trackers: List<TrackerInfo>,
    val addedTimestamp: Long
)
