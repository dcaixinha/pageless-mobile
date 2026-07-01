package live.pageless.mobile.data.repository

import live.pageless.mobile.data.local.BookDao
import live.pageless.mobile.data.local.SessionStore
import live.pageless.mobile.data.remote.BookSummaryDto
import live.pageless.mobile.data.remote.PagelessApi
import live.pageless.mobile.data.remote.bookCoverUrl
import javax.inject.Inject
import javax.inject.Singleton

/** A book as shown on a home shelf. */
data class ShelfBook(
    val id: String,
    val title: String,
    val author: String?,
    val coverUrl: String?,
    val finished: Boolean = false,
    /** 0f..1f listening progress, or null when not started/unknown. */
    val progressFraction: Float? = null,
)

/** The home page's shelves. */
data class HomeShelves(
    val continueListening: List<ShelfBook> = emptyList(),
    val discover: List<ShelfBook> = emptyList(),
    val listenAgain: List<ShelfBook> = emptyList(),
)

@Singleton
class HomeRepository
    @Inject
    constructor(
        private val api: PagelessApi,
        private val sessionStore: SessionStore,
        private val bookDao: BookDao,
        private val connectionStatusRepository: ConnectionStatusRepository,
    ) {
        suspend fun load(): Result<HomeShelves> {
            val result =
                runCatching {
                    val baseUrl = sessionStore.currentServerUrl()
                    val home = api.home()
                    HomeShelves(
                        continueListening = home.continueListening.map { it.toShelfBook(baseUrl) },
                        discover = home.discover.map { it.toShelfBook(baseUrl) },
                        listenAgain = home.listenAgain.map { it.toShelfBook(baseUrl) },
                    )
                }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        private suspend fun BookSummaryDto.toShelfBook(baseUrl: String): ShelfBook {
            val fraction =
                progress?.let { p ->
                    if (p.durationSeconds > 0) {
                        (p.currentSeconds / p.durationSeconds).toFloat().coerceIn(0f, 1f)
                    } else {
                        null
                    }
                }
            return ShelfBook(
                id = id,
                title = title,
                author = authors.joinToString(", ") { it.name }.ifEmpty { null },
                coverUrl = bookDao.get(id)?.coverModel(baseUrl) ?: if (hasCover) bookCoverUrl(baseUrl, id) else null,
                finished = progress?.finished == true,
                progressFraction = fraction,
            )
        }
    }
