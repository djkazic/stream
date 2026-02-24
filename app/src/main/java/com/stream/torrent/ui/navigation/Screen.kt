package com.stream.torrent.ui.navigation

sealed class Screen(val route: String) {
    data object TorrentList : Screen("torrent_list")
    data object TorrentDetail : Screen("torrent_detail/{infoHash}") {
        fun createRoute(infoHash: String) = "torrent_detail/$infoHash"
    }
    data object Player : Screen("player/{infoHash}/{fileIndex}") {
        fun createRoute(infoHash: String, fileIndex: Int) = "player/$infoHash/$fileIndex"
    }
    data object Settings : Screen("settings")
}
