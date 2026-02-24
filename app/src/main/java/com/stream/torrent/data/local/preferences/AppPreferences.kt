package com.stream.torrent.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val downloadPath: Flow<String>
        get() = dataStore.data.map { it[KEY_DOWNLOAD_PATH] ?: "" }

    val maxDownloadSpeed: Flow<Int>
        get() = dataStore.data.map { it[KEY_MAX_DOWNLOAD_SPEED] ?: 0 }

    val maxUploadSpeed: Flow<Int>
        get() = dataStore.data.map { it[KEY_MAX_UPLOAD_SPEED] ?: 0 }

    val maxConcurrentTorrents: Flow<Int>
        get() = dataStore.data.map { it[KEY_MAX_CONCURRENT] ?: 3 }

    val isDhtEnabled: Flow<Boolean>
        get() = dataStore.data.map { it[KEY_DHT_ENABLED] ?: true }

    val isEncryptionEnabled: Flow<Boolean>
        get() = dataStore.data.map { it[KEY_ENCRYPTION_ENABLED] ?: true }

    suspend fun setDownloadPath(path: String) {
        dataStore.edit { it[KEY_DOWNLOAD_PATH] = path }
    }

    suspend fun setMaxDownloadSpeed(speed: Int) {
        dataStore.edit { it[KEY_MAX_DOWNLOAD_SPEED] = speed }
    }

    suspend fun setMaxUploadSpeed(speed: Int) {
        dataStore.edit { it[KEY_MAX_UPLOAD_SPEED] = speed }
    }

    suspend fun setMaxConcurrentTorrents(max: Int) {
        dataStore.edit { it[KEY_MAX_CONCURRENT] = max }
    }

    suspend fun setDhtEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DHT_ENABLED] = enabled }
    }

    suspend fun setEncryptionEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENCRYPTION_ENABLED] = enabled }
    }

    companion object {
        private val KEY_DOWNLOAD_PATH = stringPreferencesKey("download_path")
        private val KEY_MAX_DOWNLOAD_SPEED = intPreferencesKey("max_download_speed")
        private val KEY_MAX_UPLOAD_SPEED = intPreferencesKey("max_upload_speed")
        private val KEY_MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        private val KEY_DHT_ENABLED = booleanPreferencesKey("dht_enabled")
        private val KEY_ENCRYPTION_ENABLED = booleanPreferencesKey("encryption_enabled")
    }
}
