package com.stream.torrent.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val maxDownloadSpeed by viewModel.maxDownloadSpeed.collectAsState()
    val maxUploadSpeed by viewModel.maxUploadSpeed.collectAsState()
    val maxConcurrent by viewModel.maxConcurrentTorrents.collectAsState()
    val isDhtEnabled by viewModel.isDhtEnabled.collectAsState()
    val isEncryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Speed Limits", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (maxDownloadSpeed == 0) "" else maxDownloadSpeed.toString(),
                    onValueChange = {
                        val speed = it.toIntOrNull() ?: 0
                        viewModel.setMaxDownloadSpeed(speed)
                    },
                    label = { Text("Download (KB/s)") },
                    placeholder = { Text("Unlimited") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = if (maxUploadSpeed == 0) "" else maxUploadSpeed.toString(),
                    onValueChange = {
                        val speed = it.toIntOrNull() ?: 0
                        viewModel.setMaxUploadSpeed(speed)
                    },
                    label = { Text("Upload (KB/s)") },
                    placeholder = { Text("Unlimited") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Concurrent Downloads", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = maxConcurrent.toString(),
                onValueChange = {
                    val max = it.toIntOrNull() ?: 3
                    viewModel.setMaxConcurrentTorrents(max.coerceIn(1, 10))
                },
                label = { Text("Max concurrent torrents") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Network", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitch(
                label = "DHT (Distributed Hash Table)",
                description = "Enable peer discovery via DHT. Disabled for private torrents.",
                checked = isDhtEnabled,
                onCheckedChange = { viewModel.setDhtEnabled(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSwitch(
                label = "Encryption",
                description = "Enable protocol encryption for tracker compatibility.",
                checked = isEncryptionEnabled,
                onCheckedChange = { viewModel.setEncryptionEnabled(it) }
            )
        }
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
