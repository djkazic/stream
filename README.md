# Stream

An Android app for streaming media from torrent files. Opens `.torrent` files, downloads their contents, and plays media inline using ExoPlayer — no need to wait for a full download.

## Features

- **Torrent streaming** — Play video/audio while pieces are still downloading via a custom `TorrentDataSource` feeding directly into ExoPlayer
- **Private tracker support** — Respects the `private` flag (disables DHT/PEX/LSD), preserves passkeys in announce URLs, enables encryption
- **Foreground service** — Downloads continue in the background with a persistent notification
- **File browser** — Browse files within a torrent, see per-file progress, tap media files to play
- **Torrent management** — Add, pause, resume, and remove torrents with swipe-to-delete

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Torrent engine | libtorrent4j 2.1.0-38 |
| Media playback | AndroidX Media3 (ExoPlayer) |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Architecture | MVVM |

## Requirements

- Android 11+ (API 30)
- Target SDK 35

## Building

```
./gradlew assembleDebug
```

To install on a connected device or emulator:

```
./gradlew installDebug
```

## Project Structure

```
app/src/main/java/com/stream/torrent/
├── StreamApp.kt                # Application class
├── MainActivity.kt             # Single activity, navigation host
├── di/                         # Hilt modules
├── data/
│   ├── local/db/               # Room database, DAO, entities
│   ├── local/preferences/      # DataStore preferences
│   └── repository/             # TorrentRepository
├── domain/
│   ├── model/                  # Domain models
│   └── usecase/                # Use cases
├── engine/                     # libtorrent4j wrapper, piece priority manager
├── player/                     # TorrentDataSource, PlayerManager
├── service/                    # Foreground download service
├── ui/
│   ├── navigation/             # Nav graph, screen routes
│   ├── theme/                  # Material 3 theme
│   ├── torrentlist/            # Torrent list screen
│   ├── torrentdetail/          # Detail screen (files, info, peers, trackers)
│   ├── player/                 # Player screen
│   ├── addtorrent/             # Add torrent bottom sheet
│   └── settings/               # Settings screen
└── util/                       # File, format, and MIME utilities
```

## How Streaming Works

1. User selects a media file within a torrent
2. The engine enables sequential download and prioritizes the first and last pieces
3. `TorrentDataSource` implements ExoPlayer's `DataSource` interface, reading directly from the partially-downloaded file on disk and blocking when pieces aren't yet available
4. `PiecePriorityManager` maintains a rolling priority window that advances as playback progresses
