package live.pageless.mobile.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchEngineTest {
    private val books =
        listOf(
            LibraryBookSearchData(
                id = "title",
                title = "Alpha Book",
                subtitle = null,
                authorNames = emptyList(),
                narratorNames = emptyList(),
                publisherName = null,
            ),
            LibraryBookSearchData(
                id = "subtitle",
                title = "Book",
                subtitle = "Alpha Subtitle",
                authorNames = emptyList(),
                narratorNames = emptyList(),
                publisherName = null,
            ),
            LibraryBookSearchData(
                id = "credits",
                title = "Other",
                subtitle = null,
                authorNames = listOf("Alpha Author"),
                narratorNames = listOf("Alpha Narrator"),
                publisherName = "Alpha Publisher",
            ),
            LibraryBookSearchData(
                id = "facet-only",
                title = "Unrelated",
                subtitle = null,
                authorNames = emptyList(),
                narratorNames = emptyList(),
                publisherName = null,
            ),
        )

    @Test
    fun `one character filters books but does not enable grouped results`() {
        assertEquals(listOf("title", "subtitle", "credits", "facet-only"), LibrarySearchEngine.filterBookIds(books, "a"))
        assertFalse(LibrarySearchEngine.ready("a"))
        assertEquals(emptyList<LibrarySearchFacetGroup>(), LibrarySearchEngine.facetGroups(emptyMap(), "a"))
    }

    @Test
    fun `book matching mirrors web text and credit fields`() {
        assertEquals(listOf("title", "subtitle", "credits"), LibrarySearchEngine.filterBookIds(books, " ALPHA "))
        assertTrue(LibrarySearchEngine.ready(" ALPHA "))
    }

    @Test
    fun `facet groups use web ordering and exclude progress`() {
        val options =
            LibrarySearchEngine.facetCategories.associateWith { category ->
                listOf(LibraryFilterOption("${category.name}-id", "Alpha ${category.label}", 2))
            } +
                (
                    LibraryFilterCategory.PROGRESS to
                        listOf(LibraryFilterOption("finished", "Alpha Finished", 1))
                )

        val groups = LibrarySearchEngine.facetGroups(options, "alpha")

        assertEquals(LibrarySearchEngine.facetCategories, groups.map { it.category })
        assertFalse(groups.any { it.category == LibraryFilterCategory.PROGRESS })
        assertTrue(groups.all { it.items.single().bookCount == 2 })
    }

    @Test
    fun `facet groups cap displayed items without capping totals`() {
        val options =
            mapOf(
                LibraryFilterCategory.AUTHORS to
                    (1..7).map { LibraryFilterOption("author-$it", "Alpha Author $it", it) },
            )

        val group = LibrarySearchEngine.facetGroups(options, "alpha").single()

        assertEquals(7, group.total)
        assertEquals(5, group.items.size)
    }

    @Test
    fun `single facet replacement clears every unrelated selection`() {
        val previous =
            LibraryFilters(
                authorIds = setOf("author"),
                genreIds = setOf("genre"),
                languages = setOf("English"),
            )

        val replaced = LibraryFilters.only(LibraryFilterCategory.COLLECTIONS, "collection")

        assertEquals(3, previous.count)
        assertEquals(setOf("collection"), replaced.collectionIds)
        assertEquals(1, replaced.count)
    }
}
