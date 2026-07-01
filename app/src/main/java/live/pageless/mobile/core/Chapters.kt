package live.pageless.mobile.core

/**
 * Pure chapter math.
 *
 * DUPLICATED FROM SERVER — keep in sync with `Pageless.Library.Chapters`
 * (`current_index/2`). Any consumer only needs [start] and [end] seconds.
 */
object Chapters {
    /** Minimal shape needed to locate a position within a chapter list. */
    interface Span {
        val startSeconds: Double
        val endSeconds: Double
    }

    /**
     * Returns the zero-based index of the chapter containing [position]
     * (seconds), or null when [chapters] is empty.
     *
     * Chapters are expected contiguous and ordered. A position at/after the end
     * of the last chapter resolves to the last chapter; a position before the
     * first resolves to the first.
     */
    fun currentIndex(
        chapters: List<Span>,
        position: Double,
    ): Int? {
        if (chapters.isEmpty()) return null

        val found = chapters.indexOfFirst { position >= it.startSeconds && position < it.endSeconds }
        if (found >= 0) return found

        return if (position >= chapters.last().endSeconds) chapters.size - 1 else 0
    }
}
