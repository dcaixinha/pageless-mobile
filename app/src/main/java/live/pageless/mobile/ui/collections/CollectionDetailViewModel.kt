package live.pageless.mobile.ui.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import live.pageless.mobile.data.local.CollectionEntity
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.data.repository.CollectionRepository
import live.pageless.mobile.data.repository.coverModel
import live.pageless.mobile.ui.series.SeriesBookRow
import javax.inject.Inject

@HiltViewModel
class CollectionDetailViewModel
    @Inject
    constructor(
        private val collectionRepository: CollectionRepository,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val collectionId: String = checkNotNull(savedStateHandle["collectionId"])

        val collection: StateFlow<CollectionEntity?> =
            collectionRepository
                .observe(collectionId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val books: StateFlow<List<SeriesBookRow>> =
            combine(
                collectionRepository.observeBooks(collectionId),
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
            viewModelScope.launch { collectionRepository.refresh(collectionId) }
        }
    }
