package live.pageless.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors the server's `Pageless.Playback.finished_at_position?/2` doctests. */
class PlaybackRulesTest {
    @Test
    fun finishedAtThreshold() {
        assertTrue(PlaybackRules.finishedAtPosition(990.0, 1000.0))
    }

    @Test
    fun notFinishedMidway() {
        assertFalse(PlaybackRules.finishedAtPosition(500.0, 1000.0))
    }

    @Test
    fun zeroDurationIsNeverFinished() {
        assertFalse(PlaybackRules.finishedAtPosition(10.0, 0.0))
    }

    @Test
    fun thresholdMatchesServer() {
        assertEquals(0.98, PlaybackRules.FINISHED_THRESHOLD, 0.0)
    }
}
