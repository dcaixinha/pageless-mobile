package live.pageless.mobile.data.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.pageless.mobile.data.local.BookDao
import live.pageless.mobile.data.local.BookEntity
import live.pageless.mobile.data.local.SessionStore
import live.pageless.mobile.data.remote.bookCoverUrl
import live.pageless.mobile.data.repository.CacheCoordinator
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Stores authenticated cover images in app-private storage for offline use. */
@Singleton
class CoverCache
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val client: OkHttpClient,
        private val sessionStore: SessionStore,
        private val bookDao: BookDao,
        private val cacheCoordinator: CacheCoordinator,
    ) {
        private val coversDir: File
            get() = File(context.filesDir, "covers").apply { mkdirs() }

        suspend fun cacheForOffline(book: BookEntity): String? =
            withContext(Dispatchers.IO) {
                cacheCoordinator.exclusive {
                    val current = bookDao.get(book.id) ?: return@exclusive null

                    if (!current.hasCover) {
                        clearBookCoverFiles(current.id)
                        bookDao.upsert(current.copy(coverLocalPath = null, coverUpdatedAt = null))
                        return@exclusive null
                    }

                    val existing = current.coverLocalPath?.let(::File)
                    if (existing?.exists() == true && current.coverUpdatedAt == current.updatedAt) {
                        return@exclusive existing.absolutePath
                    }

                    val url = bookCoverUrl(sessionStore.currentServerUrl(), current.id)
                    val response = client.newCall(Request.Builder().url(url).build()).execute()
                    response.use {
                        if (!it.isSuccessful) throw IOException("cover HTTP ${it.code}")
                        val body = it.body ?: throw IOException("empty cover body")
                        val ext = extensionFor(body.contentType()?.toString())
                        val target = File(coversDir, "${current.id}.$ext")
                        val tmp = File(target.absolutePath + ".part")

                        try {
                            body.byteStream().use { input ->
                                tmp.outputStream().use { output -> input.copyTo(output) }
                            }
                            if (!tmp.renameTo(target)) {
                                tmp.copyTo(target, overwrite = true)
                                tmp.delete()
                            }
                            clearBookCoverFiles(current.id, keep = target)
                            bookDao.upsert(
                                current.copy(coverLocalPath = target.absolutePath, coverUpdatedAt = current.updatedAt),
                            )
                            target.absolutePath
                        } catch (t: Throwable) {
                            tmp.delete()
                            throw t
                        }
                    }
                }
            }

        suspend fun delete(bookId: String) =
            withContext(Dispatchers.IO) {
                cacheCoordinator.exclusive {
                    clearBookCoverFiles(bookId)
                    bookDao.get(bookId)?.let { book ->
                        bookDao.upsert(book.copy(coverLocalPath = null, coverUpdatedAt = null))
                    }
                }
            }

        private fun clearBookCoverFiles(
            bookId: String,
            keep: File? = null,
        ) {
            coversDir
                .listFiles()
                ?.filter { it.name == bookId || it.name.startsWith("$bookId.") }
                ?.filter { keep == null || it.absolutePath != keep.absolutePath }
                ?.forEach { it.delete() }
        }

        private fun extensionFor(contentType: String?): String =
            when (contentType?.substringBefore(';')?.lowercase()) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
    }
