package live.pageless.mobile.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import live.pageless.mobile.data.local.BookDao
import live.pageless.mobile.data.local.BookEntity
import live.pageless.mobile.data.local.BookFacetDao
import live.pageless.mobile.data.local.BookFacetEntity
import live.pageless.mobile.data.local.CachedLibraryDao
import live.pageless.mobile.data.local.CachedLibraryEntity
import live.pageless.mobile.data.local.ChapterDao
import live.pageless.mobile.data.local.ChapterEntity
import live.pageless.mobile.data.local.PagelessDatabase
import live.pageless.mobile.data.local.ProgressDao
import live.pageless.mobile.data.local.ProgressEntity
import live.pageless.mobile.data.remote.BookDetailDto
import live.pageless.mobile.data.remote.PagelessApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first library access: the UI observes Room, while [refresh] pulls
 * from the server and updates the local cache. Reads never require the network.
 */
@Singleton
class LibraryRepository
    @Inject
    constructor(
        private val api: PagelessApi,
        private val database: PagelessDatabase,
        private val bookDao: BookDao,
        private val bookFacetDao: BookFacetDao,
        private val cachedLibraryDao: CachedLibraryDao,
        private val cacheCoordinator: CacheCoordinator,
        private val chapterDao: ChapterDao,
        private val progressDao: ProgressDao,
        private val connectionStatusRepository: ConnectionStatusRepository,
    ) {
        fun observeBooks(): Flow<List<BookEntity>> = bookDao.observeAll()

        fun observeBookFacets(): Flow<List<BookFacetEntity>> = bookFacetDao.observeAll()

        fun observeBookFacets(bookId: String): Flow<List<BookFacetEntity>> = bookFacetDao.observeForBook(bookId)

        fun observeLibraries(): Flow<List<CachedLibraryEntity>> = cachedLibraryDao.observeAll()

        fun observeBook(id: String): Flow<BookEntity?> = bookDao.observe(id)

        suspend fun getBook(id: String): BookEntity? = bookDao.get(id)

        fun observeChapters(bookId: String): Flow<List<ChapterEntity>> = chapterDao.observeForBook(bookId)

        fun observeProgress(bookId: String): Flow<ProgressEntity?> = progressDao.observe(bookId)

        fun observeAllProgress(): Flow<List<ProgressEntity>> = progressDao.observeAll()

        /** Pulls all books from the server into the local cache. */
        suspend fun refreshBooks(): Result<Unit> {
            val result =
                runCatching {
                    cacheCoordinator.exclusive {
                        val libraries = api.libraries().libraries
                        val books = api.books().books
                        val facets = books.flatMap { it.toFacetEntities() }
                        val cachedLibraries = libraries.map { CachedLibraryEntity(it.id, it.name) }

                        database.withTransaction {
                            val entities = books.map { it.toEntity(bookDao.get(it.id)) }
                            bookDao.deleteAll()
                            bookDao.upsertAll(entities)
                            bookFacetDao.replaceAll(facets)
                            cachedLibraryDao.replaceAll(cachedLibraries)
                        }
                    }
                }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        /** Pulls a single book's detail (incl. chapters, progress) into the cache. */
        suspend fun refreshBook(id: String): Result<Unit> = refreshBookDetail(id).map { }

        /**
         * Like [refreshBook] but returns the fetched detail DTO so callers can read
         * fields not persisted on [BookEntity] (e.g. series membership).
         */
        suspend fun refreshBookDetail(id: String): Result<BookDetailDto> {
            val result =
                runCatching {
                    cacheCoordinator.exclusive {
                        val detail = api.book(id).book
                        database.withTransaction {
                            val existing = bookDao.get(id)
                            bookDao.upsert(detail.toEntity(existing))
                            bookFacetDao.replaceForBook(id, detail.toFacetEntities())
                            chapterDao.replaceForBook(id, detail.chapters.map { it.toEntity(id) })
                            detail.progress?.let {
                                // Do not clobber unsynced local playback with stale server progress.
                                if (progressDao.get(id)?.dirty != true) {
                                    progressDao.upsert(it.toEntity())
                                }
                            }
                        }
                        detail
                    }
                }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }
    }
