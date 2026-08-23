package live.pageless.mobile.data.download

import java.io.File

/** Subdirectory of `filesDir` holding downloaded `.m4b` files. */
internal const val AUDIO_DIR_NAME = "audiobooks"

/** Subdirectory of `filesDir` holding cached cover images. */
internal const val COVERS_DIR_NAME = "covers"

/**
 * Deletes everything inside [dir], leaving the directory itself in place, and
 * returns how many entries were removed.
 *
 * Deliberately works on the filesystem rather than iterating Room rows. Every
 * caller runs at a moment when the database is being wiped, so the rows that
 * would identify the files are gone or going; worse, files orphaned by an
 * *earlier* wipe have no rows at all and would be missed forever. Clearing the
 * directory reclaims those too.
 *
 * Partial downloads (`*.part`) are included for the same reason.
 */
internal fun clearDirectoryContents(dir: File): Int {
    val entries = dir.listFiles() ?: return 0
    return entries.count { entry ->
        if (entry.isDirectory) entry.deleteRecursively() else entry.delete()
    }
}

/**
 * Removes all downloaded audio and cached covers under [filesDir].
 *
 * For callers that hold no reference to [AudioDownloader] or [CoverCache] —
 * currently Room's destructive-migration callback, which runs while the schema
 * is being dropped.
 */
internal fun clearDownloadedContent(filesDir: File): Int =
    clearDirectoryContents(File(filesDir, AUDIO_DIR_NAME)) +
        clearDirectoryContents(File(filesDir, COVERS_DIR_NAME))
