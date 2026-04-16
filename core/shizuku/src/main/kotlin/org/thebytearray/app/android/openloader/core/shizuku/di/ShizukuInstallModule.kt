package org.thebytearray.app.android.openloader.core.shizuku.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.thebytearray.app.android.openloader.core.domain.di.ShizukuBackend
import org.thebytearray.app.android.openloader.core.domain.install.InstallBackend
import org.thebytearray.app.android.openloader.core.shizuku.ShizukuInstallBackend
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ShizukuInstallModule {

    @Provides
    @Singleton
    @ShizukuBackend
    fun provideShizukuInstallBackend(backend: ShizukuInstallBackend): InstallBackend = backend
}
