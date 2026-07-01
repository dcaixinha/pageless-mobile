package live.pageless.mobile.data.repository

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import live.pageless.mobile.data.local.PagelessDatabase
import live.pageless.mobile.data.local.SessionStore
import live.pageless.mobile.data.remote.LoginRequest
import live.pageless.mobile.data.remote.MeResponse
import live.pageless.mobile.data.remote.PagelessApi
import live.pageless.mobile.data.sync.SyncScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository
    @Inject
    constructor(
        private val api: PagelessApi,
        private val database: PagelessDatabase,
        private val cacheCoordinator: CacheCoordinator,
        private val sessionStore: SessionStore,
        private val syncScheduler: SyncScheduler,
        private val connectionStatusRepository: ConnectionStatusRepository,
    ) {
        val token: Flow<String?> = sessionStore.token
        val serverUrl: Flow<String> = sessionStore.serverUrl
        val email: Flow<String?> = sessionStore.email
        val ignorePrefixesWhenSorting: Flow<Boolean> = sessionStore.ignorePrefixesWhenSorting
        val dateFormat: Flow<String> = sessionStore.dateFormat
        val timeFormat: Flow<String> = sessionStore.timeFormat
        val displayName: Flow<String?> =
            combine(sessionStore.firstName, sessionStore.email) { firstName, email ->
                firstName?.takeIf { it.isNotBlank() } ?: email
            }

        /**
         * Logs in against [serverUrl] and persists the returned token. The URL is
         * saved first so the [live.pageless.mobile.data.remote.BaseUrlInterceptor]
         * targets the right host for the login call.
         */
        suspend fun login(
            serverUrl: String,
            email: String,
            password: String,
        ): Result<Unit> {
            val result =
                runCatching {
                    cacheCoordinator.exclusive {
                        if (sessionStore.currentServerUrl() != serverUrl.trim()) {
                            withContext(Dispatchers.IO) { database.clearAllTables() }
                        }
                        sessionStore.setServerUrl(serverUrl.trim())
                        val device = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifEmpty { "Android device" }
                        val response = api.login(LoginRequest(email.trim(), password, device))
                        sessionStore.saveSession(
                            response.token,
                            response.user.email,
                            response.user.firstName,
                            serverUrl.trim(),
                            response.user.ignorePrefixesWhenSorting,
                            response.user.dateFormat,
                            response.user.timeFormat,
                        )
                    }
                    syncScheduler.schedulePeriodic()
                    syncScheduler.enqueueNow()
                }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        suspend fun refreshCurrentUser(): Result<MeResponse> =
            runCatching {
                val response = api.me()
                sessionStore.setUserPreferences(
                    response.user.ignorePrefixesWhenSorting,
                    response.user.dateFormat,
                    response.user.timeFormat,
                )
                response
            }

        suspend fun logout() {
            cacheCoordinator.exclusive {
                runCatching { api.logout() }
                syncScheduler.cancelAll()
                sessionStore.clear()
                withContext(Dispatchers.IO) { database.clearAllTables() }
            }
        }
    }
