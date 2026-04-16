package org.thebytearray.app.android.openloader.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.thebytearray.app.android.openloader.core.datastore.AdbKeys
import org.thebytearray.app.android.openloader.core.datastore.UserPreferences
import org.thebytearray.app.android.openloader.core.datastore.openLoaderAdbKeysDataStore
import org.thebytearray.app.android.openloader.core.datastore.openLoaderInstallHistoryDataStore
import org.thebytearray.app.android.openloader.core.datastore.openLoaderUserPreferencesDataStore
import org.thebytearray.app.android.openloader.data.InstallHistory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpenLoaderDataStoreModule {

    @Provides
    @Singleton
    @UserPreferences
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.openLoaderUserPreferencesDataStore

    @Provides
    @Singleton
    @AdbKeys
    fun provideAdbKeysDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.openLoaderAdbKeysDataStore

    @Provides
    @Singleton
    fun provideInstallHistoryDataStore(
        @ApplicationContext context: Context,
    ): DataStore<InstallHistory> = context.openLoaderInstallHistoryDataStore
}
