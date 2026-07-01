package live.pageless.mobile.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import live.pageless.mobile.core.Chapters
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.data.repository.LibraryRepository
import live.pageless.mobile.data.repository.coverModel
import live.pageless.mobile.playback.PlayerConnection
import live.pageless.mobile.playback.PlayerState
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MiniPlayerViewModel
    @Inject
    constructor(
        private val playerConnection: PlayerConnection,
        private val libraryRepository: LibraryRepository,
        private val playerSettingsStore: live.pageless.mobile.data.local.PlayerSettingsStore,
        authRepository: AuthRepository,
    ) : ViewModel() {
        val state: StateFlow<PlayerState> = playerConnection.state

        val settings: StateFlow<live.pageless.mobile.data.local.PlayerSettings> =
            playerSettingsStore.settings
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    live.pageless.mobile.data.local
                        .PlayerSettings(),
                )

        private val currentBookId: Flow<String?> =
            playerConnection.state.map { it.bookId }.distinctUntilChanged()

        val coverUrl: StateFlow<String?> =
            combine(currentBookId, authRepository.serverUrl) { id, serverUrl -> id to serverUrl }
                .flatMapLatest { (id, serverUrl) ->
                    if (id == null) {
                        flowOf(null)
                    } else {
                        libraryRepository.observeBook(id).map { book ->
                            book?.coverModel(serverUrl)
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        /** Chapters for the current book, used to show the current chapter subtitle. */
        private val chapters: Flow<List<live.pageless.mobile.data.local.ChapterEntity>> =
            currentBookId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else libraryRepository.observeChapters(id)
            }

        /**
         * Current chapter title, recomputed as the playhead moves. Exposed as a
         * StateFlow so the mini-player recomposes when the chapter changes.
         */
        val currentChapterTitle: StateFlow<String?> =
            combine(chapters, playerConnection.state.map { it.positionMs }.distinctUntilChanged()) { chs, positionMs ->
                if (chs.isEmpty()) return@combine null
                val spans =
                    chs.map {
                        object : Chapters.Span {
                            override val startSeconds = it.startSeconds
                            override val endSeconds = it.endSeconds
                        }
                    }
                val idx = Chapters.currentIndex(spans, positionMs / 1000.0) ?: return@combine null
                val ch = chs[idx]
                ch.title ?: "Chapter ${ch.index + 1}"
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        /**
         * The progress window the mini-player bar should show: chapter bounds when
         * "use chapter track" is on and a chapter is active, else the whole book
         * (0..duration). Emits (startMs, endMs).
         */
        val progressWindow: StateFlow<Pair<Long, Long>> =
            combine(
                chapters,
                playerConnection.state,
                settings,
            ) { chs, playerState, cfg ->
                val duration = playerState.durationMs
                if (cfg.useChapterTrack && chs.isNotEmpty()) {
                    val spans =
                        chs.map {
                            object : Chapters.Span {
                                override val startSeconds = it.startSeconds
                                override val endSeconds = it.endSeconds
                            }
                        }
                    val idx = Chapters.currentIndex(spans, playerState.positionMs / 1000.0)
                    if (idx != null) {
                        val ch = chs[idx]
                        return@combine (ch.startSeconds * 1000).toLong() to (ch.endSeconds * 1000).toLong()
                    }
                }
                0L to duration
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L to 0L)

        fun connect() = playerConnection.connect()

        fun playPause() = playerConnection.playPause()

        fun skipForward() = playerConnection.seekBy(settings.value.jumpForwardSeconds * 1000L)

        fun skipBackward() = playerConnection.seekBy(-settings.value.jumpBackwardSeconds * 1000L)
    }
