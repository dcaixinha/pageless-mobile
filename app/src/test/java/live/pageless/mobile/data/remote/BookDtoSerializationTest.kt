package live.pageless.mobile.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BookDtoSerializationTest {
    @Test
    fun `book summary decodes normalized metadata`() {
        val response =
            Json.decodeFromString<BooksResponse>(
                """
                {
                  "books": [{
                    "id": "book",
                    "title": "Book",
                    "narrators": [
                      {"id": "n1", "name": "Primary Reader"},
                      {"id": "n2", "name": "Doe, Jane"}
                    ],
                    "genres": [{"id": "g1", "name": "Fantasy"}],
                    "series": [{"id": "s1", "name": "Saga"}],
                    "publisher": {"id": "p1", "name": "Publisher"},
                    "language": "English",
                    "size": 1234,
                    "added_at": "2025-01-01T00:00:00Z",
                    "file_modified": "2024-01-01T00:00:00Z"
                  }]
                }
                """.trimIndent(),
            )

        assertEquals(
            listOf("Primary Reader", "Doe, Jane"),
            response.books
                .single()
                .narrators
                .map { it.name },
        )
        assertEquals(
            listOf("Fantasy"),
            response.books
                .single()
                .genres
                .map { it.name },
        )
        assertEquals(
            listOf("Saga"),
            response.books
                .single()
                .series
                .map { it.name },
        )
        assertEquals(1234L, response.books.single().size)
        assertEquals("2025-01-01T00:00:00Z", response.books.single().addedAt)
        assertEquals(PublisherDto("p1", "Publisher"), response.books.single().publisher)
        assertEquals("English", response.books.single().language)
    }

    @Test
    fun `detail and series book decode nullable normalized publisher`() {
        val detail =
            Json.decodeFromString<BookDetailDto>(
                """{"id":"detail","title":"Detail","publisher":{"id":"p1","name":"Publisher"}}""",
            )
        val seriesBook =
            Json.decodeFromString<SeriesBookDto>(
                """{"id":"series-book","title":"Series Book","publisher":null}""",
            )

        assertEquals(PublisherDto("p1", "Publisher"), detail.publisher)
        assertEquals(null, seriesBook.publisher)
    }
}
