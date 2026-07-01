package live.pageless.mobile.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import live.pageless.mobile.BuildConfig
import live.pageless.mobile.data.local.BookDao
import live.pageless.mobile.data.local.BookFacetDao
import live.pageless.mobile.data.local.BookmarkDao
import live.pageless.mobile.data.local.CachedLibraryDao
import live.pageless.mobile.data.local.ChapterDao
import live.pageless.mobile.data.local.CollectionDao
import live.pageless.mobile.data.local.DownloadDao
import live.pageless.mobile.data.local.PagelessDatabase
import live.pageless.mobile.data.local.PlaybackHistoryDao
import live.pageless.mobile.data.local.PlaylistDao
import live.pageless.mobile.data.local.ProgressDao
import live.pageless.mobile.data.local.SeriesDao
import live.pageless.mobile.data.local.SessionStore
import live.pageless.mobile.data.remote.AuthInterceptor
import live.pageless.mobile.data.remote.BaseUrlInterceptor
import live.pageless.mobile.data.remote.PagelessApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideSessionStore(
        @ApplicationContext context: Context,
    ): SessionStore = SessionStore(context)

    @Provides
    @Singleton
    fun provideOkHttp(
        baseUrlInterceptor: BaseUrlInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor(baseUrlInterceptor)
                .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            // Placeholder base URL; BaseUrlInterceptor rewrites host at runtime.
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): PagelessApi = retrofit.create(PagelessApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): PagelessDatabase =
        Room
            .databaseBuilder(context, PagelessDatabase::class.java, "pageless.db")
            // Local cache is re-synced from the server, so a destructive
            // migration on schema changes is acceptable and simplest.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBookDao(db: PagelessDatabase): BookDao = db.bookDao()

    @Provides fun provideBookFacetDao(db: PagelessDatabase): BookFacetDao = db.bookFacetDao()

    @Provides fun provideCachedLibraryDao(db: PagelessDatabase): CachedLibraryDao = db.cachedLibraryDao()

    @Provides fun provideChapterDao(db: PagelessDatabase): ChapterDao = db.chapterDao()

    @Provides fun provideProgressDao(db: PagelessDatabase): ProgressDao = db.progressDao()

    @Provides fun provideDownloadDao(db: PagelessDatabase): DownloadDao = db.downloadDao()

    @Provides fun provideBookmarkDao(db: PagelessDatabase): BookmarkDao = db.bookmarkDao()

    @Provides fun providePlaybackHistoryDao(db: PagelessDatabase): PlaybackHistoryDao = db.playbackHistoryDao()

    @Provides fun provideSeriesDao(db: PagelessDatabase): SeriesDao = db.seriesDao()

    @Provides fun provideCollectionDao(db: PagelessDatabase): CollectionDao = db.collectionDao()

    @Provides fun providePlaylistDao(db: PagelessDatabase): PlaylistDao = db.playlistDao()
}
