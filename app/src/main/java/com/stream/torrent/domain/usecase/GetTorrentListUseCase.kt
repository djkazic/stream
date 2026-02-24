package com.stream.torrent.domain.usecase

import com.stream.torrent.data.repository.TorrentRepository
import com.stream.torrent.domain.model.TorrentModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTorrentListUseCase @Inject constructor(
    private val repository: TorrentRepository
) {
    operator fun invoke(): Flow<List<TorrentModel>> {
        return repository.getTorrentList()
    }
}
