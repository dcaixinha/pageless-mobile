package live.pageless.mobile.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterEngineTest {
    private val books =
        listOf(
            book(
                id = "one",
                authors = setOf("author-a"),
                narrators = setOf("narrator-a"),
                genres = setOf("fantasy"),
                series = setOf("series-a"),
                collections = setOf("collection-a"),
                playlists = setOf("playlist-a"),
                publishers = setOf("publisher-a"),
                languages = setOf("English"),
                library = "library-a",
                progress = LibraryProgressState.NOT_STARTED,
            ),
            book(
                id = "two",
                authors = setOf("author-b"),
                narrators = setOf("narrator-a"),
                genres = setOf("history"),
                series = setOf("series-b"),
                collections = setOf("collection-b"),
                playlists = setOf("playlist-b"),
                publishers = setOf("publisher-b"),
                languages = setOf("Portuguese"),
                library = "library-a",
                progress = LibraryProgressState.IN_PROGRESS,
            ),
            book(
                id = "three",
                authors = setOf("author-c"),
                narrators = setOf("narrator-c"),
                genres = setOf("fantasy"),
                series = setOf("series-a"),
                collections = setOf("collection-a"),
                playlists = setOf("playlist-c"),
                publishers = setOf("publisher-a"),
                languages = setOf("English"),
                library = "library-b",
                progress = LibraryProgressState.FINISHED,
            ),
        )

    @Test
    fun `empty filters include every book`() {
        assertEquals(listOf("one", "two", "three"), LibraryFilterEngine.filter(books, LibraryFilters()))
    }

    @Test
    fun `values are ORed within a category`() {
        val filters = LibraryFilters(authorIds = setOf("author-a", "author-b"))

        assertEquals(listOf("one", "two"), LibraryFilterEngine.filter(books, filters))
    }

    @Test
    fun `categories are ANDed together`() {
        val filters =
            LibraryFilters(
                narratorIds = setOf("narrator-a"),
                genreIds = setOf("fantasy"),
                seriesIds = setOf("series-a"),
                collectionIds = setOf("collection-a"),
                playlistIds = setOf("playlist-a"),
                publisherIds = setOf("publisher-a"),
                libraryIds = setOf("library-a"),
            )

        assertEquals(listOf("one"), LibraryFilterEngine.filter(books, filters))
    }

    @Test
    fun `publisher filter matches the singular publisher facet`() {
        val filters = LibraryFilters(publisherIds = setOf("publisher-b"))

        assertEquals(listOf("two"), LibraryFilterEngine.filter(books, filters))
    }

    @Test
    fun `language filter matches its free form value`() {
        val filters = LibraryFilters(languages = setOf("Portuguese"))

        assertEquals(listOf("two"), LibraryFilterEngine.filter(books, filters))
    }

    @Test
    fun `collection and playlist filters use membership ids`() {
        val filters =
            LibraryFilters(
                collectionIds = setOf("collection-a", "collection-b"),
                playlistIds = setOf("playlist-c"),
            )

        assertEquals(listOf("three"), LibraryFilterEngine.filter(books, filters))
    }

    @Test
    fun `progress filters distinguish every offline state`() {
        val filters =
            LibraryFilters(
                progressStates =
                    setOf(
                        LibraryProgressState.IN_PROGRESS,
                        LibraryProgressState.FINISHED,
                    ),
            )

        assertEquals(listOf("two", "three"), LibraryFilterEngine.filter(books, filters))
    }

    @Test
    fun `toggle and clear maintain the active value count`() {
        val selected =
            LibraryFilters()
                .toggle(LibraryFilterCategory.NARRATORS, "narrator-a")
                .toggle(LibraryFilterCategory.PROGRESS, "finished")

        assertEquals(2, selected.count)
        assertEquals(1, selected.clear(LibraryFilterCategory.NARRATORS).count)
    }

    @Test
    fun `navigation arguments initialize exactly one requested filter`() {
        val filters = initialFilters("COLLECTIONS", "collection-a")

        assertEquals(setOf("collection-a"), filters.collectionIds)
        assertEquals(1, filters.count)
        assertEquals(LibraryFilters(), initialFilters("unknown", "collection-a"))
    }

    private fun book(
        id: String,
        authors: Set<String>,
        narrators: Set<String>,
        genres: Set<String>,
        series: Set<String>,
        collections: Set<String>,
        playlists: Set<String>,
        publishers: Set<String>,
        languages: Set<String>,
        library: String,
        progress: LibraryProgressState,
    ) = LibraryBookFilterData(
        id = id,
        authorIds = authors,
        narratorIds = narrators,
        genreIds = genres,
        seriesIds = series,
        collectionIds = collections,
        playlistIds = playlists,
        publisherIds = publishers,
        languages = languages,
        libraryId = library,
        progressState = progress,
    )
}
