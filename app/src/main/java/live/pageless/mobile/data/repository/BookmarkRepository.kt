package live.pageless.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import live.pageless.mobile.core.Iso8601
import live.pageless.mobile.data.local.BookmarkDao
import live.pageless.mobile.data.local.BookmarkEntity
import live.pageless.mobile.data.remote.BookmarkUpsertRequest
import live.pageless.mobile.data.remote.PagelessApi
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first bookmarks. Local creates/deletes are applied immediately (with
 * client-generated UUIDs matching the server's key), and [sync] pushes dirty
 * records + deletion tombstones, then pulls remote changes.
 */
@Singleton
class BookmarkRepository
    @Inject
    constructor(
        private val api: PagelessApi,
        private val bookmarkDao: BookmarkDao,
        private val connectionStatusRepository: ConnectionStatusRepository,
    ) {
        fun observeForBook(bookId: String): Flow<List<BookmarkEntity>> = bookmarkDao.observeForBook(bookId)

        /** Adds a bookmark at [positionSeconds] locally (marked dirty for sync). */
        suspend fun add(
            bookId: String,
            positionSeconds: Double,
            note: String?,
        ) {
            bookmarkDao.upsert(
                BookmarkEntity(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    positionSeconds = positionSeconds.coerceAtLeast(0.0),
                    note = note?.trim()?.ifEmpty { null },
                    updatedAt = Iso8601.now(),
                    dirty = true,
                    deleted = false,
                ),
            )
        }

        /** Tombstones a bookmark locally; the deletion is pushed on next sync. */
        suspend fun delete(id: String) {
            bookmarkDao.markDeleted(id)
        }

        /**
         * Two-way sync: push dirty creates/updates and deletion tombstones, then
         * pull remote bookmarks and merge them into the local cache.
         */
        suspend fun sync(since: String? = null): Result<Unit> {
            val result =
                runCatching {
                    // Push local deletions first.
                    for (tomb in bookmarkDao.tombstones()) {
                        runCatching { api.deleteBookmark(tomb.id) }
                        bookmarkDao.hardDelete(tomb.id)
                    }

                    // Push local creates/updates (idempotent PUT with our id).
                    for (local in bookmarkDao.dirty()) {
                        api.upsertBookmark(
                            local.id,
                            BookmarkUpsertRequest(
                                bookId = local.bookId,
                                positionSeconds = local.positionSeconds,
                                note = local.note,
                            ),
                        )
                        bookmarkDao.clearDirty(local.id)
                    }

                    // Pull remote and apply (skip anything still pending local changes).
                    for (dto in api.bookmarks(since).bookmarks) {
                        applyRemote(dto)
                    }
                }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        /**
         * Refreshes a single book's bookmarks from the server into the local cache
         * (used when opening the bookmarks view online).
         */
        suspend fun refreshForBook(bookId: String): Result<Unit> {
            val result =
                runCatching {
                    for (dto in api.bookmarksForBook(bookId).bookmarks) {
                        applyRemote(dto)
                    }
                }
            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        /**
         * Applies a pulled bookmark to the local cache: a server tombstone
         * (`deleted`) removes it locally; otherwise it's stored. Records with
         * pending local changes are left untouched (pushed on next sync).
         */
        private suspend fun applyRemote(dto: live.pageless.mobile.data.remote.BookmarkDto) {
            val current = bookmarkDao.get(dto.id)
            if (current?.dirty == true || current?.deleted == true) return

            if (dto.deleted) {
                bookmarkDao.hardDelete(dto.id)
            } else {
                bookmarkDao.upsert(
                    BookmarkEntity(
                        id = dto.id,
                        bookId = dto.bookId,
                        positionSeconds = dto.positionSeconds,
                        note = dto.note,
                        updatedAt = dto.updatedAt,
                        dirty = false,
                        deleted = false,
                    ),
                )
            }
        }
    }
