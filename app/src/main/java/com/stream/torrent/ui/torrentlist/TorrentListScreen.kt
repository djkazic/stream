package com.stream.torrent.ui.torrentlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stream.torrent.domain.model.TorrentModel
import com.stream.torrent.domain.model.TorrentStatus
import com.stream.torrent.ui.addtorrent.AddTorrentSheet
import com.stream.torrent.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentListScreen(
    onTorrentClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: TorrentListViewModel = hiltViewModel()
) {
    val torrents by viewModel.torrents.collectAsState()
    val showAddSheet by viewModel.showAddSheet.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stream") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddSheet() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Torrent")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (torrents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No torrents",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add a .torrent file",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(torrents, key = { it.infoHash }) { torrent ->
                    SwipeToDismissTorrentItem(
                        torrent = torrent,
                        onClick = { onTorrentClick(torrent.infoHash) },
                        onDismiss = { viewModel.removeTorrent(torrent.infoHash, true) }
                    )
                }
            }
        }

        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideAddSheet() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AddTorrentSheet(
                    onTorrentSelected = { uri -> viewModel.addTorrent(uri) },
                    onDismiss = { viewModel.hideAddSheet() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissTorrentItem(
    torrent: TorrentModel,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error
                else Color.Transparent,
                label = "dismiss_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        TorrentListItem(torrent = torrent, onClick = onClick)
    }
}

@Composable
private fun TorrentListItem(
    torrent: TorrentModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = torrent.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { torrent.progress },
                modifier = Modifier.fillMaxWidth(),
                drawStopIndicator = {},
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusText(torrent.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(torrent.status)
                )
                Text(
                    text = FormatUtils.formatProgress(torrent.progress),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (torrent.status == TorrentStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        Text(
                            text = "↓ ${FormatUtils.formatSpeed(torrent.downloadSpeed)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "↑ ${FormatUtils.formatSpeed(torrent.uploadSpeed)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "${torrent.numPeers} peers",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${FormatUtils.formatFileSize(torrent.downloadedBytes)} / ${FormatUtils.formatFileSize(torrent.totalSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (torrent.isPrivate) {
                    Text(
                        text = "Private",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun statusText(status: TorrentStatus): String = when (status) {
    TorrentStatus.CHECKING -> "Checking"
    TorrentStatus.DOWNLOADING -> "Downloading"
    TorrentStatus.SEEDING -> "Seeding"
    TorrentStatus.PAUSED -> "Paused"
    TorrentStatus.STOPPED -> "Stopped"
    TorrentStatus.ERROR -> "Error"
    TorrentStatus.METADATA -> "Getting metadata"
}

@Composable
private fun statusColor(status: TorrentStatus): Color = when (status) {
    TorrentStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
    TorrentStatus.SEEDING -> Color(0xFF4CAF50)
    TorrentStatus.ERROR -> MaterialTheme.colorScheme.error
    TorrentStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
}
