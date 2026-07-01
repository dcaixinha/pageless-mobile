package live.pageless.mobile.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit interface for the Pageless mobile JSON API. */
interface PagelessApi {
    @POST("api/session")
    suspend fun login(
        @Body body: LoginRequest,
    ): LoginResponse

    @DELETE("api/session")
    suspend fun logout()

    @GET("api/me")
    suspend fun me(): MeResponse

    @GET("api/home")
    suspend fun home(): HomeResponse

    @GET("api/libraries")
    suspend fun libraries(): LibrariesResponse

    @GET("api/books")
    suspend fun books(
        @Query("library_id") libraryId: String? = null,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
    ): BooksResponse

    @GET("api/books/{id}")
    suspend fun book(
        @Path("id") id: String,
    ): BookResponse

    @GET("api/series")
    suspend fun series(): SeriesListResponse

    @GET("api/series/{id}")
    suspend fun seriesDetail(
        @Path("id") id: String,
    ): SeriesResponse

    @GET("api/collections")
    suspend fun collections(): CollectionsResponse

    @GET("api/collections/{id}")
    suspend fun collection(
        @Path("id") id: String,
    ): CollectionResponse

    @GET("api/playlists")
    suspend fun playlists(): PlaylistsResponse

    @GET("api/playlists/{id}")
    suspend fun playlist(
        @Path("id") id: String,
    ): PlaylistResponse

    @GET("api/progress")
    suspend fun progress(
        @Query("since") since: String? = null,
    ): ProgressResponse

    @POST("api/progress/{bookId}")
    suspend fun updateProgress(
        @Path("bookId") bookId: String,
        @Body body: ProgressUpdateRequest,
    ): ProgressSyncResponse

    @GET("api/books/{bookId}/bookmarks")
    suspend fun bookmarksForBook(
        @Path("bookId") bookId: String,
    ): BookmarksResponse

    @GET("api/bookmarks")
    suspend fun bookmarks(
        @Query("since") since: String? = null,
    ): BookmarksResponse

    @PUT("api/bookmarks/{id}")
    suspend fun upsertBookmark(
        @Path("id") id: String,
        @Body body: BookmarkUpsertRequest,
    ): BookmarkSyncResponse

    @DELETE("api/bookmarks/{id}")
    suspend fun deleteBookmark(
        @Path("id") id: String,
    )

    @POST("api/listening-history")
    suspend fun syncListeningHistory(
        @Body body: ListeningHistorySyncRequest,
    ): ListeningHistorySyncResponse
}

/**
 * Builds the download URL for a book's audio file. Fetched with a streaming
 * client (not Retrofit) so it can be written to disk with Range support in
 * Phase 4.
 */
fun bookDownloadUrl(
    baseUrl: String,
    bookId: String,
): String = baseUrl.trimEnd('/') + "/api/books/$bookId/download"

/** URL for a book's cover image (authenticated via the OkHttp interceptor). */
fun bookCoverUrl(
    baseUrl: String,
    bookId: String,
): String = baseUrl.trimEnd('/') + "/api/books/$bookId/cover"
