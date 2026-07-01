package live.pageless.mobile.ui.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.pageless.mobile.core.Chapters
import live.pageless.mobile.core.DateTimeFormat
import live.pageless.mobile.data.local.BookEntity
import live.pageless.mobile.data.local.BookmarkEntity
import live.pageless.mobile.data.local.ChapterEntity
import live.pageless.mobile.data.local.PlayerSettings
import live.pageless.mobile.data.local.PlayerSettingsStore
import live.pageless.mobile.data.local.ProgressEntity
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.data.repository.BookmarkRepository
import live.pageless.mobile.data.repository.CollectionRepository
import live.pageless.mobile.data.repository.DownloadRepository
import live.pageless.mobile.data.repository.DownloadStatus
import live.pageless.mobile.data.repository.LibraryRepository
import live.pageless.mobile.data.repository.PlaylistRepository
import live.pageless.mobile.data.repository.coverModel
import live.pageless.mobile.playback.PlayerConnection
import live.pageless.mobile.playback.PlayerState
import live.pageless.mobile.ui.library.LibraryFilterCategory
import javax.inject.Inject

data class BookDetailUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

/** A series this book belongs to, for the detail screen's tappable Series row. */
data class BookSeriesRef(
    val id: String,
    val name: String,
    val sequence: String?,
)

data class BookMetadataLink(
    val id: String,
    val name: String,
)

@HiltViewModel
class BookDetailViewModel
    @Inject
    constructor(
        private val libraryRepository: LibraryRepository,
        private val downloadRepository: DownloadRepository,
        private val bookmarkRepository: BookmarkRepository,
        private val collectionRepository: CollectionRepository,
        private val playlistRepository: PlaylistRepository,
        private val playerConnection: PlayerConnection,
        playerSettingsStore: PlayerSettingsStore,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val bookId: String = checkNotNull(savedStateHandle["bookId"])

        val bookmarks: StateFlow<List<BookmarkEntity>> =
            bookmarkRepository
                .observeForBook(bookId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        /** Absolute cover URL when the book has a cover, else null (reactive to book + server). */
        val coverUrl: StateFlow<String?> =
            combine(libraryRepository.observeBook(bookId), authRepository.serverUrl) { book, serverUrl ->
                book?.coverModel(serverUrl)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val playerState: StateFlow<PlayerState> = playerConnection.state

        val playerSettings: StateFlow<PlayerSettings> =
            playerSettingsStore.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerSettings())

        val dateFormat: StateFlow<String> =
            authRepository.dateFormat
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    DateTimeFormat.DEFAULT_DATE_FORMAT,
                )

        /** True when the player is currently loaded with this book. */
        val isActiveBook: Boolean
            get() = playerConnection.state.value.bookId == bookId && !playerConnection.state.value.isPreview

        val book: StateFlow<BookEntity?> =
            libraryRepository
                .observeBook(bookId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val chapters: StateFlow<List<ChapterEntity>> =
            libraryRepository
                .observeChapters(bookId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val progress: StateFlow<ProgressEntity?> =
            libraryRepository
                .observeProgress(bookId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val downloadStatus: StateFlow<DownloadStatus> =
            downloadRepository
                .observeStatus(bookId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadStatus())

        private val _state = MutableStateFlow(BookDetailUiState())
        val state: StateFlow<BookDetailUiState> = _state.asStateFlow()

        /** Series this book belongs to (name + sequence + id for deep-linking). */
        private val _series = MutableStateFlow<List<BookSeriesRef>>(emptyList())
        val series: StateFlow<List<BookSeriesRef>> = _series.asStateFlow()

        val filterMetadata: StateFlow<Map<LibraryFilterCategory, List<BookMetadataLink>>> =
            combine(
                libraryRepository.observeBookFacets(bookId),
                collectionRepository.observeAll(),
                collectionRepository.observeAllMembers(),
                playlistRepository.observeAll(),
                playlistRepository.observeAllMembers(),
            ) { facets, collections, collectionMembers, playlists, playlistMembers ->
                val links = mutableMapOf<LibraryFilterCategory, List<BookMetadataLink>>()

                fun facetLinks(category: String): List<BookMetadataLink> =
                    facets.filter { it.category == category }.map { BookMetadataLink(it.facetId, it.name) }

                links[LibraryFilterCategory.AUTHORS] = facetLinks("author")
                links[LibraryFilterCategory.NARRATORS] = facetLinks("narrator")
                links[LibraryFilterCategory.GENRES] = facetLinks("genre")
                links[LibraryFilterCategory.PUBLISHERS] = facetLinks("publisher")
                links[LibraryFilterCategory.LANGUAGES] = facetLinks("language")

                val collectionIds =
                    collectionMembers.filter { it.bookId == bookId }.mapTo(mutableSetOf()) { it.collectionId }
                links[LibraryFilterCategory.COLLECTIONS] =
                    collections.filter { it.id in collectionIds }.map { BookMetadataLink(it.id, it.name) }

                val playlistIds =
                    playlistMembers.filter { it.bookId == bookId }.mapTo(mutableSetOf()) { it.playlistId }
                links[LibraryFilterCategory.PLAYLISTS] =
                    playlists.filter { it.id in playlistIds }.map { BookMetadataLink(it.id, it.name) }

                links
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

        init {
            refresh()
        }

        fun refresh() {
            _state.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                val result = libraryRepository.refreshBookDetail(bookId)
                result.onSuccess { detail ->
                    collectionRepository.refreshAll()
                    playlistRepository.refreshAll()
                    _series.value =
                        detail.series.map {
                            BookSeriesRef(id = it.id, name = it.name, sequence = it.sequence)
                        }
                }
                _state.update {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.let { e -> e.message ?: "Failed to load" },
                    )
                }
            }
        }

        fun startDownload() {
            val title = book.value?.title ?: "Audiobook"
            downloadRepository.enqueue(bookId, title)
        }

        fun cancelDownload() {
            downloadRepository.cancel(bookId)
            viewModelScope.launch { downloadRepository.delete(bookId) }
        }

        fun deleteDownload() {
            viewModelScope.launch { downloadRepository.delete(bookId) }
        }

        // --- Playback ---

        fun connectPlayer() = playerConnection.connect()

        fun play() {
            val b = book.value ?: return
            playerConnection.play(bookId, b.title, b.authors)
        }

        fun preload() {
            val b = book.value ?: return
            playerConnection.preload(bookId, b.title, b.authors)
        }

        /** Starts this book at a specific position (e.g. tapping a chapter). */
        fun playFrom(positionMs: Long) {
            val b = book.value ?: return
            playerConnection.play(bookId, b.title, b.authors, startPositionMs = positionMs)
        }

        fun playPause() {
            if (playerConnection.state.value.bookId == bookId) {
                playerConnection.playPause()
            } else {
                play()
            }
        }

        fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)

        fun seekBy(deltaMs: Long) = playerConnection.seekBy(deltaMs)

        // --- Bookmarks ---

        /** Refresh bookmarks from the server (best-effort; local cache still shows offline). */
        fun refreshBookmarks() {
            viewModelScope.launch { bookmarkRepository.refreshForBook(bookId) }
        }

        /** Adds a bookmark at the current player position (or saved progress) with an optional note. */
        fun addBookmark(note: String?) {
            val posMs =
                if (isActiveBook) {
                    playerConnection.state.value.positionMs
                } else {
                    ((progress.value?.currentSeconds ?: 0.0) * 1000).toLong()
                }
            viewModelScope.launch { bookmarkRepository.add(bookId, posMs / 1000.0, note) }
        }

        fun deleteBookmark(id: String) {
            viewModelScope.launch { bookmarkRepository.delete(id) }
        }

        fun playBookmark(positionSeconds: Double) {
            val posMs = (positionSeconds * 1000).toLong()
            if (isActiveBook) playerConnection.seekTo(posMs) else playFrom(posMs)
        }

        fun previewBookmark(positionSeconds: Double) {
            val b = book.value ?: return
            playerConnection.play(
                bookId = bookId,
                title = b.title,
                author = b.authors,
                startPositionMs = (positionSeconds * 1000).toLong(),
                preview = true,
            )
        }

        fun setSpeed(speed: Float) = playerConnection.setSpeed(speed)

        /** Jumps to the start of the previous/next chapter relative to current position. */
        fun jumpChapter(forward: Boolean) {
            val chs = chapters.value
            if (chs.isEmpty()) return
            val posSec = playerConnection.state.value.positionMs / 1000.0
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
    }
