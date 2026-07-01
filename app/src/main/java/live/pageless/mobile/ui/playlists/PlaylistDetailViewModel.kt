package live.pageless.mobile.ui.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import live.pageless.mobile.data.local.PlaylistEntity
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.data.repository.PlaylistRepository
import live.pageless.mobile.data.repository.coverModel
import live.pageless.mobile.ui.series.SeriesBookRow
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

        val playlist: StateFlow<PlaylistEntity?> =
            playlistRepository
                .observe(playlistId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val books: StateFlow<List<SeriesBookRow>> =
            combine(
                playlistRepository.observeBooks(playlistId),
                authRepository.serverUrl,
            ) { books, serverUrl ->
                books.map { b ->
                    SeriesBookRow(
                        id = b.id,
                        title = b.title,
                        author = b.authors,
                        coverUrl = b.coverModel(serverUrl),
                        sequence = null,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            viewModelScope.launch { playlistRepository.refresh(playlistId) }
        }
    }
