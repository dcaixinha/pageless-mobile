package live.pageless.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight projection for building index cover mosaics: a membership row
 * (which parent it belongs to, ordered) plus the book's cover fields.
 */
data class MemberCoverRow(
    val parentId: String,
    val position: Int,
    val bookId: String,
    val hasCover: Boolean,
    val coverLocalPath: String?,
    val coverUpdatedAt: String?,
    val updatedAt: String?,
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE libraryId = :libraryId ORDER BY title COLLATE NOCASE")
    fun observeByLibrary(libraryId: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observe(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun get(id: String): BookEntity?

    @Upsert
    suspend fun upsertAll(books: List<BookEntity>)

    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query("DELETE FROM books")
    suspend fun deleteAll()
}

@Dao
interface BookFacetDao {
    @Query("SELECT * FROM book_facets ORDER BY category, name COLLATE NOCASE, position")
    fun observeAll(): Flow<List<BookFacetEntity>>

    @Query("SELECT * FROM book_facets WHERE bookId = :bookId ORDER BY category, position")
    fun observeForBook(bookId: String): Flow<List<BookFacetEntity>>

    @Query("DELETE FROM book_facets")
    suspend fun deleteAll()

    @Query("DELETE FROM book_facets WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(facets: List<BookFacetEntity>)

    @Transaction
    suspend fun replaceAll(facets: List<BookFacetEntity>) {
        deleteAll()
        insertAll(facets)
    }

    @Transaction
    suspend fun replaceForBook(
        bookId: String,
        facets: List<BookFacetEntity>,
    ) {
        deleteForBook(bookId)
        insertAll(facets)
    }
}

@Dao
interface CachedLibraryDao {
    @Query("SELECT * FROM cached_libraries ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CachedLibraryEntity>>

    @Query("DELETE FROM cached_libraries")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(libraries: List<CachedLibraryEntity>)

    @Transaction
    suspend fun replaceAll(libraries: List<CachedLibraryEntity>) {
        deleteAll()
        insertAll(libraries)
    }
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index`")
    fun observeForBook(bookId: String): Flow<List<ChapterEntity>>

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    suspend fun replaceForBook(
        bookId: String,
        chapters: List<ChapterEntity>,
    ) {
        deleteForBook(bookId)
        insertAll(chapters)
    }
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE bookId = :bookId")
    fun observe(bookId: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE bookId = :bookId")
    suspend fun get(bookId: String): ProgressEntity?

    @Query("SELECT * FROM progress WHERE dirty = 1")
    suspend fun dirty(): List<ProgressEntity>

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE bookId = :bookId")
    suspend fun delete(bookId: String)

    @Query("UPDATE progress SET dirty = 0 WHERE bookId = :bookId")
    suspend fun clearDirty(bookId: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE bookId = :bookId")
    fun observe(bookId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE completed = 1")
    fun observeCompleted(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE bookId = :bookId")
    suspend fun get(bookId: String): DownloadEntity?

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE bookId = :bookId")
    suspend fun delete(bookId: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND deleted = 0 ORDER BY positionSeconds")
    fun observeForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE dirty = 1 AND deleted = 0")
    suspend fun dirty(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE deleted = 1")
    suspend fun tombstones(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun get(id: String): BookmarkEntity?

    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("UPDATE bookmarks SET dirty = 0 WHERE id = :id")
    suspend fun clearDirty(id: String)

    @Query("UPDATE bookmarks SET deleted = 1, dirty = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun hardDelete(id: String)
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    fun observe(id: String): Flow<SeriesEntity?>

    // Books in a series, ordered by the stored position (numeric sequence order).
    @Query(
        """
        SELECT b.* FROM books b
        INNER JOIN series_books sb ON sb.bookId = b.id
        WHERE sb.seriesId = :seriesId
        ORDER BY sb.position
        """,
    )
    fun observeBooks(seriesId: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM series_books WHERE seriesId = :seriesId ORDER BY position")
    fun observeMembers(seriesId: String): Flow<List<SeriesBookEntity>>

    // Preview rows for the index: series joined to their books (ordered), used
    // to build cover mosaics without an N+1 of per-series queries.
    @Query(
        """
        SELECT sb.seriesId AS parentId, sb.position AS position,
               b.id AS bookId, b.hasCover AS hasCover,
               b.coverLocalPath AS coverLocalPath, b.coverUpdatedAt AS coverUpdatedAt,
               b.updatedAt AS updatedAt
        FROM series_books sb
        INNER JOIN books b ON b.id = sb.bookId
        ORDER BY sb.seriesId, sb.position
        """,
    )
    fun observeMemberPreviews(): Flow<List<MemberCoverRow>>

    @Upsert
    suspend fun upsert(series: SeriesEntity)

    @Upsert
    suspend fun upsertAll(series: List<SeriesEntity>)

    @Query("DELETE FROM series_books WHERE seriesId = :seriesId")
    suspend fun deleteMembers(seriesId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<SeriesBookEntity>)

    suspend fun replaceMembers(
        seriesId: String,
        members: List<SeriesBookEntity>,
    ) {
        deleteMembers(seriesId)
        insertMembers(members)
    }
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observe(id: String): Flow<CollectionEntity?>

    @Query("SELECT * FROM collection_books ORDER BY collectionId, position")
    fun observeAllMembers(): Flow<List<CollectionBookEntity>>

    @Query(
        """
        SELECT b.* FROM books b
        INNER JOIN collection_books cb ON cb.bookId = b.id
        WHERE cb.collectionId = :collectionId
        ORDER BY cb.position
        """,
    )
    fun observeBooks(collectionId: String): Flow<List<BookEntity>>

    @Query(
        """
        SELECT cb.collectionId AS parentId, cb.position AS position,
               b.id AS bookId, b.hasCover AS hasCover,
               b.coverLocalPath AS coverLocalPath, b.coverUpdatedAt AS coverUpdatedAt,
               b.updatedAt AS updatedAt
        FROM collection_books cb
        INNER JOIN books b ON b.id = cb.bookId
        ORDER BY cb.collectionId, cb.position
        """,
    )
    fun observeMemberPreviews(): Flow<List<MemberCoverRow>>

    @Upsert
    suspend fun upsert(collection: CollectionEntity)

    @Upsert
    suspend fun upsertAll(collections: List<CollectionEntity>)

    @Query("DELETE FROM collection_books WHERE collectionId = :collectionId")
    suspend fun deleteMembers(collectionId: String)

    @Query("DELETE FROM collection_books")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM collections")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<CollectionBookEntity>)

    suspend fun replaceMembers(
        collectionId: String,
        members: List<CollectionBookEntity>,
    ) {
        deleteMembers(collectionId)
        insertMembers(members)
    }

    @Transaction
    suspend fun replaceAll(
        collections: List<CollectionEntity>,
        members: List<CollectionBookEntity>,
    ) {
        deleteAllMembers()
        deleteAll()
        upsertAll(collections)
        insertMembers(members)
    }
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observe(id: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlist_books ORDER BY playlistId, position")
    fun observeAllMembers(): Flow<List<PlaylistBookEntity>>

    @Query(
        """
        SELECT b.* FROM books b
        INNER JOIN playlist_books pb ON pb.bookId = b.id
        WHERE pb.playlistId = :playlistId
        ORDER BY pb.position
        """,
    )
    fun observeBooks(playlistId: String): Flow<List<BookEntity>>

    @Query(
        """
        SELECT pb.playlistId AS parentId, pb.position AS position,
               b.id AS bookId, b.hasCover AS hasCover,
               b.coverLocalPath AS coverLocalPath, b.coverUpdatedAt AS coverUpdatedAt,
               b.updatedAt AS updatedAt
        FROM playlist_books pb
        INNER JOIN books b ON b.id = pb.bookId
        ORDER BY pb.playlistId, pb.position
        """,
    )
    fun observeMemberPreviews(): Flow<List<MemberCoverRow>>

    @Upsert
    suspend fun upsert(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertAll(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlist_books WHERE playlistId = :playlistId")
    suspend fun deleteMembers(playlistId: String)

    @Query("DELETE FROM playlist_books")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM playlists")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<PlaylistBookEntity>)

    suspend fun replaceMembers(
        playlistId: String,
        members: List<PlaylistBookEntity>,
    ) {
        deleteMembers(playlistId)
        insertMembers(members)
    }

    @Transaction
    suspend fun replaceAll(
        playlists: List<PlaylistEntity>,
        members: List<PlaylistBookEntity>,
    ) {
        deleteAllMembers()
        deleteAll()
        upsertAll(playlists)
        insertMembers(members)
    }
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_sessions WHERE bookId = :bookId ORDER BY startedAt DESC")
    fun observeSessionsForBook(bookId: String): Flow<List<PlaybackSessionEntity>>

    @Query("SELECT * FROM playback_events WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun observeEventsForBook(bookId: String): Flow<List<PlaybackEventEntity>>

    @Query("SELECT * FROM playback_sessions WHERE id = :id")
    suspend fun getSession(id: String): PlaybackSessionEntity?

    @Query("SELECT * FROM playback_sessions WHERE dirty = 1")
    suspend fun dirtySessions(): List<PlaybackSessionEntity>

    @Query("SELECT * FROM playback_events WHERE dirty = 1")
    suspend fun dirtyEvents(): List<PlaybackEventEntity>

    @Upsert
    suspend fun upsertSession(session: PlaybackSessionEntity)

    @Upsert
    suspend fun upsertEvent(event: PlaybackEventEntity)

    @Query("UPDATE playback_sessions SET dirty = 0 WHERE id IN (:ids)")
    suspend fun clearSessionDirty(ids: List<String>)

    @Query("UPDATE playback_events SET dirty = 0 WHERE id IN (:ids)")
    suspend fun clearEventDirty(ids: List<String>)
}
