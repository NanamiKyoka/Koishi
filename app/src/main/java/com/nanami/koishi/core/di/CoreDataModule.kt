package com.nanami.koishi.core.di

import androidx.room.Room
import com.nanami.koishi.core.data.repository.InMemoryToolRepository
import com.nanami.koishi.core.data.repository.SearchHistoryRepository
import com.nanami.koishi.core.data.repository.SharedPreferencesSearchHistoryRepository
import com.nanami.koishi.core.data.repository.SharedPreferencesToolFavoritesRepository
import com.nanami.koishi.core.data.repository.ToolFavoritesRepository
import com.nanami.koishi.core.data.repository.ToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import com.nanami.koishi.core.data.storage.ToolStorageDatabase
import com.nanami.koishi.core.data.sync.WebDavClient
import com.nanami.koishi.core.data.sync.WebDavSyncManager
import org.koin.dsl.module

val coreDataModule = module {

    single<ToolStorageDatabase> {
        Room.databaseBuilder(
            get(),
            ToolStorageDatabase::class.java,
            ToolStorageDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single<ToolStorageDao> { get<ToolStorageDatabase>().toolStorageDao() }

    single<ToolRepository> { InMemoryToolRepository() }

    single<ToolFavoritesRepository> { SharedPreferencesToolFavoritesRepository(get()) }

    single<SearchHistoryRepository> { SharedPreferencesSearchHistoryRepository(get()) }

    single { WebDavClient() }

    single { WebDavSyncManager(get(), get(), get(), get(), get()) }
}
