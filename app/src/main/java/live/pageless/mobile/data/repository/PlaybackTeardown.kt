package live.pageless.mobile.data.repository

/**
 * Tears down live playback when an account's local state is being destroyed.
 *
 * Exists so [AuthRepository] can end playback without depending on the playback
 * package. `PlayerConnection` already depends on the repositories, so injecting
 * it here directly would point the dependency arrow both ways; this keeps the
 * data layer depending only on an abstraction it owns.
 *
 * Sign-out is reachable from three separate ViewModels, and switching servers
 * from a fourth place, so the teardown belongs at that single choke point
 * rather than at each call site where the next one added would forget it.
 */
interface PlaybackTeardown {
    /**
     * Stops playback, clears the queue, and drops any state the previous
     * account left in the player and its media notification.
     */
    suspend fun stopAndClearPlayback()
}
