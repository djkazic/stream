package com.stream.torrent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.stream.torrent.MainActivity
import com.stream.torrent.R
import com.stream.torrent.data.local.db.TorrentDao
import com.stream.torrent.engine.TorrentEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class TorrentService : Service() {

    companion object {
        private const val CHANNEL_ID = "stream_torrent_channel"
        private const val NOTIFICATION_ID = 1
    }

    @Inject lateinit var engine: TorrentEngine
    @Inject lateinit var torrentDao: TorrentDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))
        serviceScope.launch(Dispatchers.IO) {
            engine.start()
            restoreTorrents()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun restoreTorrents() {
        val entities = torrentDao.getAllTorrentsOnce()
        val torrentsToRestore = entities.mapNotNull { entity ->
            val torrentFile = File(entity.torrentFilePath)
            val saveDir = File(entity.savePath)
            if (torrentFile.exists()) {
                torrentFile to saveDir
            } else {
                null
            }
        }
        engine.restoreTorrents(torrentsToRestore)

        // Pause torrents that were paused before
        entities.filter { it.isPaused }.forEach { entity ->
            engine.pauseTorrent(entity.infoHash)
        }

        updateNotification("Running - ${entities.size} torrents")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Torrent Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows torrent download progress"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stream")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }
}
