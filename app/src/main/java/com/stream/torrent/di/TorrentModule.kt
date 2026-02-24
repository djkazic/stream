package com.stream.torrent.di

import com.stream.torrent.data.repository.TorrentRepository
import com.stream.torrent.data.repository.TorrentRepositoryImpl
import com.stream.torrent.engine.TorrentEngine
import com.stream.torrent.engine.TorrentEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TorrentModule {

    @Binds
    @Singleton
    abstract fun bindTorrentEngine(impl: TorrentEngineImpl): TorrentEngine

    @Binds
    @Singleton
    abstract fun bindTorrentRepository(impl: TorrentRepositoryImpl): TorrentRepository
}
