package live.pageless.mobile.ui.collections

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
import live.pageless.mobile.data.repository.CollectionRepository
import live.pageless.mobile.data.repository.coverModel
import javax.inject.Inject

data class CollectionTile(
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
class CollectionsViewModel
    @Inject
    constructor(
        private val collectionRepository: CollectionRepository,
        authRepository: AuthRepository,
    ) : ViewModel() {
        val tiles: StateFlow<List<CollectionTile>> =
            combine(
                collectionRepository.observeAll(),
                collectionRepository.observeMemberPreviews(),
                authRepository.serverUrl,
            ) { collections, previews, serverUrl ->
                val byParent = previews.groupBy { it.parentId }
                collections.map { c ->
                    val members = byParent[c.id].orEmpty()
                    CollectionTile(
                        id = c.id,
                        name = c.name,
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
                val result = collectionRepository.refreshAll()
                _state.update {
                    it.copy(
                        refreshing = false,
                        error = result.exceptionOrNull()?.let { e -> e.message ?: "Failed to load" },
                    )
                }
            }
        }
    }
