package live.pageless.mobile.data.repository

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import live.pageless.mobile.data.download.AudioDownloader
import live.pageless.mobile.data.download.CoverCache
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
        private val audioDownloader: AudioDownloader,
        private val coverCache: CoverCache,
        private val playbackTeardown: PlaybackTeardown,
        private val downloadRepository: DownloadRepository,
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
                            clearLocalContent()
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
                clearLocalContent()
            }
        }

        /**
         * Drops everything the signed-out account left on the device: the Room
         * cache, downloaded `.m4b` files and cached covers.
         *
         * The files matter as much as the rows. Clearing only the database leaves
         * the audio and artwork of the previous account readable by anyone who
         * later signs in on the same device, and unreachable by the app — so the
         * space can never be reclaimed from inside it.
         *
         * Playback is stopped too. Leaving it running would keep the previous
         * account's audiobook audible, with its title, author and cover on the
         * lock screen — and deleting the file does not stop it, because the
         * player already holds an open descriptor.
         *
         * Download notifications go the same way. "Download complete" names the
         * book and is dismissible rather than self-clearing, so it survives
         * sign-out and leaves the previous account's titles in the shade.
         *
         * Downloads still running are cancelled as well. Nothing else stops
         * them: sync work is cancelled by unique name, but downloads are unique
         * work named per book, so only the shared tag reaches them all.
         *
         * Must be called with the [CacheCoordinator] lock already held; its mutex
         * is not reentrant, which is why this uses the lock-free `deleteAllFiles`
         * variants rather than the per-book `delete` methods.
         */
        private suspend fun clearLocalContent() {
            // Before the database, deliberately. PlaybackService saves progress on
            // a timer and skips when the player has no current item, so emptying
            // the queue first is what stops a late save from writing a progress
            // row for the account whose rows are about to be dropped.
            playbackTeardown.stopAndClearPlayback()
            // Before the files are removed, so an in-flight worker cannot write
            // the outgoing account's audio back into the directory just cleared.
            downloadRepository.cancelAll()
            audioDownloader.cancelNotifications()
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                audioDownloader.deleteAllFiles()
                coverCache.deleteAllFiles()
            }
        }
    }
