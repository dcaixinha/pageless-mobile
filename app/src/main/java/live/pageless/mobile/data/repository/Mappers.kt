package live.pageless.mobile.data.repository

import live.pageless.mobile.data.local.BookEntity
import live.pageless.mobile.data.local.BookFacetEntity
import live.pageless.mobile.data.local.ChapterEntity
import live.pageless.mobile.data.local.ProgressEntity
import live.pageless.mobile.data.remote.AuthorDto
import live.pageless.mobile.data.remote.BookDetailDto
import live.pageless.mobile.data.remote.BookSummaryDto
import live.pageless.mobile.data.remote.ChapterDto
import live.pageless.mobile.data.remote.GenreDto
import live.pageless.mobile.data.remote.NarratorDto
import live.pageless.mobile.data.remote.ProgressDto
import live.pageless.mobile.data.remote.PublisherDto
import live.pageless.mobile.data.remote.SeriesBookDto
import live.pageless.mobile.data.remote.SeriesRefDto

/** Conversions between wire DTOs and local Room entities. */

private fun cachedCoverPath(
    existing: BookEntity?,
    hasCover: Boolean,
    updatedAt: String?,
): String? = existing?.takeIf { hasCover && it.coverUpdatedAt == updatedAt }?.coverLocalPath

private fun cachedCoverUpdatedAt(
    existing: BookEntity?,
    hasCover: Boolean,
    updatedAt: String?,
): String? = existing?.takeIf { hasCover && it.coverUpdatedAt == updatedAt }?.coverUpdatedAt

fun BookSummaryDto.toEntity(existing: BookEntity? = null): BookEntity =
    BookEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        authors = authors.joinToString(", ") { it.name }.ifEmpty { null },
        narrators = narrators.joinToString(", ") { it.name }.ifEmpty { null },
        durationSeconds = durationSeconds,
        size = size,
        publishedYear = publishedYear,
        publishedDate = publishedDate,
        addedAt = addedAt,
        fileModified = fileModified,
        libraryId = libraryId,
        hasCover = hasCover,
        coverLocalPath = cachedCoverPath(existing, hasCover, updatedAt),
        coverUpdatedAt = cachedCoverUpdatedAt(existing, hasCover, updatedAt),
        description = existing?.description,
        publisher = publisher?.name,
        language = language,
        updatedAt = updatedAt,
    )

fun BookDetailDto.toEntity(existing: BookEntity? = null): BookEntity =
    BookEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        authors = authors.joinToString(", ") { it.name }.ifEmpty { null },
        narrators = narrators.joinToString(", ") { it.name }.ifEmpty { null },
        durationSeconds = durationSeconds,
        size = size,
        publishedYear = publishedYear,
        publishedDate = publishedDate,
        addedAt = addedAt,
        fileModified = fileModified,
        libraryId = libraryId,
        hasCover = hasCover,
        coverLocalPath = cachedCoverPath(existing, hasCover, updatedAt),
        coverUpdatedAt = cachedCoverUpdatedAt(existing, hasCover, updatedAt),
        description = description ?: existing?.description,
        publisher = publisher?.name,
        language = language,
        updatedAt = updatedAt,
    )

fun SeriesBookDto.toEntity(existing: BookEntity? = null): BookEntity =
    BookEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        authors = authors.joinToString(", ") { it.name }.ifEmpty { null },
        narrators = narrators.joinToString(", ") { it.name }.ifEmpty { null },
        durationSeconds = durationSeconds,
        size = size,
        publishedYear = publishedYear,
        publishedDate = publishedDate,
        addedAt = addedAt,
        fileModified = fileModified,
        libraryId = libraryId,
        hasCover = hasCover,
        coverLocalPath = cachedCoverPath(existing, hasCover, updatedAt),
        coverUpdatedAt = cachedCoverUpdatedAt(existing, hasCover, updatedAt),
        description = existing?.description,
        publisher = publisher?.name,
        language = language,
        updatedAt = updatedAt,
    )

fun BookSummaryDto.toFacetEntities(): List<BookFacetEntity> =
    facetEntities(id, authors, narrators, genres, series, publisher, language)

fun BookDetailDto.toFacetEntities(): List<BookFacetEntity> =
    facetEntities(id, authors, narrators, genres, series, publisher, language)

fun SeriesBookDto.toFacetEntities(): List<BookFacetEntity> =
    facetEntities(id, authors, narrators, genres, series, publisher, language)

private fun facetEntities(
    bookId: String,
    authors: List<AuthorDto>,
    narrators: List<NarratorDto>,
    genres: List<GenreDto>,
    series: List<SeriesRefDto>,
    publisher: PublisherDto?,
    language: String?,
): List<BookFacetEntity> =
    facets(bookId, "author", authors.map { it.id to it.name }) +
        facets(bookId, "narrator", narrators.map { it.id to it.name }) +
        facets(bookId, "genre", genres.map { it.id to it.name }) +
        facets(bookId, "series", series.map { it.id to it.name }) +
        facets(bookId, "publisher", listOfNotNull(publisher?.let { it.id to it.name })) +
        facets(bookId, "language", listOfNotNull(language?.takeIf { it.isNotBlank() }?.let { it to it }))

private fun facets(
    bookId: String,
    category: String,
    values: List<Pair<String, String>>,
): List<BookFacetEntity> =
    values.mapIndexed { index, (id, name) ->
        BookFacetEntity(
            bookId = bookId,
            category = category,
            facetId = id,
            name = name,
            position = index,
        )
    }

fun ChapterDto.toEntity(bookId: String): ChapterEntity =
    ChapterEntity(
        id = id,
        bookId = bookId,
        title = title,
        index = index,
        startSeconds = startSeconds,
        endSeconds = endSeconds,
    )

fun ProgressDto.toEntity(dirty: Boolean = false): ProgressEntity =
    ProgressEntity(
        bookId = bookId,
        currentSeconds = currentSeconds,
        durationSeconds = durationSeconds,
        finished = finished,
        startedAt = startedAt,
        finishedAt = finishedAt,
        lastPlayedAt = lastPlayedAt,
        updatedAt = updatedAt,
        dirty = dirty,
    )
