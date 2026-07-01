package live.pageless.mobile.data.repository

import android.os.Build
import kotlinx.coroutines.flow.Flow
import live.pageless.mobile.core.Iso8601
import live.pageless.mobile.data.local.PlaybackEventEntity
import live.pageless.mobile.data.local.PlaybackHistoryDao
import live.pageless.mobile.data.local.PlaybackSessionEntity
import live.pageless.mobile.data.remote.ListeningEventSyncDto
import live.pageless.mobile.data.remote.ListeningHistorySyncRequest
import live.pageless.mobile.data.remote.ListeningSessionSyncDto
import live.pageless.mobile.data.remote.PagelessApi
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackSessionStart(
    val bookId: String,
    val title: String?,
    val authors: String?,
    val playMethod: String,
    val positionSeconds: Double,
    val durationSeconds: Double,
)

@Singleton
class PlaybackHistoryRepository
    @Inject
    constructor(
        private val dao: PlaybackHistoryDao,
        private val api: PagelessApi,
        private val connectionStatusRepository: ConnectionStatusRepository,
    ) {
        fun observeSessionsForBook(bookId: String): Flow<List<PlaybackSessionEntity>> = dao.observeSessionsForBook(bookId)

        fun observeEventsForBook(bookId: String): Flow<List<PlaybackEventEntity>> = dao.observeEventsForBook(bookId)

        suspend fun startSession(input: PlaybackSessionStart): String {
            val now = Iso8601.now()
            val sessionId = UUID.randomUUID().toString()
            dao.upsertSession(
                PlaybackSessionEntity(
                    id = sessionId,
                    bookId = input.bookId,
                    title = input.title,
                    authors = input.authors,
                    playMethod = input.playMethod,
                    deviceInfo = deviceInfo(),
                    startedAt = now,
                    updatedAt = now,
                    endedAt = null,
                    timeListenedSeconds = 0,
                    lastPositionSeconds = input.positionSeconds.coerceAtLeast(0.0),
                    durationSeconds = input.durationSeconds.coerceAtLeast(0.0),
                ),
            )
            recordEvent(sessionId, input.bookId, "Play", input.positionSeconds)
            return sessionId
        }

        suspend fun addListeningTime(
            sessionId: String,
            seconds: Long,
            positionSeconds: Double,
        ) {
            val session = dao.getSession(sessionId) ?: return
            dao.upsertSession(
                session.copy(
                    updatedAt = Iso8601.now(),
                    timeListenedSeconds = session.timeListenedSeconds + seconds.coerceAtLeast(0),
                    lastPositionSeconds = positionSeconds.coerceAtLeast(0.0),
                    dirty = true,
                ),
            )
        }

        suspend fun endSession(
            sessionId: String,
            positionSeconds: Double,
        ) {
            val session = dao.getSession(sessionId) ?: return
            val now = Iso8601.now()
            dao.upsertSession(
                session.copy(
                    updatedAt = now,
                    endedAt = now,
                    lastPositionSeconds = positionSeconds.coerceAtLeast(0.0),
                    dirty = true,
                ),
            )
        }

        suspend fun recordEvent(
            sessionId: String,
            bookId: String,
            event: String,
            positionSeconds: Double,
            type: String = "Playback",
            serverSyncAttempted: Boolean = false,
            serverSyncSuccess: Boolean? = null,
            serverSyncMessage: String? = null,
        ) {
            dao.upsertEvent(
                PlaybackEventEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    bookId = bookId,
                    event = event,
                    type = type,
                    positionSeconds = positionSeconds.coerceAtLeast(0.0),
                    timestamp = Iso8601.now(),
                    serverSyncAttempted = serverSyncAttempted,
                    serverSyncSuccess = serverSyncSuccess,
                    serverSyncMessage = serverSyncMessage,
                ),
            )
        }

        suspend fun sync(): Result<Unit> {
            val sessions = dao.dirtySessions()
            val events = dao.dirtyEvents()
            if (sessions.isEmpty() && events.isEmpty()) return Result.success(Unit)

            val result =
                runCatching {
                    api.syncListeningHistory(
                        ListeningHistorySyncRequest(
                            sessions = sessions.map { it.toSyncDto() },
                            events = events.map { it.toSyncDto() },
                        ),
                    )
                    if (sessions.isNotEmpty()) dao.clearSessionDirty(sessions.map { it.id })
                    if (events.isNotEmpty()) dao.clearEventDirty(events.map { it.id })
                }

            result
                .onSuccess { connectionStatusRepository.markServerSuccess() }
                .onFailure { connectionStatusRepository.markServerFailure() }
            return result
        }

        private fun deviceInfo(): String = "Android ${Build.VERSION.RELEASE}\n${Build.MANUFACTURER} ${Build.MODEL}".trim()

        private fun PlaybackSessionEntity.toSyncDto(): ListeningSessionSyncDto =
            ListeningSessionSyncDto(
                id = id,
                bookId = bookId,
                title = title,
                authors = authors,
                playMethod = playMethod,
                deviceInfo = deviceInfo,
                startedAt = startedAt,
                updatedAt = updatedAt,
                endedAt = endedAt,
                timeListenedSeconds = timeListenedSeconds,
                lastPositionSeconds = lastPositionSeconds,
                durationSeconds = durationSeconds,
            )

        private fun PlaybackEventEntity.toSyncDto(): ListeningEventSyncDto =
            ListeningEventSyncDto(
                id = id,
                sessionId = sessionId,
                bookId = bookId,
                event = event,
                type = type,
                positionSeconds = positionSeconds,
                timestamp = timestamp,
                serverSyncAttempted = serverSyncAttempted,
                serverSyncSuccess = serverSyncSuccess,
                serverSyncMessage = serverSyncMessage,
            )
    }
