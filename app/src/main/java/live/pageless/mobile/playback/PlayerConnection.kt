package live.pageless.mobile.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.pageless.mobile.data.local.SessionStore
import live.pageless.mobile.data.remote.bookDownloadUrl
import live.pageless.mobile.data.repository.DownloadRepository
import live.pageless.mobile.data.repository.LibraryRepository
import live.pageless.mobile.data.repository.ProgressRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** Snapshot of the player's observable state for the UI. */
data class PlayerState(
    val bookId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1.0f,
    val isPreview: Boolean = false,
) {
    /** True when a book is loaded in the player. */
    val hasContent: Boolean get() = bookId != null
}

/**
 * App-facing handle to the [PlaybackService], connecting via a [MediaController].
 * Exposes state as a flow and offers play/pause/seek/speed commands. Chooses a
 * local downloaded file when available, otherwise streams from the server.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Singleton
class PlayerConnection
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val downloadRepository: DownloadRepository,
        private val libraryRepository: LibraryRepository,
        private val progressRepository: ProgressRepository,
        private val sessionStore: SessionStore,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        private val _state = MutableStateFlow(PlayerState())
        val state: StateFlow<PlayerState> = _state.asStateFlow()

        private var controller: MediaController? = null

        private val listener =
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    val meta = player.currentMediaItem?.mediaMetadata
                    _state.update {
                        it.copy(
                            bookId = player.currentMediaItem?.mediaId,
                            title = meta?.title?.toString(),
                            author = meta?.artist?.toString(),
                            isPlaying = player.isPlaying,
                            positionMs = player.currentPosition,
                            durationMs = player.duration.takeIf { d -> d > 0 } ?: 0,
                            speed = player.playbackParameters.speed,
                            isPreview = meta?.extras?.getBoolean(EXTRA_PREVIEW) == true,
                        )
                    }
                }
            }

        fun connect() {
            if (controller != null) return
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                controller = future.get().also { it.addListener(listener) }
                startPositionTicker()
                startProgressObserver()
            }, MoreExecutors.directExecutor())
        }

        fun release() {
            controller?.removeListener(listener)
            controller?.release()
            controller = null
        }

        /**
         * Loads and starts a book. Uses the downloaded file if present; otherwise
         * the authenticated stream URL. Resumes from saved progress.
         */
        fun play(
            bookId: String,
            title: String,
            author: String?,
            startPositionMs: Long? = null,
            preview: Boolean = false,
        ) {
            val c = controller ?: return

            if (!preview && c.currentMediaItem?.mediaId == bookId && c.playbackState != Player.STATE_IDLE) {
                if (startPositionMs != null) c.seekTo(startPositionMs.coerceAtLeast(0))
                c.playWhenReady = true
                c.play()
                return
            }

            scope.launch {
                if (preview) {
                    saveCurrentProgressBeforePreview(c)
                }

                val local = downloadRepository.localPathIfComplete(bookId)
                val uri =
                    if (local != null) {
                        Uri.fromFile(File(local))
                    } else {
                        Uri.parse(bookDownloadUrl(sessionStore.currentServerUrl(), bookId))
                    }

                val resumeMs =
                    startPositionMs
                        ?: (progressRepository.resumePositionSeconds(bookId) * 1000).toLong()

                val book = libraryRepository.getBook(bookId)
                val artworkUri =
                    book
                        ?.coverLocalPath
                        ?.takeIf { book.hasCover && book.coverUpdatedAt == book.updatedAt && File(it).exists() }
                        ?.let { Uri.fromFile(File(it)) }

                val item =
                    MediaItem
                        .Builder()
                        .setMediaId(bookId)
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata
                                .Builder()
                                .setTitle(title)
                                .setArtist(author)
                                .setArtworkUri(artworkUri)
                                .setExtras(Bundle().apply { putBoolean(EXTRA_PREVIEW, preview) })
                                .build(),
                        ).build()

                // Provide the start position atomically with the item so it is
                // applied when the item is prepared. A separate seekTo() after
                // prepare() can race and be dropped before the timeline is ready.
                if (resumeMs > 0) {
                    c.setMediaItem(item, resumeMs)
                } else {
                    c.setMediaItem(item)
                }

                c.prepare()
                c.playWhenReady = true
            }
        }

        /**
         * Pre-prepare a book while viewing its detail screen so tapping Resume can
         * reuse a READY player instead of doing a cold seek.
         * This is intentionally conservative: it never replaces active/other loaded
         * playback and it does not start playback.
         */
        fun preload(
            bookId: String,
            title: String,
            author: String?,
        ) {
            scope.launch {
                var c = controller
                var waitedMs = 0L
                while (c == null && waitedMs < 1_000L) {
                    connect()
                    delay(50)
                    waitedMs += 50
                    c = controller
                }

                c ?: run {
                    return@launch
                }

                if (c.isPlaying || state.value.isPreview) {
                    return@launch
                }

                val loadedBookId = c.currentMediaItem?.mediaId
                if (loadedBookId == bookId && c.playbackState != Player.STATE_IDLE) {
                    return@launch
                }
                if (loadedBookId != null && loadedBookId != bookId) {
                    return@launch
                }

                val local = downloadRepository.localPathIfComplete(bookId)
                val uri =
                    if (local != null) {
                        Uri.fromFile(File(local))
                    } else {
                        Uri.parse(bookDownloadUrl(sessionStore.currentServerUrl(), bookId))
                    }
                val resumeMs = (progressRepository.resumePositionSeconds(bookId) * 1000).toLong()
                val book = libraryRepository.getBook(bookId)
                val artworkUri =
                    book
                        ?.coverLocalPath
                        ?.takeIf { book.hasCover && book.coverUpdatedAt == book.updatedAt && File(it).exists() }
                        ?.let { Uri.fromFile(File(it)) }

                val item =
                    MediaItem
                        .Builder()
                        .setMediaId(bookId)
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata
                                .Builder()
                                .setTitle(title)
                                .setArtist(author)
                                .setArtworkUri(artworkUri)
                                .setExtras(Bundle().apply { putBoolean(EXTRA_PREVIEW, false) })
                                .build(),
                        ).build()

                if (resumeMs > 0) c.setMediaItem(item, resumeMs) else c.setMediaItem(item)
                c.playWhenReady = false
                c.prepare()
            }
        }

        fun playPause() {
            val c = controller ?: return
            if (c.isPlaying) c.pause() else c.play()
        }

        fun seekTo(positionMs: Long) {
            controller?.seekTo(positionMs.coerceAtLeast(0))
        }

        fun seekBy(deltaMs: Long) {
            val c = controller ?: return
            val duration = c.duration.takeIf { it > 0 } ?: state.value.durationMs
            val position = c.currentPosition.takeIf { it > 0 } ?: state.value.positionMs
            c.seekTo((position + deltaMs).coerceIn(0, duration.coerceAtLeast(0)))
        }

        fun setSpeed(speed: Float) {
            controller?.setPlaybackSpeed(speed)
        }

        private suspend fun saveCurrentProgressBeforePreview(controller: MediaController) {
            val currentBookId = controller.currentMediaItem?.mediaId ?: return
            val duration = controller.duration.takeIf { it > 0 } ?: return
            progressRepository.record(
                currentBookId,
                controller.currentPosition / 1000.0,
                duration / 1000.0,
            )
        }

        private fun startPositionTicker() {
            scope.launch {
                while (true) {
                    val c = controller
                    if (c != null && c.isPlaying) {
                        _state.update {
                            it.copy(
                                positionMs = c.currentPosition,
                                durationMs = c.duration.takeIf { d -> d > 0 } ?: it.durationMs,
                                isPreview =
                                    c.currentMediaItem
                                        ?.mediaMetadata
                                        ?.extras
                                        ?.getBoolean(EXTRA_PREVIEW) == true,
                            )
                        }
                    }
                    delay(500)
                }
            }
        }

        private fun startProgressObserver() {
            scope.launch {
                state
                    .map { it.bookId }
                    .distinctUntilChanged()
                    .flatMapLatest { bookId ->
                        if (bookId == null) flowOf(null) else progressRepository.observe(bookId)
                    }.distinctUntilChangedBy { progress ->
                        progress?.let { "${it.bookId}:${it.currentSeconds}:${it.lastPlayedAt}:${it.dirty}" }
                    }.collect { progress ->
                        val c = controller ?: return@collect
                        if (progress == null || progress.dirty || progress.finished || c.isPlaying || state.value.isPreview) return@collect
                        if (c.currentMediaItem?.mediaId != progress.bookId) return@collect

                        val targetMs = (progress.currentSeconds * 1000).toLong().coerceAtLeast(0)
                        if (abs(c.currentPosition - targetMs) >= REMOTE_PROGRESS_SEEK_THRESHOLD_MS) {
                            c.seekTo(targetMs)
                        }
                    }
            }
        }

        companion object {
            const val EXTRA_PREVIEW = "live.pageless.mobile.PREVIEW"
            private const val REMOTE_PROGRESS_SEEK_THRESHOLD_MS = 1_000L
        }
    }
