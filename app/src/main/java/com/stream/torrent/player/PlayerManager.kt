package com.stream.torrent.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.stream.torrent.engine.PiecePriorityManager
import com.stream.torrent.engine.TorrentEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: TorrentEngine,
    private val piecePriorityManager: PiecePriorityManager
) {
    companion object {
        private const val TAG = "PlayerManager"
    }

    private var player: ExoPlayer? = null

    fun getPlayer(): ExoPlayer {
        return player ?: createPlayer().also { player = it }
    }

    private fun createPlayer(): ExoPlayer {
        // Use smaller buffers so playback starts quickly with partial downloads
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,   // minBufferMs — keep at least 5s buffered
                30_000,  // maxBufferMs — buffer up to 30s ahead
                1_000,   // bufferForPlaybackMs — start playing after 1s of data
                2_000    // bufferForPlaybackAfterRebufferMs — resume after 2s on rebuffer
            )
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} - ${error.message}", error)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val stateName = when (playbackState) {
                            Player.STATE_IDLE -> "IDLE"
                            Player.STATE_BUFFERING -> "BUFFERING"
                            Player.STATE_READY -> "READY"
                            Player.STATE_ENDED -> "ENDED"
                            else -> "UNKNOWN"
                        }
                        Log.i(TAG, "Playback state: $stateName")
                    }

                    override fun onRenderedFirstFrame() {
                        Log.i(TAG, "Rendered first frame")
                    }
                })
            }
    }

    fun prepareForStreaming(filePath: String, infoHash: String, fileIndex: Int) {
        val file = File(filePath)
        Log.i(TAG, "prepareForStreaming: path=$filePath, exists=${file.exists()}, size=${file.length()}")

        // Pre-fetch torrent metadata now (before piece checking blocks the session)
        var pieceLength = 0
        var fileOffset = 0L
        try {
            val handle = engine.getHandle(infoHash)
            if (handle != null) {
                val ti = handle.torrentFile()
                if (ti != null) {
                    pieceLength = ti.pieceLength()
                    fileOffset = ti.files().fileOffset(fileIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not pre-fetch metadata: ${e.message}")
        }
        Log.i(TAG, "Metadata: pieceLength=$pieceLength, fileOffset=$fileOffset")

        val exoPlayer = getPlayer()
        val dataSourceFactory = TorrentDataSourceFactory(
            engine, piecePriorityManager, infoHash, fileIndex, pieceLength, fileOffset
        )
        val fileUri = Uri.fromFile(file)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(fileUri))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    fun play() {
        player?.playWhenReady = true
    }

    fun pause() {
        player?.pause()
    }

    fun release() {
        player?.release()
        player = null
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }
}
