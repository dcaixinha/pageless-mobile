package live.pageless.mobile.data.repository

import live.pageless.mobile.data.remote.AuthorDto
import live.pageless.mobile.data.remote.BookDetailDto
import live.pageless.mobile.data.remote.BookSummaryDto
import live.pageless.mobile.data.remote.GenreDto
import live.pageless.mobile.data.remote.NarratorDto
import live.pageless.mobile.data.remote.PublisherDto
import live.pageless.mobile.data.remote.SeriesRefDto
import org.junit.Assert.assertEquals
import org.junit.Test

class BookMetadataMapperTest {
    @Test
    fun `structured metadata preserves display order and facet ids`() {
        val dto =
            BookSummaryDto(
                id = "book",
                title = "Book",
                authors = listOf(AuthorDto("author", "Author")),
                narrators =
                    listOf(
                        NarratorDto("narrator-1", "Primary Reader"),
                        NarratorDto("narrator-2", "Doe, Jane"),
                    ),
                genres = listOf(GenreDto("genre", "Fantasy")),
                series = listOf(SeriesRefDto("series", "Saga")),
                publisher = PublisherDto("publisher", "Publisher"),
                language = "English",
                size = 1234,
                addedAt = "2025-01-01T00:00:00Z",
                fileModified = "2024-01-01T00:00:00Z",
            )

        assertEquals("Primary Reader, Doe, Jane", dto.toEntity().narrators)
        assertEquals("Publisher", dto.toEntity().publisher)
        assertEquals(1234L, dto.toEntity().size)
        assertEquals("2025-01-01T00:00:00Z", dto.toEntity().addedAt)
        assertEquals(
            listOf("author", "narrator-1", "narrator-2", "genre", "series", "publisher", "English"),
            dto.toFacetEntities().map { it.facetId },
        )
        assertEquals(listOf(0, 0, 1, 0, 0, 0, 0), dto.toFacetEntities().map { it.position })
        assertEquals("publisher", dto.toFacetEntities()[5].category)
        assertEquals("language", dto.toFacetEntities().last().category)
    }

    @Test
    fun `detail publisher maps to the entity display string`() {
        val dto =
            BookDetailDto(
                id = "book",
                title = "Book",
                publisher = PublisherDto("publisher", "Publisher"),
            )

        assertEquals("Publisher", dto.toEntity().publisher)
    }
}
