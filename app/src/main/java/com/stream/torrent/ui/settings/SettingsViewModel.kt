package com.stream.torrent.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.torrent.data.local.preferences.AppPreferences
import com.stream.torrent.engine.TorrentEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val engine: TorrentEngine
) : ViewModel() {

    val downloadPath = preferences.downloadPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val maxDownloadSpeed = preferences.maxDownloadSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val maxUploadSpeed = preferences.maxUploadSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val maxConcurrentTorrents = preferences.maxConcurrentTorrents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val isDhtEnabled = preferences.isDhtEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isEncryptionEnabled = preferences.isEncryptionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setMaxDownloadSpeed(speed: Int) {
        viewModelScope.launch {
            preferences.setMaxDownloadSpeed(speed)
            engine.setDownloadSpeedLimit(speed * 1024)
        }
    }

    fun setMaxUploadSpeed(speed: Int) {
        viewModelScope.launch {
            preferences.setMaxUploadSpeed(speed)
            engine.setUploadSpeedLimit(speed * 1024)
        }
    }

    fun setMaxConcurrentTorrents(max: Int) {
        viewModelScope.launch {
            preferences.setMaxConcurrentTorrents(max)
        }
    }

    fun setDhtEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDhtEnabled(enabled)
            engine.setDhtEnabled(enabled)
        }
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setEncryptionEnabled(enabled)
        }
    }
}
