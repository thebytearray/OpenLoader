package org.thebytearray.app.android.openloader.core.common.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.thebytearray.app.android.openloader.core.common.network.Dispatcher
import org.thebytearray.app.android.openloader.core.common.network.OpenLoaderDispatchers.Default
import org.thebytearray.app.android.openloader.core.common.network.OpenLoaderDispatchers.IO

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Dispatcher(IO)
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
