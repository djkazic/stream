package com.stream.torrent.util

import java.util.Locale

object FormatUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceAtMost(units.size - 1)
        return String.format(
            Locale.US,
            "%.1f %s",
            bytes / Math.pow(1024.0, index.toDouble()),
            units[index]
        )
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }

    fun formatProgress(progress: Float): String {
        return String.format(Locale.US, "%.1f%%", progress * 100)
    }

    fun formatEta(downloadSpeed: Long, remainingBytes: Long): String {
        if (downloadSpeed <= 0) return "∞"
        val seconds = remainingBytes / downloadSpeed
        return formatDuration(seconds)
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format(Locale.US, "%dh %02dm", hours, minutes)
            minutes > 0 -> String.format(Locale.US, "%dm %02ds", minutes, secs)
            else -> String.format(Locale.US, "%ds", secs)
        }
    }

    fun formatInfoHash(hash: String): String {
        return hash.take(8) + "..." + hash.takeLast(8)
    }
}
