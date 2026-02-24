package com.stream.torrent.engine

import android.util.Log
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.TorrentAlert
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertHandler @Inject constructor() {

    companion object {
        private const val TAG = "AlertHandler"
    }

    fun handleAlert(alert: Alert<*>, knownInfoHashes: MutableSet<String>) {
        try {
            when (alert.type()) {
                AlertType.ADD_TORRENT -> {
                    val torrentAlert = alert as? TorrentAlert<*>
                    torrentAlert?.let {
                        try {
                            val hash = it.handle().infoHash().toHex()
                            knownInfoHashes.add(hash)
                        } catch (_: Exception) { }
                    }
                }
                AlertType.TORRENT_FINISHED -> {
                    Log.i(TAG, "Torrent finished: ${alert.message()}")
                }
                AlertType.TORRENT_ERROR -> {
                    Log.w(TAG, "Torrent error: ${alert.message()}")
                }
                AlertType.TRACKER_ERROR -> {
                    Log.w(TAG, "Tracker error: ${alert.message()}")
                }
                else -> { }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling alert: ${e.message}")
        }
    }
}
