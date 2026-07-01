package live.pageless.mobile.core

/**
 * Pure last-write-wins merge for playback progress, mirroring the server's
 * `Pageless.Playback.upsert_progress/3` conflict rule (newer `lastPlayedAt`
 * wins). Kept pure so it is unit-testable without Room/network.
 *
 * Timestamps are compared as ISO-8601 strings; because ISO-8601 UTC timestamps
 * are lexicographically ordered by time, string comparison is sufficient when
 * both sides are produced in UTC (as the server and this app both are).
 */
object ProgressMerge {
    interface Timestamped {
        val lastPlayedAt: String?
    }

    /**
     * Returns true when [incoming] should replace [current] (i.e. it is newer or
     * there is no current record). A null [current] always accepts incoming; a
     * null incoming timestamp never wins over a non-null stored one.
     */
    fun incomingWins(
        current: Timestamped?,
        incoming: Timestamped,
    ): Boolean {
        if (current == null) return true
        val currentTs = current.lastPlayedAt ?: return true
        val incomingTs = incoming.lastPlayedAt ?: return false
        return incomingTs >= currentTs
    }
}
