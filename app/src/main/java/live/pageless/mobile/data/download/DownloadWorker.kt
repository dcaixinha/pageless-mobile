package live.pageless.mobile.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import live.pageless.mobile.data.local.DownloadDao
import live.pageless.mobile.data.local.DownloadEntity
import live.pageless.mobile.data.repository.LibraryRepository

/**
 * Downloads a book's audio in the background, surviving app death and retrying
 * transient failures. Runs as a foreground worker so it shows an ongoing
 * progress notification (and isn't killed while the app is backgrounded).
 * The DB row is updated on completion so the rest of the app observes
 * availability.
 */
@HiltWorker
class DownloadWorker
    @AssistedInject
    constructor(
        @Assisted private val appContext: Context,
        @Assisted params: WorkerParameters,
        private val downloader: AudioDownloader,
        private val coverCache: CoverCache,
        private val downloadDao: DownloadDao,
        private val libraryRepository: LibraryRepository,
    ) : CoroutineWorker(appContext, params) {
        private val bookId: String? = inputData.getString(KEY_BOOK_ID)
        private val title: String = inputData.getString(KEY_TITLE) ?: "Audiobook"

        override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(percent = null)

        override suspend fun doWork(): Result {
            val bookId = bookId ?: return Result.failure()
            val file = downloader.fileFor(bookId)

            // Show the ongoing notification up front (indeterminate until first tick).
            setForeground(foregroundInfo(percent = null))

            val metadataResult = libraryRepository.refreshBook(bookId)
            if (metadataResult.isFailure) return downloadFailed(bookId)

            val book = libraryRepository.getBook(bookId) ?: return downloadFailed(bookId)
            if (book.hasCover) {
                val coverResult = runCatching { coverCache.cacheForOffline(book) }
                if (coverResult.isFailure) return downloadFailed(bookId)
            }

            // Throttle progress updates: the download emits on every buffer read
            // (many/sec). Updating the notification/WorkManager that often floods
            // Android's notification rate limiter, so update at most ~1x/sec and
            // only when the whole-percent value actually changes.
            var lastUpdateMs = 0L
            var lastPercent = -1
            var rowPersisted = false

            var terminal: Result? = null
            downloader.download(bookId).collect { p ->
                when (p) {
                    is DownloadProgress.Running -> {
                        val pct = p.fraction?.let { (it * 100).toInt() } ?: -1
                        val now = System.currentTimeMillis()
                        val changed = pct != lastPercent
                        val elapsed = now - lastUpdateMs >= PROGRESS_INTERVAL_MS

                        if ((changed && elapsed) || !rowPersisted) {
                            lastUpdateMs = now
                            lastPercent = pct
                            rowPersisted = true
                            downloadDao.upsert(
                                DownloadEntity(bookId, file.absolutePath, "audio/mp4", p.totalBytes, completed = false),
                            )
                            setProgress(workDataOf(KEY_PROGRESS to pct))
                            setForeground(foregroundInfo(percent = pct.takeIf { it >= 0 }))
                        }
                    }

                    is DownloadProgress.Completed -> {
                        downloadDao.upsert(
                            DownloadEntity(bookId, p.file.absolutePath, "audio/mp4", p.bytes, completed = true),
                        )
                        DownloadNotifications.completed(appContext, bookId, title)
                        terminal = Result.success()
                    }

                    is DownloadProgress.Failed -> {
                        terminal = downloadFailed(bookId)
                    }
                }
            }
            return terminal ?: Result.success()
        }

        private suspend fun downloadFailed(bookId: String): Result {
            downloadDao.delete(bookId)
            // Retry transient failures up to the attempt cap.
            return if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                DownloadNotifications.failed(appContext, bookId, title)
                Result.failure()
            }
        }

        private fun foregroundInfo(percent: Int?): ForegroundInfo {
            val id = DownloadNotifications.progressNotificationId(bookId ?: title)
            val notification = DownloadNotifications.progress(appContext, title, percent)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                ForegroundInfo(id, notification)
            }
        }

        companion object {
            const val KEY_BOOK_ID = "book_id"
            const val KEY_TITLE = "title"
            const val KEY_PROGRESS = "progress"
            private const val MAX_ATTEMPTS = 3
            private const val PROGRESS_INTERVAL_MS = 1_000L

            fun inputData(
                bookId: String,
                title: String,
            ): Data = workDataOf(KEY_BOOK_ID to bookId, KEY_TITLE to title)

            fun workName(bookId: String): String = "download_$bookId"
        }
    }
