package live.pageless.mobile.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheCoordinator
    @Inject
    constructor() {
        private val mutex = Mutex()

        suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }
    }
