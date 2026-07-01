package live.pageless.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import live.pageless.mobile.core.Iso8601
import live.pageless.mobile.core.PlaybackRules
import live.pageless.mobile.core.ProgressMerge
import live.pageless.mobile.data.local.ProgressDao
import live.pageless.mobile.data.local.ProgressEntity
import live.pageless.mobile.data.remote.PagelessApi
import live.pageless.mobile.data.remote.ProgressUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Adapter so [ProgressEntity] can be compared by the pure merge rule. */
private class TimestampedEntity(
    private val e: ProgressEntity?,
) : ProgressMerge.Timestamped {
    override val lastPlayedAt: String? = e?.lastPlayedAt
}

/**
 * Offline-first playback progress: local writes are optimistic (persisted
 * immediately with a `dirty` flag), and [sync] reconciles with the server using
 * the same last-write-wins rule the server applies.
 */
@Singleton
class ProgressRepository
    @Inject
    constructor(
        private val api: PagelessApi,
        private val progressDao: ProgressDao,
        private val connectionStatusRepository: ConnectionStatusRepository,
    ) {
        private val syncMutex = Mutex()

        fun observe(bookId: String): Flow<ProgressEntity?> = progressDao.observe(bookId)

        /** One-shot read of the saved position in seconds (0.0 if none/finished). */
        suspend fun resumePositionSeconds(bookId: String): Double {
            val p = progressDao.get(bookId) ?: return 0.0
            // If the book was finished, start from the beginning again.
            return if (p.finished) 0.0 else p.currentSeconds
        }

        /**
         * Records a local playback position. Marked dirty for later push. Finished
         * state is derived with the shared rule so it matches the server.
         */
        suspend fun record(
            bookId: String,
            currentSeconds: Double,
            durationSeconds: Double,
        ) {
            val now = Iso8601.now()
            progressDao.upsert(
                updatedProgress(
                    existing = progressDao.get(bookId),
                    bookId = bookId,
                    currentSeconds = currentSeconds,
                    durationSeconds = durationSeconds,
                    now = now,
                ),
            )
        }

        /**
         * Two-way sync: push all dirty local records, then pull the server's changes
         * and merge them in (server value wins only when strictly newer). Returns a
         * Result so callers can surface failures without crashing offline flows.
         */
        suspend fun sync(since: String? = null): Result<Unit> =
            syncMutex.withLock {
                val result =
                    runCatching {
                        // Push local changes first so the server has our latest before we pull.
                        for (local in progressDao.dirty()) {
                            val response =
                                api.updateProgress(
                                    local.bookId,
                                    ProgressUpdateRequest(
                                        currentSeconds = local.currentSeconds,
                                        durationSeconds = local.durationSeconds,
                                        lastPlayedAt = local.lastPlayedAt ?: Iso8601.now(),
                                    ),
                                )
                            // Store the server's authoritative result and clear the dirty flag.
                            progressDao.upsert(response.progress.toEntity(dirty = false))
                        }

                        // Pull remote changes and merge non-dirty updates.
                        val remote = api.progress(since).progress
                        for (dto in remote) {
                            val current = progressDao.get(dto.bookId)
                            // Never clobber an unsynced local write; that will be pushed next sync.
                            if (current?.dirty == true) continue

                            // A server tombstone removes the local record (progress reset elsewhere).
                            if (dto.deleted) {
                                progressDao.delete(dto.bookId)
                                continue
                            }

                            if (ProgressMerge.incomingWins(TimestampedEntity(current), TimestampedEntity(dto.toEntity()))) {
                                progressDao.upsert(dto.toEntity(dirty = false))
                            }
                        }
                    }
                result
                    .onSuccess { connectionStatusRepository.markServerSuccess() }
                    .onFailure { connectionStatusRepository.markServerFailure() }
            }
    }

internal fun updatedProgress(
    existing: ProgressEntity?,
    bookId: String,
    currentSeconds: Double,
    durationSeconds: Double,
    now: String,
): ProgressEntity {
    val finished = PlaybackRules.finishedAtPosition(currentSeconds, durationSeconds)
    return ProgressEntity(
        bookId = bookId,
        currentSeconds = currentSeconds.coerceAtLeast(0.0),
        durationSeconds = durationSeconds.coerceAtLeast(0.0),
        finished = finished,
        startedAt = existing?.startedAt ?: now,
        finishedAt = if (finished) existing?.finishedAt ?: now else null,
        lastPlayedAt = now,
        updatedAt = now,
        dirty = true,
    )
}
