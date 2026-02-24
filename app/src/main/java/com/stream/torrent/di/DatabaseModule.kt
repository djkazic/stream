package com.stream.torrent.di

import android.content.Context
import androidx.room.Room
import com.stream.torrent.data.local.db.AppDatabase
import com.stream.torrent.data.local.db.TorrentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "stream_database"
        ).build()
    }

    @Provides
    fun provideTorrentDao(database: AppDatabase): TorrentDao {
        return database.torrentDao()
    }
}
