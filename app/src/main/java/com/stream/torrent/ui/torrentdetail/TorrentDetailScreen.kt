package com.stream.torrent.ui.torrentdetail

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stream.torrent.domain.model.TrackerInfo
import com.stream.torrent.domain.model.TorrentFileItem
import com.stream.torrent.domain.model.TorrentModel
import com.stream.torrent.domain.model.TorrentStatus
import com.stream.torrent.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailScreen(
    infoHash: String,
    onPlayFile: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: TorrentDetailViewModel = hiltViewModel()
) {
    val torrent by viewModel.torrent.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = torrent?.name ?: "Loading...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    torrent?.let { t ->
                        if (t.status == TorrentStatus.PAUSED) {
                            IconButton(onClick = { viewModel.resumeTorrent() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                            }
                        } else {
                            IconButton(onClick = { viewModel.pauseTorrent() }) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        val t = torrent
        if (t == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { viewModel.selectTab(0) },
                        text = { Text("Files") })
                    Tab(selected = selectedTab == 1, onClick = { viewModel.selectTab(1) },
                        text = { Text("Info") })
                    Tab(selected = selectedTab == 2, onClick = { viewModel.selectTab(2) },
                        text = { Text("Peers") })
                    Tab(selected = selectedTab == 3, onClick = { viewModel.selectTab(3) },
                        text = { Text("Trackers") })
                }

                when (selectedTab) {
                    0 -> FilesTab(torrent = t, onPlayFile = onPlayFile)
                    1 -> InfoTab(torrent = t)
                    2 -> PeersTab(torrent = t)
                    3 -> TrackersTab(torrent = t)
                }
            }
        }
    }
}

@Composable
private fun FilesTab(torrent: TorrentModel, onPlayFile: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(torrent.files) { file ->
            FileItem(file = file, onPlay = { onPlayFile(file.index) })
        }
    }
}

@Composable
private fun FileItem(file: TorrentFileItem, onPlay: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (file.isMedia) Modifier.clickable(onClick = onPlay) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (file.isMedia) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { file.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FormatUtils.formatFileSize(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatUtils.formatProgress(file.progress),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun InfoTab(torrent: TorrentModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { InfoRow("Info Hash", torrent.infoHash) }
        item { InfoRow("Total Size", FormatUtils.formatFileSize(torrent.totalSize)) }
        item { InfoRow("Downloaded", FormatUtils.formatFileSize(torrent.downloadedBytes)) }
        item { InfoRow("Uploaded", FormatUtils.formatFileSize(torrent.uploadedBytes)) }
        item { InfoRow("Progress", FormatUtils.formatProgress(torrent.progress)) }
        item { InfoRow("Save Path", torrent.savePath) }
        if (torrent.isPrivate) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Private Torrent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PeersTab(torrent: TorrentModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${torrent.numPeers}",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Connected Peers",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${torrent.numSeeds} seeds",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrackersTab(torrent: TorrentModel) {
    if (torrent.trackers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No trackers",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(torrent.trackers) { tracker ->
                TrackerItem(tracker = tracker)
            }
        }
    }
}

@Composable
private fun TrackerItem(tracker: TrackerInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = tracker.url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tracker.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (tracker.status) {
                        "Working" -> MaterialTheme.colorScheme.primary
                        "Failed" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (tracker.message.isNotEmpty()) {
                    Text(
                        text = tracker.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
