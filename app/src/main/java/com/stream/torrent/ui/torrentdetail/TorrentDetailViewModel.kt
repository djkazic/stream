package com.stream.torrent.ui.torrentdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.torrent.data.repository.TorrentRepository
import com.stream.torrent.domain.model.TorrentModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TorrentRepository
) : ViewModel() {

    private val infoHash: String = savedStateHandle.get<String>("infoHash") ?: ""

    private val _torrent = MutableStateFlow<TorrentModel?>(null)
    val torrent: StateFlow<TorrentModel?> = _torrent.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getTorrentDetail(infoHash).collect { model ->
                _torrent.value = model
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun pauseTorrent() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.pauseTorrent(infoHash)
        }
    }

    fun resumeTorrent() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resumeTorrent(infoHash)
        }
    }
}
