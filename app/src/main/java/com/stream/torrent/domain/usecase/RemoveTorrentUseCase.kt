package com.stream.torrent.domain.usecase

import com.stream.torrent.data.repository.TorrentRepository
import javax.inject.Inject

class RemoveTorrentUseCase @Inject constructor(
    private val repository: TorrentRepository
) {
    suspend operator fun invoke(infoHash: String, deleteFiles: Boolean) {
        repository.removeTorrent(infoHash, deleteFiles)
    }
}
