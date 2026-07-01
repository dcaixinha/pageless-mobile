package live.pageless.mobile.core

/**
 * Pure, layer-agnostic playback rules.
 *
 * DUPLICATED FROM SERVER — keep in sync with the Elixir reference:
 *   - `Pageless.Playback.finished_threshold/0` + `finished_at_position?/2`
 *
 * These rules must match the server exactly so both ends agree on when a book
 * counts as finished during progress sync. The mirrored unit tests in
 * `PlaybackRulesTest` cover the same cases as the server's doctests.
 */
object PlaybackRules {
    /** Fraction of a book's duration at which it is considered finished. */
    const val FINISHED_THRESHOLD: Double = 0.98

    /**
     * Returns true when [currentSeconds] counts as "finished" for a book of the
     * given [durationSeconds] (at or past [FINISHED_THRESHOLD] of the way through).
     */
    fun finishedAtPosition(
        currentSeconds: Double,
        durationSeconds: Double,
    ): Boolean = durationSeconds > 0 && currentSeconds / durationSeconds >= FINISHED_THRESHOLD
}
