package live.pageless.mobile.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import live.pageless.mobile.MainActivity
import live.pageless.mobile.R
import live.pageless.mobile.data.local.PlayerSettingsStore
import live.pageless.mobile.data.repository.PlaybackHistoryRepository
import live.pageless.mobile.data.repository.PlaybackSessionStart
import live.pageless.mobile.data.repository.ProgressRepository
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Foreground media service hosting the ExoPlayer + MediaSession. Media3 provides
 * the notification and lock-screen controls (play/pause/seek) automatically from
 * the session, and keeps playback alive in the background via the foreground
 * service declared in the manifest.
 *
 * The service periodically persists the current position to
 * [ProgressRepository] (offline-first, marked dirty) so progress survives app
 * death and later syncs to the server.
 */
@OptIn(markerClass = [UnstableApi::class])
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var progressRepository: ProgressRepository

    @Inject lateinit var playbackHistoryRepository: PlaybackHistoryRepository

    // Shared authenticated OkHttp client so ExoPlayer's HTTP requests carry the
    // bearer token (streaming) via the same interceptors as the API client.
    @Inject lateinit var okHttpClient: OkHttpClient

    @Inject lateinit var settingsStore: PlayerSettingsStore

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    // Whether external controllers (media notification / lock screen) may seek.
    @Volatile private var allowSeekFromNotification = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var saveJob: Job? = null
    private var lastServerSyncAttemptMs = 0L
    private var currentSessionId: String? = null
    private var currentSessionBookId: String? = null
    private var listeningStartedAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        // Route HTTP(S) through OkHttp (auth + base-URL interceptors) and fall
        // back to the platform resolvers for local file:// URIs.
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setHandleAudioBecomingNoisy(true)
                .build()

        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        startSaving()
                    } else {
                        recordPauseEvent()
                        stopSaving()
                        saveNow(forceSync = true)
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: PositionInfo,
                    newPosition: PositionInfo,
                    reason: Int,
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) recordSeekEvent()
                    saveNow()
                }
            },
        )

        // Tapping the notification (or lock-screen media controls) opens the app.
        // Media3 uses the session activity as the notification's content intent.
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        mediaSession =
            MediaSession
                .Builder(this, player)
                .setSessionActivity(contentIntent)
                .setCallback(SessionCallback())
                .build()

        // Brand the media notification's status-bar icon.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider
                .Builder(this)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat_pageless) },
        )

        // Keep the notification/lock-screen scrub capability in sync with the
        // user's "Allow position seeking on media notification controls" setting.
        // When the value changes we re-grant the (possibly reduced) command set
        // to every connected external controller, which shows/hides the scrub bar.
        scope.launch {
            settingsStore.settings
                .map { it.allowSeekFromNotification }
                .distinctUntilChanged()
                .collect { allow ->
                    allowSeekFromNotification = allow
                    val session = mediaSession ?: return@collect
                    session.connectedControllers.forEach { controller ->
                        session.setAvailableCommands(
                            controller,
                            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                            playerCommandsFor(controller, allow),
                        )
                    }
                }
        }
    }

    /**
     * Player commands granted to external controllers (media notification, lock
     * screen, Android Auto, etc). When notification seeking is disabled we strip
     * the seek-within-item commands so no scrub bar is shown, while keeping
     * play/pause and next/previous-item transport.
     */
    private fun playerCommandsFor(
        controller: MediaSession.ControllerInfo,
        allowSeek: Boolean,
    ): Player.Commands {
        val builder = Player.Commands.Builder().addAllCommands()
        if (!allowSeek && controller.packageName != packageName) {
            builder.remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            builder.remove(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
        }
        return builder.build()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult
                .AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(playerCommandsFor(controller, allowSeekFromNotification))
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        recordStopEvent()
        saveNow(forceSync = true)
        // If the user swipes the app away while paused, stop the service.
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        recordStopEvent()
        saveBlocking()
        stopSaving()
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun startSaving() {
        if (saveJob?.isActive == true) return
        recordPlayEvent()
        saveJob =
            scope.launch {
                while (isActive) {
                    delay(SAVE_INTERVAL_MS)
                    saveNow()
                }
            }
    }

    private fun stopSaving() {
        saveJob?.cancel()
        saveJob = null
    }

    private fun saveNow(forceSync: Boolean = false) {
        if (player.currentMediaItem
                ?.mediaMetadata
                ?.extras
                ?.getBoolean(PlayerConnection.EXTRA_PREVIEW) == true
        ) {
            return
        }
        val bookId = player.currentMediaItem?.mediaId ?: return
        val positionSec = player.currentPosition / 1000.0
        val durationSec = player.duration.takeIf { it > 0 }?.div(1000.0) ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                progressRepository.record(bookId, positionSec, durationSec)
                val syncResult = syncIfDue(forceSync)
                val attempted = syncResult != null
                recordHistoryEvent(
                    event = "Save",
                    positionSeconds = positionSec,
                    serverSyncAttempted = attempted,
                    serverSyncSuccess = syncResult?.isSuccess,
                    serverSyncMessage = syncResult?.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun saveBlocking() {
        if (player.currentMediaItem
                ?.mediaMetadata
                ?.extras
                ?.getBoolean(PlayerConnection.EXTRA_PREVIEW) == true
        ) {
            return
        }
        val bookId = player.currentMediaItem?.mediaId ?: return
        val positionSec = player.currentPosition / 1000.0
        val durationSec = player.duration.takeIf { it > 0 }?.div(1000.0) ?: return
        runBlocking(Dispatchers.IO) {
            progressRepository.record(bookId, positionSec, durationSec)
            val syncResult = progressRepository.sync()
            recordHistoryEvent(
                event = "Save",
                positionSeconds = positionSec,
                serverSyncAttempted = true,
                serverSyncSuccess = syncResult.isSuccess,
                serverSyncMessage = syncResult.exceptionOrNull()?.message,
            )
        }
    }

    private suspend fun syncIfDue(force: Boolean): Result<Unit>? {
        val now = System.currentTimeMillis()
        if (!force && now - lastServerSyncAttemptMs < ACTIVE_SYNC_INTERVAL_MS) return null
        lastServerSyncAttemptMs = now
        return progressRepository.sync()
    }

    private fun recordPlayEvent() {
        if (player.currentMediaItem
                ?.mediaMetadata
                ?.extras
                ?.getBoolean(PlayerConnection.EXTRA_PREVIEW) == true
        ) {
            return
        }
        val bookId = player.currentMediaItem?.mediaId ?: return
        val durationSec = player.duration.takeIf { it > 0 }?.div(1000.0) ?: return
        val positionSec = player.currentPosition / 1000.0
        val meta = player.currentMediaItem?.mediaMetadata
        val playMethod = playMethod()

        scope.launch(Dispatchers.IO) {
            if (currentSessionId == null || currentSessionBookId != bookId) {
                currentSessionId =
                    playbackHistoryRepository.startSession(
                        PlaybackSessionStart(
                            bookId = bookId,
                            title = meta?.title?.toString(),
                            authors = meta?.artist?.toString(),
                            playMethod = playMethod,
                            positionSeconds = positionSec,
                            durationSeconds = durationSec,
                        ),
                    )
                currentSessionBookId = bookId
            } else {
                recordHistoryEvent("Play", positionSeconds = positionSec)
            }
            listeningStartedAtMs = SystemClock.elapsedRealtime()
        }
    }

    private fun recordPauseEvent() {
        if (player.currentMediaItem
                ?.mediaMetadata
                ?.extras
                ?.getBoolean(PlayerConnection.EXTRA_PREVIEW) == true
        ) {
            return
        }
        val startedAt = listeningStartedAtMs.takeIf { it > 0 } ?: return
        val sessionId = currentSessionId ?: return
        val bookId = currentSessionBookId ?: return
        val positionSec = player.currentPosition / 1000.0
        val listenedSec = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0) / 1000
        listeningStartedAtMs = 0L
        scope.launch(Dispatchers.IO) {
            playbackHistoryRepository.addListeningTime(sessionId, listenedSec, positionSec)
            playbackHistoryRepository.recordEvent(sessionId, bookId, "Pause", positionSec)
        }
    }

    private fun recordSeekEvent() {
        recordHistoryEvent("Seek")
    }

    private fun recordStopEvent() {
        recordPauseEvent()
        val sessionId = currentSessionId ?: return
        val bookId = currentSessionBookId ?: return
        val positionSec = player.currentPosition / 1000.0
        scope.launch(Dispatchers.IO) {
            playbackHistoryRepository.recordEvent(sessionId, bookId, "Stop", positionSec)
            playbackHistoryRepository.endSession(sessionId, positionSec)
        }
        currentSessionId = null
        currentSessionBookId = null
    }

    private fun recordHistoryEvent(
        event: String,
        positionSeconds: Double = player.currentPosition / 1000.0,
        serverSyncAttempted: Boolean = false,
        serverSyncSuccess: Boolean? = null,
        serverSyncMessage: String? = null,
    ) {
        val sessionId = currentSessionId ?: return
        val bookId = currentSessionBookId ?: return
        scope.launch(Dispatchers.IO) {
            playbackHistoryRepository.recordEvent(
                sessionId = sessionId,
                bookId = bookId,
                event = event,
                positionSeconds = positionSeconds,
                serverSyncAttempted = serverSyncAttempted,
                serverSyncSuccess = serverSyncSuccess,
                serverSyncMessage = serverSyncMessage,
            )
        }
    }

    private fun playMethod(): String {
        val uri = player.currentMediaItem?.localConfiguration?.uri
        return if (uri?.scheme == "file") "Local" else "Direct Play"
    }

    companion object {
        private const val SAVE_INTERVAL_MS = 10_000L
        private const val ACTIVE_SYNC_INTERVAL_MS = 60_000L
    }
}
