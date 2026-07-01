package live.pageless.mobile.ui.home

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
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.data.repository.DownloadRepository
import live.pageless.mobile.data.repository.HomeRepository
import live.pageless.mobile.data.repository.HomeShelves
import live.pageless.mobile.data.repository.LibraryRepository
import live.pageless.mobile.data.repository.ShelfBook
import live.pageless.mobile.data.repository.coverModel
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val shelves: HomeShelves = HomeShelves(),
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val homeRepository: HomeRepository,
        private val downloadRepository: DownloadRepository,
        private val authRepository: AuthRepository,
        libraryRepository: LibraryRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = _state.asStateFlow()

        /** Locally downloaded books, sourced from Room so they work offline. */
        val localBooks: StateFlow<List<ShelfBook>> =
            combine(
                downloadRepository.observeCompleted(),
                libraryRepository.observeBooks(),
                libraryRepository.observeAllProgress(),
                authRepository.serverUrl,
            ) { downloads, books, progressList, serverUrl ->
                val byId = books.associateBy { it.id }
                val progressByBook = progressList.associateBy { it.bookId }
                downloads.mapNotNull { dl ->
                    val book = byId[dl.bookId] ?: return@mapNotNull null
                    val progress = progressByBook[book.id]
                    val fraction =
                        progress?.let { p ->
                            if (p.durationSeconds > 0) {
                                (p.currentSeconds / p.durationSeconds).toFloat().coerceIn(0f, 1f)
                            } else {
                                null
                            }
                        }
                    ShelfBook(
                        id = book.id,
                        title = book.title,
                        author = book.authors,
                        coverUrl = book.coverModel(serverUrl),
                        finished = progress?.finished == true,
                        progressFraction = fraction,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            refresh()
        }

        fun refresh() {
            _state.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                homeRepository.load().fold(
                    onSuccess = { shelves ->
                        _state.update { it.copy(loading = false, shelves = shelves) }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(loading = false, error = e.message ?: "Failed to load") }
                    },
                )
            }
        }

        fun logout(onDone: () -> Unit) {
            viewModelScope.launch {
                authRepository.logout()
                onDone()
            }
        }
    }
