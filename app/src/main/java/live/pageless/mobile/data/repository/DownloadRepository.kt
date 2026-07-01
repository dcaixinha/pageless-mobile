package live.pageless.mobile.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import live.pageless.mobile.data.download.AudioDownloader
import live.pageless.mobile.data.download.CoverCache
import live.pageless.mobile.data.download.DownloadWorker
import live.pageless.mobile.data.local.DownloadDao
import live.pageless.mobile.data.local.DownloadEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** UI-facing download status derived from WorkManager + the persisted row. */
data class DownloadStatus(
    val completed: Boolean = false,
    val running: Boolean = false,
    val progressPercent: Int? = null,
    val failed: Boolean = false,
)

/**
 * Manages offline downloads via WorkManager so they survive app death and retry
 * transient failures. The actual byte transfer lives in [DownloadWorker]; this
 * repository enqueues work, exposes status, and manages deletion.
 */
@Singleton
class DownloadRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val downloader: AudioDownloader,
        private val coverCache: CoverCache,
        private val downloadDao: DownloadDao,
    ) {
        private val workManager = WorkManager.getInstance(context)

        fun observe(bookId: String): Flow<DownloadEntity?> = downloadDao.observe(bookId)

        fun observeCompleted(): Flow<List<DownloadEntity>> = downloadDao.observeCompleted()

        /** Combined status for the UI, merging WorkManager progress with the DB row. */
        fun observeStatus(bookId: String): Flow<DownloadStatus> =
            combine(
                workManager.getWorkInfosForUniqueWorkFlow(DownloadWorker.workName(bookId)),
                downloadDao.observe(bookId),
            ) { infos, download ->
                val info = infos.firstOrNull()
                when (info?.state) {
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED ->
                        DownloadStatus(
                            running = true,
                            progressPercent =
                                info.progress
                                    .getInt(DownloadWorker.KEY_PROGRESS, -1)
                                    .takeIf { it >= 0 },
                        )

                    WorkInfo.State.SUCCEEDED -> DownloadStatus(completed = download?.completed == true)
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> DownloadStatus(failed = info.state == WorkInfo.State.FAILED)
                    else -> DownloadStatus(completed = download?.completed == true)
                }
            }

        /** Enqueues a download; unique per book so re-tapping doesn't duplicate work. */
        fun enqueue(
            bookId: String,
            title: String,
        ) {
            val request =
                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setInputData(DownloadWorker.inputData(bookId, title))
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()

            workManager.enqueueUniqueWork(
                DownloadWorker.workName(bookId),
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(bookId: String) {
            workManager.cancelUniqueWork(DownloadWorker.workName(bookId))
        }

        suspend fun delete(bookId: String) {
            workManager.cancelUniqueWork(DownloadWorker.workName(bookId))
            downloader.delete(bookId)
            coverCache.delete(bookId)
            downloadDao.delete(bookId)
        }

        /** Local path if the book is fully downloaded, else null. */
        suspend fun localPathIfComplete(bookId: String): String? =
            downloadDao
                .get(bookId)
                ?.takeIf { it.completed }
                ?.localPath
                ?.takeIf { File(it).exists() }
    }
