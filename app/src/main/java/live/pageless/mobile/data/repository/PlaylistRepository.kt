package live.pageless.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import live.pageless.mobile.data.local.BookDao
import live.pageless.mobile.data.local.BookEntity
import live.pageless.mobile.data.local.MemberCoverRow
import live.pageless.mobile.data.local.PlaylistBookEntity
import live.pageless.mobile.data.local.PlaylistDao
import live.pageless.mobile.data.local.PlaylistEntity
import live.pageless.mobile.data.remote.PagelessApi
import live.pageless.mobile.data.remote.PlaylistDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first browse access to the user's playlists. The UI observes Room;
 * [refresh] pulls from the server. Read-only on mobile.
 */
@Singleton
class PlaylistRepository
    @Inject
    constructor(
        private val api: PagelessApi,
        private val playlistDao: PlaylistDao,
        private val bookDao: BookDao,
        private val cacheCoordinator: CacheCoordinator,
        private val connectionStatusRepository: ConnectionStatusRepository,
    ) {
        fun observeAll(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()

        fun observe(id: String): Flow<PlaylistEntity?> = playlistDao.observe(id)

        fun observeAllMembers(): Flow<List<PlaylistBookEntity>> = playlistDao.observeAllMembers()

        fun observeBooks(playlistId: String): Flow<List<BookEntity>> = playlistDao.observeBooks(playlistId)

        fun observeMemberPreviews(): Flow<List<MemberCoverRow>> = playlistDao.observeMemberPreviews()

        suspend fun refreshAll(): Result<Unit> {
            val result =
                runCatching {
                    cacheCoordinator.exclusive {
                        val playlists = api.playlists().playlists
                        val books = playlists.flatMap { it.books }.distinctBy { it.id }
                        bookDao.upsertAll(books.map { it.toEntity(bookDao.get(it.id)) })

                        val entities = playlists.map { PlaylistEntity(it.id, it.name, it.description, it.updatedAt) }
                        val members =
                            playlists.flatMap { playlist ->
                                playlist.books.mapIndexed { index, book ->
                                    PlaylistBookEntity(playlist.id, book.id, index)
                                }
                            }
                        playlistDao.replaceAll(entities, members)
                    }
                }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        suspend fun refresh(id: String): Result<Unit> {
            val result = runCatching { cacheCoordinator.exclusive { cache(api.playlist(id).playlist) } }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        private suspend fun cache(dto: PlaylistDto) {
            playlistDao.upsert(
                PlaylistEntity(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    updatedAt = dto.updatedAt,
                ),
            )

            bookDao.upsertAll(dto.books.map { it.toEntity(bookDao.get(it.id)) })

            val members =
                dto.books.mapIndexed { index, book ->
                    PlaylistBookEntity(playlistId = dto.id, bookId = book.id, position = index)
                }
            playlistDao.replaceMembers(dto.id, members)
        }
    }
