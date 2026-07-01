package live.pageless.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import live.pageless.mobile.data.local.BookDao
import live.pageless.mobile.data.local.BookEntity
import live.pageless.mobile.data.local.MemberCoverRow
import live.pageless.mobile.data.local.SeriesBookEntity
import live.pageless.mobile.data.local.SeriesDao
import live.pageless.mobile.data.local.SeriesEntity
import live.pageless.mobile.data.remote.PagelessApi
import live.pageless.mobile.data.remote.SeriesDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first browse access to series. The UI observes Room; [refresh] pulls
 * from the server. Series are read-only on mobile (managed on the web).
 */
@Singleton
class SeriesRepository
    @Inject
    constructor(
        private val api: PagelessApi,
        private val seriesDao: SeriesDao,
        private val bookDao: BookDao,
        private val cacheCoordinator: CacheCoordinator,
        private val connectionStatusRepository: ConnectionStatusRepository,
    ) {
        fun observeAll(): Flow<List<SeriesEntity>> = seriesDao.observeAll()

        fun observe(id: String): Flow<SeriesEntity?> = seriesDao.observe(id)

        fun observeBooks(seriesId: String): Flow<List<BookEntity>> = seriesDao.observeBooks(seriesId)

        fun observeMembers(seriesId: String): Flow<List<SeriesBookEntity>> = seriesDao.observeMembers(seriesId)

        fun observeMemberPreviews(): Flow<List<MemberCoverRow>> = seriesDao.observeMemberPreviews()

        /** Pulls all series (with their books) into the local cache. */
        suspend fun refreshAll(): Result<Unit> {
            val result =
                runCatching {
                    cacheCoordinator.exclusive { api.series().series.forEach { cache(it) } }
                }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        /** Pulls a single series (with its books) into the local cache. */
        suspend fun refresh(id: String): Result<Unit> {
            val result = runCatching { cacheCoordinator.exclusive { cache(api.seriesDetail(id).series) } }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        private suspend fun cache(dto: SeriesDto) {
            seriesDao.upsert(SeriesEntity(id = dto.id, name = dto.name))

            // Cache the member books so covers/titles are available offline.
            bookDao.upsertAll(dto.books.map { it.toEntity(bookDao.get(it.id)) })

            val members =
                dto.books.mapIndexed { index, book ->
                    SeriesBookEntity(
                        seriesId = dto.id,
                        bookId = book.id,
                        sequence = book.sequence,
                        position = index,
                    )
                }
            seriesDao.replaceMembers(dto.id, members)
        }
    }
