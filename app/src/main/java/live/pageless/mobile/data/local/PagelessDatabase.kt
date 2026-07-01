package live.pageless.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        BookFacetEntity::class,
        CachedLibraryEntity::class,
        ChapterEntity::class,
        ProgressEntity::class,
        DownloadEntity::class,
        BookmarkEntity::class,
        PlaybackSessionEntity::class,
        PlaybackEventEntity::class,
        SeriesEntity::class,
        SeriesBookEntity::class,
        CollectionEntity::class,
        CollectionBookEntity::class,
        PlaylistEntity::class,
        PlaylistBookEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class PagelessDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun bookFacetDao(): BookFacetDao

    abstract fun cachedLibraryDao(): CachedLibraryDao

    abstract fun chapterDao(): ChapterDao

    abstract fun progressDao(): ProgressDao

    abstract fun downloadDao(): DownloadDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    abstract fun seriesDao(): SeriesDao

    abstract fun collectionDao(): CollectionDao

    abstract fun playlistDao(): PlaylistDao
}
