package com.stream.torrent.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stream.torrent.ui.player.PlayerScreen
import com.stream.torrent.ui.settings.SettingsScreen
import com.stream.torrent.ui.torrentdetail.TorrentDetailScreen
import com.stream.torrent.ui.torrentlist.TorrentListScreen

@Composable
fun StreamNavHost(
    navController: NavHostController = rememberNavController(),
    onTorrentFileReceived: (Uri) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TorrentList.route
    ) {
        composable(Screen.TorrentList.route) {
            TorrentListScreen(
                onTorrentClick = { infoHash ->
                    navController.navigate(Screen.TorrentDetail.createRoute(infoHash))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.TorrentDetail.route,
            arguments = listOf(navArgument("infoHash") { type = NavType.StringType })
        ) { backStackEntry ->
            val infoHash = backStackEntry.arguments?.getString("infoHash") ?: return@composable
            TorrentDetailScreen(
                infoHash = infoHash,
                onPlayFile = { fileIndex ->
                    navController.navigate(Screen.Player.createRoute(infoHash, fileIndex))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("infoHash") { type = NavType.StringType },
                navArgument("fileIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val infoHash = backStackEntry.arguments?.getString("infoHash") ?: return@composable
            val fileIndex = backStackEntry.arguments?.getInt("fileIndex") ?: return@composable
            PlayerScreen(
                infoHash = infoHash,
                fileIndex = fileIndex,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
