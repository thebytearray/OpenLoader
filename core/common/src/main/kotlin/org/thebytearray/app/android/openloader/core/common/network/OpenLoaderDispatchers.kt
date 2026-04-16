package org.thebytearray.app.android.openloader.core.common.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val openLoaderDispatcher: OpenLoaderDispatchers)

enum class OpenLoaderDispatchers {
    Default,
    IO,
}
