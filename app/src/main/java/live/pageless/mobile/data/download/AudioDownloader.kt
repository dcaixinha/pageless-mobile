package live.pageless.mobile.data.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import live.pageless.mobile.data.local.SessionStore
import live.pageless.mobile.data.remote.bookDownloadUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/** Progress of an in-flight download. */
sealed interface DownloadProgress {
    data class Running(
        val bytesRead: Long,
        val totalBytes: Long?,
    ) : DownloadProgress {
        val fraction: Float? = totalBytes?.takeIf { it > 0 }?.let { (bytesRead.toFloat() / it) }
    }

    data class Completed(
        val file: File,
        val bytes: Long,
    ) : DownloadProgress

    data class Failed(
        val error: Throwable,
    ) : DownloadProgress
}

/**
 * Streams a book's `.m4b` from the server to app-private storage.
 *
 * Auth and base-URL rewriting are handled by the shared OkHttp interceptors, so
 * this just builds the download path and writes the body to disk, emitting
 * progress. Cancellation deletes the partial file.
 */
@Singleton
class AudioDownloader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val client: OkHttpClient,
        private val sessionStore: SessionStore,
    ) {
        private val downloadsDir: File
            get() = File(context.filesDir, AUDIO_DIR_NAME).apply { mkdirs() }

        fun fileFor(bookId: String): File = File(downloadsDir, "$bookId.m4b")

        fun download(bookId: String): Flow<DownloadProgress> =
            flow {
                val baseUrl = sessionStore.currentServerUrl()
                val url = bookDownloadUrl(baseUrl, bookId)
                val target = fileFor(bookId)
                val tmp = File(target.absolutePath + ".part")

                try {
                    val response = client.newCall(Request.Builder().url(url).build()).execute()
                    response.use {
                        if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
                        val body = it.body ?: throw IOException("empty body")
                        val total = body.contentLength().takeIf { len -> len > 0 }

                        body.byteStream().use { input ->
                            tmp.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var readTotal = 0L
                                while (true) {
                                    coroutineContext.ensureActive()
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    readTotal += read
                                    emit(DownloadProgress.Running(readTotal, total))
                                }
                            }
                        }

                        if (!tmp.renameTo(target)) {
                            tmp.copyTo(target, overwrite = true)
                            tmp.delete()
                        }
                        emit(DownloadProgress.Completed(target, target.length()))
                    }
                } catch (t: Throwable) {
                    tmp.delete()
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    emit(DownloadProgress.Failed(t))
                }
            }.flowOn(Dispatchers.IO)

        fun delete(bookId: String) {
            fileFor(bookId).delete()
        }

        /**
         * Deletes every downloaded book, including partial downloads.
         *
         * For account teardown, where the Room rows naming these files are being
         * cleared in the same operation. Unlike [delete] this takes no lock and
         * touches no database, so it is safe to call from inside an existing
         * [live.pageless.mobile.data.repository.CacheCoordinator] block.
         */
        fun deleteAllFiles(): Int = clearDirectoryContents(downloadsDir)

        /**
         * Dismisses any download notifications left in the shade.
         *
         * Lives here because this class already holds the download subsystem's
         * application [Context], which [AuthRepository][live.pageless.mobile.data.repository.AuthRepository]
         * deliberately does not.
         */
        fun cancelNotifications() = DownloadNotifications.cancelAll(context)
    }
