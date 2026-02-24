package com.stream.torrent.ui.torrentlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.torrent.domain.model.TorrentModel
import com.stream.torrent.domain.usecase.AddTorrentUseCase
import com.stream.torrent.domain.usecase.GetTorrentListUseCase
import com.stream.torrent.domain.usecase.RemoveTorrentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TorrentListViewModel @Inject constructor(
    private val getTorrentListUseCase: GetTorrentListUseCase,
    private val addTorrentUseCase: AddTorrentUseCase,
    private val removeTorrentUseCase: RemoveTorrentUseCase
) : ViewModel() {

    private val _torrents = MutableStateFlow<List<TorrentModel>>(emptyList())
    val torrents: StateFlow<List<TorrentModel>> = _torrents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    init {
        viewModelScope.launch {
            getTorrentListUseCase().collect { list ->
                _torrents.value = list
            }
        }
    }

    fun addTorrent(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = withContext(Dispatchers.IO) {
                addTorrentUseCase(uri)
            }
            result
                .onSuccess { _showAddSheet.value = false }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun removeTorrent(infoHash: String, deleteFiles: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            removeTorrentUseCase(infoHash, deleteFiles)
        }
    }

    fun showAddSheet() { _showAddSheet.value = true }
    fun hideAddSheet() { _showAddSheet.value = false }
    fun clearError() { _error.value = null }
}
