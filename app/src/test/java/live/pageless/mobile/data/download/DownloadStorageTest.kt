package live.pageless.mobile.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadStorageTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun `clearing a directory removes its contents but keeps the directory`() {
        val dir = temp.newFolder("audiobooks")
        File(dir, "a.m4b").writeText("audio")
        File(dir, "b.m4b").writeText("audio")

        val removed = clearDirectoryContents(dir)

        assertEquals(2, removed)
        assertTrue(dir.exists())
        assertEquals(0, dir.listFiles()!!.size)
    }

    @Test
    fun `clearing removes partial downloads too`() {
        val dir = temp.newFolder("audiobooks")
        File(dir, "book.m4b.part").writeText("half a download")

        clearDirectoryContents(dir)

        // A cancelled download leaves a .part file behind. It is as much dead
        // weight as a finished one, and nothing in Room ever referenced it.
        assertEquals(0, dir.listFiles()!!.size)
    }

    @Test
    fun `clearing a missing directory is a no-op rather than an error`() {
        // Neither directory exists until the first download or cover fetch, so
        // signing out of an account that never downloaded anything must not fail.
        val absent = File(temp.root, "never-created")

        assertEquals(0, clearDirectoryContents(absent))
        assertFalse(absent.exists())
    }

    @Test
    fun `clearing recurses into nested directories`() {
        val dir = temp.newFolder("covers")
        val nested = File(dir, "nested").apply { mkdirs() }
        File(nested, "cover.jpg").writeText("image")

        clearDirectoryContents(dir)

        assertEquals(0, dir.listFiles()!!.size)
    }

    @Test
    fun `clearing downloaded content removes both audio and covers`() {
        val filesDir = temp.newFolder("files")
        val audio = File(filesDir, AUDIO_DIR_NAME).apply { mkdirs() }
        val covers = File(filesDir, COVERS_DIR_NAME).apply { mkdirs() }
        File(audio, "book.m4b").writeText("audio")
        File(covers, "book.jpg").writeText("image")

        val removed = clearDownloadedContent(filesDir)

        assertEquals(2, removed)
        assertEquals(0, audio.listFiles()!!.size)
        assertEquals(0, covers.listFiles()!!.size)
    }

    @Test
    fun `clearing downloaded content leaves unrelated app files alone`() {
        val filesDir = temp.newFolder("files")
        File(filesDir, AUDIO_DIR_NAME).apply { mkdirs() }
        val datastore = File(filesDir, "datastore").apply { mkdirs() }
        val prefs = File(datastore, "session.preferences_pb").apply { writeText("token") }

        clearDownloadedContent(filesDir)

        // Only the two download directories are in scope. The session store lives
        // under filesDir too and is cleared separately, by SessionStore.clear().
        assertTrue(prefs.exists())
    }
}
