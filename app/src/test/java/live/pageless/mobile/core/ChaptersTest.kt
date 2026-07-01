package live.pageless.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mirrors the server's `Pageless.Library.Chapters.current_index/2` tests. */
class ChaptersTest {
    private data class Ch(
        override val startSeconds: Double,
        override val endSeconds: Double,
    ) : Chapters.Span

    private val chapters =
        listOf(
            Ch(0.0, 500.0),
            Ch(500.0, 1000.0),
            Ch(1000.0, 1500.0),
        )

    @Test
    fun nullForEmpty() {
        assertNull(Chapters.currentIndex(emptyList(), 42.0))
    }

    @Test
    fun findsContainingChapter() {
        assertEquals(0, Chapters.currentIndex(chapters, 0.0))
        assertEquals(0, Chapters.currentIndex(chapters, 499.9))
        assertEquals(1, Chapters.currentIndex(chapters, 500.0))
        assertEquals(2, Chapters.currentIndex(chapters, 1200.0))
    }

    @Test
    fun clampsPastEndToLast() {
        assertEquals(2, Chapters.currentIndex(chapters, 5000.0))
    }

    @Test
    fun clampsBeforeStartToFirst() {
        assertEquals(0, Chapters.currentIndex(chapters, -10.0))
    }
}
