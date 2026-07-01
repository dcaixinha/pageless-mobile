package live.pageless.mobile.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LibrarySortEngineTest {
    @Test
    fun `title and duration support both directions`() {
        val books =
            listOf(
                book("b", "Beta", duration = 100.0),
                book("a", "Alpha", duration = 900.0),
            )

        assertEquals(listOf("a", "b"), LibrarySortEngine.sort(books, LibrarySortState()))
        assertEquals(
            listOf("b", "a"),
            LibrarySortEngine.sort(
                books,
                LibrarySortState(LibrarySort.TITLE, LibrarySortDirection.DESCENDING),
            ),
        )
        assertEquals(
            listOf("a", "b"),
            LibrarySortEngine.sort(
                books,
                LibrarySortState(LibrarySort.DURATION, LibrarySortDirection.DESCENDING),
            ),
        )
    }

    @Test
    fun `author sorting distinguishes first and compound last names`() {
        val books =
            listOf(
                book("zoe", "Zoe Book", authors = listOf("Zoe de la Cruz")),
                book("amy", "Amy Book", authors = listOf("Amy Smith")),
                book("none", "No Author"),
            )

        assertEquals(
            listOf("amy", "zoe", "none"),
            LibrarySortEngine.sort(
                books,
                LibrarySortState(LibrarySort.AUTHOR_FIRST, LibrarySortDirection.ASCENDING),
            ),
        )
        assertEquals(
            listOf("zoe", "amy", "none"),
            LibrarySortEngine.sort(
                books,
                LibrarySortState(LibrarySort.AUTHOR_LAST, LibrarySortDirection.ASCENDING),
            ),
        )
    }

    @Test
    fun `title sorting optionally ignores supported whole-word prefixes`() {
        val books =
            listOf(
                book("apple", "The Apple"),
                book("banana", "Banana"),
                book("zebra", "A Zebra"),
                book("orange", "An Orange"),
                book("theology", "Theology"),
            )

        assertEquals(
            listOf("apple", "banana", "orange", "theology", "zebra"),
            LibrarySortEngine.sort(books, LibrarySortState(), ignorePrefixesWhenSorting = true),
        )
        assertEquals("theology", titleSortKey("Theology", ignorePrefixesWhenSorting = true))
    }

    @Test
    fun `missing metadata sorts last in either direction`() {
        val books =
            listOf(
                book("old", "Older", published = "1990-01-01"),
                book("new", "Newer", published = "2020-01-01"),
                book("missing", "Missing"),
            )

        assertEquals(
            listOf("new", "old", "missing"),
            LibrarySortEngine.sort(
                books,
                LibrarySortState(LibrarySort.PUBLISHED, LibrarySortDirection.DESCENDING),
            ),
        )
    }

    @Test
    fun `progress timestamps and random seeds produce stable orders`() {
        val books =
            listOf(
                book("one", "One", progressUpdated = "2024-01-01T00:00:00Z"),
                book("two", "Two", progressUpdated = "2025-01-01T00:00:00Z"),
                book("three", "Three"),
                book("four", "Four"),
            )

        assertEquals(
            listOf("two", "one", "four", "three"),
            LibrarySortEngine.sort(
                books,
                LibrarySortState(LibrarySort.PROGRESS_UPDATED, LibrarySortDirection.DESCENDING),
            ),
        )

        val first = LibrarySortEngine.sort(books, LibrarySortState(LibrarySort.RANDOM, randomSeed = 1))
        val repeated = LibrarySortEngine.sort(books, LibrarySortState(LibrarySort.RANDOM, randomSeed = 1))
        val reshuffled = LibrarySortEngine.sort(books, LibrarySortState(LibrarySort.RANDOM, randomSeed = 2))
        assertEquals(first, repeated)
        assertNotEquals(first, reshuffled)
    }

    private fun book(
        id: String,
        title: String,
        authors: List<String> = emptyList(),
        published: String? = null,
        duration: Double = 0.0,
        progressUpdated: String? = null,
    ) =
        LibraryBookSortData(
            id = id,
            title = title,
            authorNames = authors,
            publishedDate = published,
            addedAt = null,
            size = null,
            durationSeconds = duration,
            fileModified = null,
            progressUpdatedAt = progressUpdated,
            progressStartedAt = null,
            progressFinishedAt = null,
        )
}
