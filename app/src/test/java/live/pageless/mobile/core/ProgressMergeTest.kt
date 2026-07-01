package live.pageless.mobile.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors the server's last-write-wins rule in `Pageless.Playback.upsert_progress/3`. */
class ProgressMergeTest {
    private class Ts(
        override val lastPlayedAt: String?,
    ) : ProgressMerge.Timestamped

    @Test
    fun incomingWinsWhenNoCurrent() {
        assertTrue(ProgressMerge.incomingWins(null, Ts("2026-01-01T00:00:00Z")))
    }

    @Test
    fun incomingWinsWhenCurrentHasNoTimestamp() {
        assertTrue(ProgressMerge.incomingWins(Ts(null), Ts("2026-01-01T00:00:00Z")))
    }

    @Test
    fun incomingWinsWhenStrictlyNewer() {
        assertTrue(
            ProgressMerge.incomingWins(
                Ts("2026-01-01T00:00:00Z"),
                Ts("2026-01-02T00:00:00Z"),
            ),
        )
    }

    @Test
    fun incomingWinsWhenEqual() {
        // Equal timestamps: incoming (server) wins by >= to converge state.
        assertTrue(
            ProgressMerge.incomingWins(
                Ts("2026-01-01T00:00:00Z"),
                Ts("2026-01-01T00:00:00Z"),
            ),
        )
    }

    @Test
    fun staleIncomingLoses() {
        assertFalse(
            ProgressMerge.incomingWins(
                Ts("2026-01-02T00:00:00Z"),
                Ts("2026-01-01T00:00:00Z"),
            ),
        )
    }

    @Test
    fun incomingWithNoTimestampLosesToStoredValue() {
        assertFalse(ProgressMerge.incomingWins(Ts("2026-01-01T00:00:00Z"), Ts(null)))
    }
}
