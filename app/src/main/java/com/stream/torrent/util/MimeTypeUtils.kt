package com.stream.torrent.util

object MimeTypeUtils {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "ts", "3gp"
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "aac", "ogg", "wav", "wma", "m4a", "opus"
    )

    private val MEDIA_EXTENSIONS = VIDEO_EXTENSIONS + AUDIO_EXTENSIONS

    fun isMediaFile(fileName: String): Boolean {
        val ext = FileUtils.getFileExtension(fileName)
        return ext in MEDIA_EXTENSIONS
    }

    fun isVideoFile(fileName: String): Boolean {
        val ext = FileUtils.getFileExtension(fileName)
        return ext in VIDEO_EXTENSIONS
    }

    fun isAudioFile(fileName: String): Boolean {
        val ext = FileUtils.getFileExtension(fileName)
        return ext in AUDIO_EXTENSIONS
    }

    fun getMimeType(fileName: String): String {
        val ext = FileUtils.getFileExtension(fileName)
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "ts" -> "video/mp2t"
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            else -> "application/octet-stream"
        }
    }
}
