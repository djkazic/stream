package com.stream.torrent.engine

import android.content.Context
import android.util.Log
import com.stream.torrent.domain.model.StreamingState
import dagger.hilt.android.qualifiers.ApplicationContext
import org.libtorrent4j.AlertListener
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.swig.libtorrent
import org.libtorrent4j.swig.session_handle
import org.libtorrent4j.swig.settings_pack
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertHandler: AlertHandler,
    private val piecePriorityManager: PiecePriorityManager
) : TorrentEngine {

    companion object {
        private const val TAG = "TorrentEngine"
    }

    private var sessionManager: SessionManager? = null
    // Only track info hash strings — never cache TorrentHandle objects
    // because their native pointers can become dangling, causing SIGSEGV
    private val knownInfoHashes: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val startedLatch = CountDownLatch(1)

    /**
     * Look up a fresh TorrentHandle from the session. Never caches the result.
     * Returns null if session is not running or torrent not found.
     */
    private fun findHandle(infoHash: String): TorrentHandle? {
        val session = sessionManager ?: return null
        return try {
            if (!session.isRunning) return null
            val hash = Sha1Hash.parseHex(infoHash)
            val handle = session.find(hash)
            if (handle != null && handle.isValid) handle else null
        } catch (e: Exception) {
            Log.w(TAG, "Error finding handle for $infoHash: ${e.message}")
            null
        }
    }

    @Synchronized
    override fun start() {
        if (sessionManager != null) return

        val settings = SettingsPack().apply {
            // Enable all alert categories so we receive tracker/peer/error alerts
            // Use 0x7fffffff to avoid sign issues with all_categories (-1 as signed int)
            setInteger(settings_pack.int_types.alert_mask.swigValue(), 0x7fffffff)

            // Listen on all interfaces, random port in typical range
            listenInterfaces("0.0.0.0:6881,[::]:6881")

            // DHT for public torrents
            setDhtBootstrapNodes("dht.transmissionbt.com:6881,dht.libtorrent.org:25401,router.bittorrent.com:6881")
            setEnableDht(true)
            setEnableLsd(true)

            // Enable protocol encryption (required by many trackers)
            setInteger(settings_pack.int_types.in_enc_policy.swigValue(), settings_pack.enc_policy.pe_enabled.swigValue())
            setInteger(settings_pack.int_types.out_enc_policy.swigValue(), settings_pack.enc_policy.pe_enabled.swigValue())
            setInteger(settings_pack.int_types.allowed_enc_level.swigValue(), settings_pack.enc_level.pe_both.swigValue())

            // Disable HTTPS tracker cert validation — Android's native layer
            // can't access the system CA store, causing all HTTPS announces to fail
            setBoolean(settings_pack.bool_types.validate_https_trackers.swigValue(), false)

            anonymousMode(false)
            connectionsLimit(200)
            activeDownloads(3)
            activeSeeds(5)
            activeLimit(10)
        }

        val sessionDir = File(context.filesDir, "session")
        sessionDir.mkdirs()

        val session = SessionManager(false)
        session.addListener(object : AlertListener {
            override fun types(): IntArray? = null
            override fun alert(alert: Alert<*>) {
                alertHandler.handleAlert(alert, knownInfoHashes)
            }
        })
        session.start(SessionParams(settings))

        // Wait for the session to actually be running
        val waitStart = System.currentTimeMillis()
        while (!session.isRunning && System.currentTimeMillis() - waitStart < 10_000) {
            Thread.sleep(100)
        }

        val resumeDir = File(context.filesDir, "resume")
        resumeDir.mkdirs()

        sessionManager = session
        startedLatch.countDown()
        Log.i(TAG, "Torrent engine started (running=${session.isRunning})")
    }

    /** Block until the engine has started, with a timeout. */
    private fun awaitStarted() {
        startedLatch.await(15, TimeUnit.SECONDS)
    }

    @Synchronized
    override fun stop() {
        sessionManager?.let { session ->
            // Try to save resume data using fresh handles
            for (infoHash in knownInfoHashes) {
                try {
                    val handle = findHandle(infoHash)
                    handle?.saveResumeData()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save resume data for $infoHash: ${e.message}")
                }
            }
            session.stop()
            knownInfoHashes.clear()
            sessionManager = null
            Log.i(TAG, "Torrent engine stopped")
        }
    }

    override fun addTorrent(torrentFile: File, saveDir: File) {
        awaitStarted()
        val session = sessionManager
        if (session == null || !session.isRunning) {
            Log.e(TAG, "Cannot add torrent: session not running")
            return
        }

        // Use load_torrent_file to get add_torrent_params with trackers preserved
        // (session.download(TorrentInfo, File) loses trackers in libtorrent2)
        val ec = org.libtorrent4j.swig.error_code()
        val params = libtorrent.load_torrent_file(torrentFile.absolutePath, ec, org.libtorrent4j.swig.load_torrent_limits())
        if (ec.value() != 0) {
            Log.e(TAG, "Failed to load torrent file: ${ec.message()}")
            return
        }
        params.setSave_path(saveDir.absolutePath)

        val infoHash = Sha1Hash(params.getInfo_hashes().get_best()).toHex()
        Log.i(TAG, "Adding torrent: $infoHash")

        // Add torrent synchronously via swig session handle
        val addEc = org.libtorrent4j.swig.error_code()
        session.swig().add_torrent(params, addEc)
        if (addEc.value() != 0) {
            Log.e(TAG, "Failed to add torrent: ${addEc.message()}")
            return
        }

        knownInfoHashes.add(infoHash)

        // Brief wait for handle, then ensure it's resumed
        Thread.sleep(200)
        try {
            findHandle(infoHash)?.resume()
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming torrent: ${e.message}")
        }
    }

    override fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        knownInfoHashes.remove(infoHash)
        val session = sessionManager ?: return
        try {
            val handle = findHandle(infoHash) ?: return
            if (deleteFiles) {
                session.remove(handle, session_handle.delete_files)
            } else {
                session.remove(handle)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing torrent: ${e.message}")
        }
        Log.i(TAG, "Removed torrent: $infoHash")
    }

    override fun pauseTorrent(infoHash: String) {
        try {
            findHandle(infoHash)?.pause()
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing torrent: ${e.message}")
        }
    }

    override fun resumeTorrent(infoHash: String) {
        try {
            findHandle(infoHash)?.resume()
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming torrent: ${e.message}")
        }
    }

    override fun getHandle(infoHash: String): TorrentHandle? {
        return findHandle(infoHash)
    }

    override fun enableSequentialDownload(infoHash: String, fileIndex: Int) {
        try {
            val handle = findHandle(infoHash) ?: return
            handle.setFlags(libtorrent.getSequential_download())
            piecePriorityManager.setupPriorities(handle, fileIndex)
        } catch (e: Exception) {
            Log.w(TAG, "Error enabling sequential download: ${e.message}")
        }
    }

    override fun getStreamingState(infoHash: String, fileIndex: Int): StreamingState {
        val handle = findHandle(infoHash)
            ?: return StreamingState.Error("Torrent not found")
        return try {
            piecePriorityManager.getStreamingState(handle, fileIndex)
        } catch (e: Exception) {
            StreamingState.Error(e.message ?: "Unknown error")
        }
    }

    override fun setDownloadSpeedLimit(bytesPerSecond: Int) {
        sessionManager?.downloadRateLimit(bytesPerSecond)
    }

    override fun setUploadSpeedLimit(bytesPerSecond: Int) {
        sessionManager?.uploadRateLimit(bytesPerSecond)
    }

    override fun setDhtEnabled(enabled: Boolean) {
        sessionManager?.let { session ->
            val settings = session.settings()
            settings.setEnableDht(enabled)
            session.applySettings(settings)
        }
    }

    override fun restoreTorrents(torrents: List<Pair<File, File>>) {
        awaitStarted()
        torrents.forEach { (torrentFile, saveDir) ->
            try {
                addTorrent(torrentFile, saveDir)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore torrent: ${e.message}")
            }
        }
    }
}
