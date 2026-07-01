package live.pageless.mobile.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTOs mirroring the Pageless server's JSON API (see
 * `PagelessWeb.API.ApiJSON` on the server). Field names must match the server
 * contract exactly.
 */

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String,
    val role: String? = null,
    @SerialName("ignore_prefixes_when_sorting") val ignorePrefixesWhenSorting: Boolean = false,
    @SerialName("date_format") val dateFormat: String = "dd/MM/yyyy",
    @SerialName("time_format") val timeFormat: String = "HH:mm",
)

@Serializable
data class MeResponse(
    val user: UserDto,
    @SerialName("server_version") val serverVersion: String? = null,
)

@Serializable
data class LibrariesResponse(
    val libraries: List<LibraryDto>,
)

@Serializable
data class LibraryDto(
    val id: String,
    val name: String,
)

@Serializable
data class BooksResponse(
    val books: List<BookSummaryDto>,
)

@Serializable
data class BookSummaryDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<AuthorDto> = emptyList(),
    val narrators: List<NarratorDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val series: List<SeriesRefDto> = emptyList(),
    val publisher: PublisherDto? = null,
    val language: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    val size: Long? = null,
    @SerialName("published_year") val publishedYear: Int? = null,
    @SerialName("published_date") val publishedDate: String? = null,
    @SerialName("added_at") val addedAt: String? = null,
    @SerialName("file_modified") val fileModified: String? = null,
    @SerialName("library_id") val libraryId: String? = null,
    @SerialName("has_cover") val hasCover: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
    // Present on home shelves (continue_listening / listen_again); null elsewhere.
    val progress: ProgressDto? = null,
)

@Serializable
data class HomeResponse(
    @SerialName("continue_listening") val continueListening: List<BookSummaryDto> = emptyList(),
    val discover: List<BookSummaryDto> = emptyList(),
    @SerialName("listen_again") val listenAgain: List<BookSummaryDto> = emptyList(),
)

@Serializable
data class BookResponse(
    val book: BookDetailDto,
)

@Serializable
data class BookDetailDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<AuthorDto> = emptyList(),
    val narrators: List<NarratorDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val series: List<SeriesRefDto> = emptyList(),
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    val size: Long? = null,
    @SerialName("published_year") val publishedYear: Int? = null,
    @SerialName("published_date") val publishedDate: String? = null,
    @SerialName("added_at") val addedAt: String? = null,
    @SerialName("file_modified") val fileModified: String? = null,
    @SerialName("library_id") val libraryId: String? = null,
    @SerialName("has_cover") val hasCover: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
    val description: String? = null,
    val publisher: PublisherDto? = null,
    val isbn: String? = null,
    val asin: String? = null,
    val language: String? = null,
    val chapters: List<ChapterDto> = emptyList(),
    @SerialName("audio_files") val audioFiles: List<AudioFileDto> = emptyList(),
    val progress: ProgressDto? = null,
)

/** A series a book belongs to, with this book's sequence within it. */
@Serializable
data class SeriesRefDto(
    val id: String,
    val name: String,
    val sequence: String? = null,
)

@Serializable
data class AuthorDto(
    val id: String,
    val name: String,
)

@Serializable
data class NarratorDto(
    val id: String,
    val name: String,
)

@Serializable
data class GenreDto(
    val id: String,
    val name: String,
)

@Serializable
data class PublisherDto(
    val id: String,
    val name: String,
)

@Serializable
data class ChapterDto(
    val id: String,
    val title: String? = null,
    val index: Int,
    @SerialName("start_seconds") val startSeconds: Double,
    @SerialName("end_seconds") val endSeconds: Double,
)

@Serializable
data class AudioFileDto(
    val id: String,
    val index: Int,
    @SerialName("mime_type") val mimeType: String? = null,
    val size: Long? = null,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
)

@Serializable
data class ProgressResponse(
    val progress: List<ProgressDto>,
)

@Serializable
data class ProgressSyncResponse(
    val progress: ProgressDto,
)

@Serializable
data class ProgressDto(
    @SerialName("book_id") val bookId: String,
    @SerialName("current_seconds") val currentSeconds: Double = 0.0,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    val finished: Boolean = false,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("last_played_at") val lastPlayedAt: String? = null,
    val deleted: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ProgressUpdateRequest(
    @SerialName("current_seconds") val currentSeconds: Double,
    @SerialName("duration_seconds") val durationSeconds: Double,
    @SerialName("last_played_at") val lastPlayedAt: String,
)

@Serializable
data class BookmarksResponse(
    val bookmarks: List<BookmarkDto> = emptyList(),
)

@Serializable
data class BookmarkSyncResponse(
    val bookmark: BookmarkDto,
)

@Serializable
data class BookmarkDto(
    val id: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("position_seconds") val positionSeconds: Double,
    val note: String? = null,
    val deleted: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class BookmarkUpsertRequest(
    @SerialName("book_id") val bookId: String,
    @SerialName("position_seconds") val positionSeconds: Double,
    val note: String? = null,
)

@Serializable
data class ListeningHistorySyncRequest(
    val sessions: List<ListeningSessionSyncDto>,
    val events: List<ListeningEventSyncDto>,
)

@Serializable
data class ListeningHistorySyncResponse(
    val history: ListeningHistoryAckDto,
)

@Serializable
data class ListeningHistoryAckDto(
    val ok: Boolean = true,
)

@Serializable
data class ListeningSessionSyncDto(
    val id: String,
    @SerialName("book_id") val bookId: String,
    val title: String? = null,
    val authors: String? = null,
    @SerialName("play_method") val playMethod: String,
    @SerialName("device_info") val deviceInfo: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("time_listened_seconds") val timeListenedSeconds: Long,
    @SerialName("last_position_seconds") val lastPositionSeconds: Double,
    @SerialName("duration_seconds") val durationSeconds: Double,
)

@Serializable
data class ListeningEventSyncDto(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("book_id") val bookId: String,
    val event: String,
    val type: String,
    @SerialName("position_seconds") val positionSeconds: Double,
    val timestamp: String,
    @SerialName("server_sync_attempted") val serverSyncAttempted: Boolean = false,
    @SerialName("server_sync_success") val serverSyncSuccess: Boolean? = null,
    @SerialName("server_sync_message") val serverSyncMessage: String? = null,
)

// ---------------------------------------------------------------------------
// Series, Collections, Playlists (browse-only mirrors of the server)
// ---------------------------------------------------------------------------

/** A book within a series, carrying its sequence number in that series. */
@Serializable
data class SeriesBookDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<AuthorDto> = emptyList(),
    val narrators: List<NarratorDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val series: List<SeriesRefDto> = emptyList(),
    val publisher: PublisherDto? = null,
    val language: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    val size: Long? = null,
    @SerialName("published_year") val publishedYear: Int? = null,
    @SerialName("published_date") val publishedDate: String? = null,
    @SerialName("added_at") val addedAt: String? = null,
    @SerialName("file_modified") val fileModified: String? = null,
    @SerialName("library_id") val libraryId: String? = null,
    @SerialName("has_cover") val hasCover: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
    val sequence: String? = null,
)

@Serializable
data class SeriesDto(
    val id: String,
    val name: String,
    val books: List<SeriesBookDto> = emptyList(),
)

@Serializable
data class SeriesListResponse(
    val series: List<SeriesDto> = emptyList(),
)

@Serializable
data class SeriesResponse(
    val series: SeriesDto,
)

@Serializable
data class CollectionDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("library_id") val libraryId: String? = null,
    val books: List<BookSummaryDto> = emptyList(),
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class CollectionsResponse(
    val collections: List<CollectionDto> = emptyList(),
)

@Serializable
data class CollectionResponse(
    val collection: CollectionDto,
)

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val books: List<BookSummaryDto> = emptyList(),
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class PlaylistsResponse(
    val playlists: List<PlaylistDto> = emptyList(),
)

@Serializable
data class PlaylistResponse(
    val playlist: PlaylistDto,
)
