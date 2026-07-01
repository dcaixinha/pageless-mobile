package live.pageless.mobile.ui.book

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.pageless.mobile.data.local.PlayerSettingsStore
import live.pageless.mobile.data.local.SessionStore
import live.pageless.mobile.data.remote.bookDownloadUrl
import live.pageless.mobile.data.repository.DownloadRepository
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

data class BookmarkPreviewState(
    val loading: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val jumpBackwardSeconds: Int = 15,
    val jumpForwardSeconds: Int = 30,
    val bookmarkContextSeconds: Int = 0,
    val error: String? = null,
)

@OptIn(markerClass = [UnstableApi::class])
@HiltViewModel
class BookmarkPreviewViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val downloadRepository: DownloadRepository,
        private val playerSettingsStore: PlayerSettingsStore,
        private val sessionStore: SessionStore,
        private val okHttpClient: OkHttpClient,
    ) : ViewModel() {
        private val _state = MutableStateFlow(BookmarkPreviewState())
        val state: StateFlow<BookmarkPreviewState> = _state.asStateFlow()

        private var player: ExoPlayer? = null
        private var preparedKey: Pair<String, Long>? = null
        private var tickerJob: Job? = null

        init {
            viewModelScope.launch {
                playerSettingsStore.settings.collect { settings ->
                    _state.update {
                        it.copy(
                            jumpBackwardSeconds = settings.jumpBackwardSeconds,
                            jumpForwardSeconds = settings.jumpForwardSeconds,
                            bookmarkContextSeconds = settings.bookmarkContextSeconds,
                        )
                    }
                }
            }
        }

        fun prepare(
            bookId: String,
            startPositionMs: Long,
        ) {
            val key = bookId to startPositionMs
            if (preparedKey == key && player != null) return

            releasePlayer()
            _state.update {
                BookmarkPreviewState(
                    loading = true,
                    positionMs = startPositionMs,
                    jumpBackwardSeconds = it.jumpBackwardSeconds,
                    jumpForwardSeconds = it.jumpForwardSeconds,
                    bookmarkContextSeconds = it.bookmarkContextSeconds,
                )
            }

            viewModelScope.launch {
                runCatching {
                    val local = downloadRepository.localPathIfComplete(bookId)
                    val uri =
                        if (local != null) {
                            Uri.fromFile(File(local))
                        } else {
                            Uri.parse(bookDownloadUrl(sessionStore.currentServerUrl(), bookId))
                        }

                    val dataSourceFactory =
                        DefaultDataSource.Factory(
                            context,
                            OkHttpDataSource.Factory(okHttpClient),
                        )
                    ExoPlayer
                        .Builder(context)
                        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                        .build()
                        .also { previewPlayer ->
                            player = previewPlayer
                            preparedKey = key
                            previewPlayer.addListener(listener)
                            previewPlayer.setMediaItem(MediaItem.fromUri(uri), startPositionMs)
                            previewPlayer.prepare()
                            startTicker()
                        }
                }.onFailure { error ->
                    _state.update {
                        BookmarkPreviewState(
                            jumpBackwardSeconds = it.jumpBackwardSeconds,
                            jumpForwardSeconds = it.jumpForwardSeconds,
                            bookmarkContextSeconds = it.bookmarkContextSeconds,
                            error = error.message ?: "Preview failed",
                        )
                    }
                }
            }
        }

        fun playPause() {
            val p = player ?: return
            if (p.isPlaying) p.pause() else p.play()
        }

        fun seekBy(deltaMs: Long) {
            val p = player ?: return
            val duration = p.duration.takeIf { it > 0 } ?: state.value.durationMs
            val target = (p.currentPosition + deltaMs).coerceIn(0, duration.coerceAtLeast(0))
            p.seekTo(target)
        }

        fun stop() {
            releasePlayer()
            _state.update {
                BookmarkPreviewState(
                    jumpBackwardSeconds = it.jumpBackwardSeconds,
                    jumpForwardSeconds = it.jumpForwardSeconds,
                    bookmarkContextSeconds = it.bookmarkContextSeconds,
                )
            }
        }

        override fun onCleared() {
            releasePlayer()
            super.onCleared()
        }

        private val listener =
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) = updateState(player)

                override fun onPlayerError(error: PlaybackException) {
                    _state.update { it.copy(loading = false, error = error.message) }
                }
            }

        private fun startTicker() {
            tickerJob?.cancel()
            tickerJob =
                viewModelScope.launch {
                    while (true) {
                        player?.let { updateState(it) }
                        delay(500)
                    }
                }
        }

        private fun updateState(player: Player) {
            _state.update {
                it.copy(
                    loading = player.playbackState == Player.STATE_BUFFERING,
                    isPlaying = player.isPlaying,
                    positionMs = player.currentPosition.coerceAtLeast(0),
                    durationMs = player.duration.takeIf { duration -> duration > 0 } ?: it.durationMs,
                    error = null,
                )
            }
        }

        private fun releasePlayer() {
            tickerJob?.cancel()
            tickerJob = null
            player?.removeListener(listener)
            player?.stop()
            player?.clearMediaItems()
            player?.release()
            player = null
            preparedKey = null
        }
    }
