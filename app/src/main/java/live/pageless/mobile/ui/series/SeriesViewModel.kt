package live.pageless.mobile.ui.series

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
import live.pageless.mobile.data.repository.SeriesRepository
import live.pageless.mobile.data.repository.coverModel
import javax.inject.Inject

/** A card on the series index: name + up to four cover URLs for the mosaic. */
data class SeriesTile(
    val id: String,
    val name: String,
    val bookCount: Int,
    val coverUrls: List<String>,
)

data class BrowseUiState(
    val refreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SeriesViewModel
    @Inject
    constructor(
        private val seriesRepository: SeriesRepository,
        authRepository: AuthRepository,
    ) : ViewModel() {
        val tiles: StateFlow<List<SeriesTile>> =
            combine(
                seriesRepository.observeAll(),
                seriesRepository.observeMemberPreviews(),
                authRepository.serverUrl,
            ) { series, previews, serverUrl ->
                val byParent = previews.groupBy { it.parentId }
                series.map { s ->
                    val members = byParent[s.id].orEmpty()
                    SeriesTile(
                        id = s.id,
                        name = s.name,
                        bookCount = members.size,
                        coverUrls = members.take(4).mapNotNull { it.coverModel(serverUrl) },
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private val _state = MutableStateFlow(BrowseUiState())
        val state: StateFlow<BrowseUiState> = _state.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            _state.update { it.copy(refreshing = true, error = null) }
            viewModelScope.launch {
                val result = seriesRepository.refreshAll()
                _state.update {
                    it.copy(
                        refreshing = false,
                        error = result.exceptionOrNull()?.let { e -> e.message ?: "Failed to load" },
                    )
                }
            }
        }
    }
