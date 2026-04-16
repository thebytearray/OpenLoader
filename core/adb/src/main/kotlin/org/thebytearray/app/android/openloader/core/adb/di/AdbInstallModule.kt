package org.thebytearray.app.android.openloader.core.adb.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.thebytearray.app.android.openloader.core.adb.AdbInstallBackend
import org.thebytearray.app.android.openloader.core.domain.di.AdbBackend
import org.thebytearray.app.android.openloader.core.domain.install.InstallBackend
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdbInstallModule {

    @Provides
    @Singleton
    @AdbBackend
    fun provideAdbInstallBackend(backend: AdbInstallBackend): InstallBackend = backend
}
