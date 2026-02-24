package com.stream.torrent.ui.player

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.torrent.domain.model.StreamingState
import com.stream.torrent.domain.usecase.StartStreamingUseCase
import com.stream.torrent.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val startStreamingUseCase: StartStreamingUseCase,
    val playerManager: PlayerManager
) : ViewModel() {

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    val infoHash: String = savedStateHandle.get<String>("infoHash") ?: ""
    val fileIndex: Int = savedStateHandle.get<Int>("fileIndex") ?: 0

    private val _streamingState = MutableStateFlow<StreamingState>(StreamingState.Idle)
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()

    init {
        Log.i(TAG, "init: infoHash=$infoHash, fileIndex=$fileIndex")
        startStreaming()
    }

    private fun startStreaming() {
        viewModelScope.launch {
            startStreamingUseCase(infoHash, fileIndex).collect { state ->
                Log.i(TAG, "Streaming state: $state")
                _streamingState.value = state
                if (state is StreamingState.Ready) {
                    Log.i(TAG, "Ready! filePath=${state.filePath}")
                    playerManager.prepareForStreaming(state.filePath, infoHash, fileIndex)
                    playerManager.play()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
