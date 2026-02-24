package com.stream.torrent.domain.usecase

import android.net.Uri
import com.stream.torrent.data.repository.TorrentRepository
import javax.inject.Inject

class AddTorrentUseCase @Inject constructor(
    private val repository: TorrentRepository
) {
    suspend operator fun invoke(torrentUri: Uri): Result<String> {
        return repository.addTorrent(torrentUri)
    }
}
