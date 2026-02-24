package com.stream.torrent.domain.usecase

import com.stream.torrent.data.repository.TorrentRepository
import com.stream.torrent.domain.model.StreamingState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StartStreamingUseCase @Inject constructor(
    private val repository: TorrentRepository
) {
    operator fun invoke(infoHash: String, fileIndex: Int): Flow<StreamingState> {
        return repository.startStreaming(infoHash, fileIndex)
    }
}
