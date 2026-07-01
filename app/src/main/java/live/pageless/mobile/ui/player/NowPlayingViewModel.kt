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
import kotlinx.coroutines.launch
import live.pageless.mobile.core.Chapters
import live.pageless.mobile.data.local.BookmarkEntity
import live.pageless.mobile.data.local.ChapterEntity
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.data.repository.BookmarkRepository
import live.pageless.mobile.data.repository.LibraryRepository
import live.pageless.mobile.data.repository.coverModel
import live.pageless.mobile.playback.PlayerConnection
import live.pageless.mobile.playback.PlayerState
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel
    @Inject
    constructor(
        private val playerConnection: PlayerConnection,
        private val libraryRepository: LibraryRepository,
        private val bookmarkRepository: BookmarkRepository,
        playerSettingsStore: live.pageless.mobile.data.local.PlayerSettingsStore,
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

        val chapters: StateFlow<List<ChapterEntity>> =
            currentBookId
                .flatMapLatest { id ->
                    if (id == null) flowOf(emptyList()) else libraryRepository.observeChapters(id)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val bookmarks: StateFlow<List<BookmarkEntity>> =
            currentBookId
                .flatMapLatest { id ->
                    if (id == null) flowOf(emptyList()) else bookmarkRepository.observeForBook(id)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        /** Cover URL for the current book (reactive to book + server url). */
        val coverUrl: StateFlow<String?> =
            combine(currentBookId, authRepository.serverUrl) { id, serverUrl ->
                id to serverUrl
            }.flatMapLatest { (id, serverUrl) ->
                if (id == null) {
                    flowOf(null)
                } else {
                    libraryRepository.observeBook(id).map { book ->
                        book?.coverModel(serverUrl)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun connect() = playerConnection.connect()

        fun playPause() = playerConnection.playPause()

        fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)

        fun seekBy(deltaMs: Long) = playerConnection.seekBy(deltaMs)

        fun skipForward() = playerConnection.seekBy(settings.value.jumpForwardSeconds * 1000L)

        fun skipBackward() = playerConnection.seekBy(-settings.value.jumpBackwardSeconds * 1000L)

        fun setSpeed(speed: Float) = playerConnection.setSpeed(speed)

        /** Jumps to the start of the previous/next chapter relative to the current position. */
        fun jumpChapter(forward: Boolean) {
            val chs = chapters.value
            if (chs.isEmpty()) return
            val posSec = state.value.positionMs / 1000.0
            val spans =
                chs.map {
                    object : Chapters.Span {
                        override val startSeconds = it.startSeconds
                        override val endSeconds = it.endSeconds
                    }
                }
            val current = Chapters.currentIndex(spans, posSec) ?: 0
            val target = (if (forward) current + 1 else current - 1).coerceIn(0, chs.size - 1)
            playerConnection.seekTo((chs[target].startSeconds * 1000).toLong())
        }

        fun addBookmark(note: String?) {
            val id = state.value.bookId ?: return
            val posSec = state.value.positionMs / 1000.0
            viewModelScope.launch { bookmarkRepository.add(id, posSec, note) }
        }

        fun deleteBookmark(bookmarkId: String) {
            viewModelScope.launch { bookmarkRepository.delete(bookmarkId) }
        }

        /** Index of the chapter under the current playhead, or null. */
        fun currentChapterIndex(): Int? {
            val chs = chapters.value
            if (chs.isEmpty()) return null
            val spans =
                chs.map {
                    object : Chapters.Span {
                        override val startSeconds = it.startSeconds
                        override val endSeconds = it.endSeconds
                    }
                }
            return Chapters.currentIndex(spans, state.value.positionMs / 1000.0)
        }
    }
