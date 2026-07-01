package live.pageless.mobile.ui.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import live.pageless.mobile.data.local.SeriesEntity
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.data.repository.SeriesRepository
import live.pageless.mobile.data.repository.coverModel
import javax.inject.Inject

/** A book within a series detail, with its sequence badge. */
data class SeriesBookRow(
    val id: String,
    val title: String,
    val author: String?,
    val coverUrl: String?,
    val sequence: String?,
)

@HiltViewModel
class SeriesDetailViewModel
    @Inject
    constructor(
        private val seriesRepository: SeriesRepository,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val seriesId: String = checkNotNull(savedStateHandle["seriesId"])

        val series: StateFlow<SeriesEntity?> =
            seriesRepository
                .observe(seriesId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val books: StateFlow<List<SeriesBookRow>> =
            combine(
                seriesRepository.observeBooks(seriesId),
                seriesRepository.observeMembers(seriesId),
                authRepository.serverUrl,
            ) { books, members, serverUrl ->
                val sequenceByBook = members.associate { it.bookId to it.sequence }
                books.map { b ->
                    SeriesBookRow(
                        id = b.id,
                        title = b.title,
                        author = b.authors,
                        coverUrl = b.coverModel(serverUrl),
                        sequence = sequenceByBook[b.id],
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            viewModelScope.launch { seriesRepository.refresh(seriesId) }
        }
    }
