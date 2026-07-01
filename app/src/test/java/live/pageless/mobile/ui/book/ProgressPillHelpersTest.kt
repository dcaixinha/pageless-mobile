package live.pageless.mobile.ui.book

import live.pageless.mobile.data.local.ProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressPillHelpersTest {
    private fun progress(
        current: Double = 0.0,
        duration: Double = 1000.0,
        finished: Boolean = false,
    ) = ProgressEntity(
        bookId = "b",
        currentSeconds = current,
        durationSeconds = duration,
        finished = finished,
        startedAt = null,
        finishedAt = null,
        lastPlayedAt = null,
        updatedAt = null,
    )

    @Test
    fun percent_is_full_when_finished_even_without_position() {
        // Imported-as-finished case: finished but no listening position.
        assertEquals(100, progressPercent(progress(current = 0.0, finished = true), false, 0L))
    }

    @Test
    fun percent_reflects_saved_position_when_not_active() {
        assertEquals(25, progressPercent(progress(current = 250.0), false, 0L))
    }

    @Test
    fun percent_uses_live_player_position_when_active() {
        assertEquals(50, progressPercent(progress(current = 0.0), true, 500_000L))
    }

    @Test
    fun remaining_is_null_when_finished() {
        assertEquals(null, remainingLabel(progress(finished = true), 1000.0, false, 0L))
    }

    @Test
    fun remaining_computes_from_saved_position() {
        assertEquals("12m remaining", remainingLabel(progress(current = 250.0), 1000.0, false, 0L))
    }
}
