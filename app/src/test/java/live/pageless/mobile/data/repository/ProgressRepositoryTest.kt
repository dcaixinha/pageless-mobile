package live.pageless.mobile.data.repository

import live.pageless.mobile.data.local.ProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRepositoryTest {
    @Test
    fun `recording preserves started timestamp and clears finished timestamp when unfinished`() {
        val existing = progress(startedAt = "started", finishedAt = "finished", finished = true)

        val updated = updatedProgress(existing, "book", 100.0, 1000.0, "now")

        assertEquals("started", updated.startedAt)
        assertNull(updated.finishedAt)
        assertFalse(updated.finished)
        assertTrue(updated.dirty)
    }

    @Test
    fun `recording derives a first finished timestamp`() {
        val existing = progress(startedAt = "started", finishedAt = null, finished = false)

        val updated = updatedProgress(existing, "book", 990.0, 1000.0, "now")

        assertEquals("started", updated.startedAt)
        assertEquals("now", updated.finishedAt)
        assertTrue(updated.finished)
    }

    private fun progress(
        startedAt: String?,
        finishedAt: String?,
        finished: Boolean,
    ) = ProgressEntity(
        bookId = "book",
        currentSeconds = 100.0,
        durationSeconds = 1000.0,
        finished = finished,
        startedAt = startedAt,
        finishedAt = finishedAt,
        lastPlayedAt = "old",
        updatedAt = "old",
    )
}
