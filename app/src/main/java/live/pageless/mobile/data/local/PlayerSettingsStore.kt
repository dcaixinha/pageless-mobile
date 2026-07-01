package live.pageless.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playerSettingsStore by preferencesDataStore(name = "player_settings")

/** User-configurable playback preferences, mirroring the server's options. */
data class PlayerSettings(
    val useChapterTrack: Boolean = true,
    val jumpForwardSeconds: Int = 30,
    val jumpBackwardSeconds: Int = 15,
    val bookmarkContextSeconds: Int = 0,
    val showTotalTrackOnNowPlaying: Boolean = true,
    val showChapterTrackOnNowPlaying: Boolean = true,
    val showChapterStartOnBookDetail: Boolean = true,
    val showChapterDurationOnBookDetail: Boolean = true,
    val allowSeekFromNotification: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Singleton
class PlayerSettingsStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val USE_CHAPTER_TRACK = booleanPreferencesKey("use_chapter_track")
            val JUMP_FORWARD = intPreferencesKey("jump_forward_seconds")
            val JUMP_BACKWARD = intPreferencesKey("jump_backward_seconds")
            val BOOKMARK_CONTEXT = intPreferencesKey("bookmark_context_seconds")
            val SHOW_TOTAL_TRACK_NOW_PLAYING = booleanPreferencesKey("show_total_track_now_playing")
            val SHOW_CHAPTER_TRACK_NOW_PLAYING = booleanPreferencesKey("show_chapter_track_now_playing")
            val SHOW_CHAPTER_START_BOOK_DETAIL = booleanPreferencesKey("show_chapter_start_book_detail")
            val SHOW_CHAPTER_DURATION_BOOK_DETAIL = booleanPreferencesKey("show_chapter_duration_book_detail")
            val ALLOW_SEEK_FROM_NOTIFICATION = booleanPreferencesKey("allow_seek_from_notification")
            val THEME_MODE = stringPreferencesKey("theme_mode")
        }

        val settings: Flow<PlayerSettings> =
            context.playerSettingsStore.data.map { prefs ->
                PlayerSettings(
                    useChapterTrack = prefs[Keys.USE_CHAPTER_TRACK] ?: true,
                    jumpForwardSeconds = prefs[Keys.JUMP_FORWARD] ?: 30,
                    jumpBackwardSeconds = prefs[Keys.JUMP_BACKWARD] ?: 15,
                    bookmarkContextSeconds = prefs[Keys.BOOKMARK_CONTEXT] ?: 0,
                    showTotalTrackOnNowPlaying = prefs[Keys.SHOW_TOTAL_TRACK_NOW_PLAYING] ?: true,
                    showChapterTrackOnNowPlaying = prefs[Keys.SHOW_CHAPTER_TRACK_NOW_PLAYING] ?: true,
                    showChapterStartOnBookDetail = prefs[Keys.SHOW_CHAPTER_START_BOOK_DETAIL] ?: true,
                    showChapterDurationOnBookDetail = prefs[Keys.SHOW_CHAPTER_DURATION_BOOK_DETAIL] ?: true,
                    allowSeekFromNotification = prefs[Keys.ALLOW_SEEK_FROM_NOTIFICATION] ?: false,
                    themeMode =
                        prefs[Keys.THEME_MODE]?.let { value ->
                            ThemeMode.entries.firstOrNull { it.name == value }
                        } ?: ThemeMode.SYSTEM,
                )
            }

        suspend fun setUseChapterTrack(value: Boolean) {
            context.playerSettingsStore.edit { it[Keys.USE_CHAPTER_TRACK] = value }
        }

        suspend fun setJumpForwardSeconds(value: Int) {
            context.playerSettingsStore.edit { it[Keys.JUMP_FORWARD] = value }
        }

        suspend fun setJumpBackwardSeconds(value: Int) {
            context.playerSettingsStore.edit { it[Keys.JUMP_BACKWARD] = value }
        }

        suspend fun setBookmarkContextSeconds(value: Int) {
            context.playerSettingsStore.edit { it[Keys.BOOKMARK_CONTEXT] = value }
        }

        suspend fun setShowTotalTrackOnNowPlaying(value: Boolean) {
            context.playerSettingsStore.edit { it[Keys.SHOW_TOTAL_TRACK_NOW_PLAYING] = value }
        }

        suspend fun setShowChapterTrackOnNowPlaying(value: Boolean) {
            context.playerSettingsStore.edit { it[Keys.SHOW_CHAPTER_TRACK_NOW_PLAYING] = value }
        }

        suspend fun setShowChapterStartOnBookDetail(value: Boolean) {
            context.playerSettingsStore.edit { it[Keys.SHOW_CHAPTER_START_BOOK_DETAIL] = value }
        }

        suspend fun setShowChapterDurationOnBookDetail(value: Boolean) {
            context.playerSettingsStore.edit { it[Keys.SHOW_CHAPTER_DURATION_BOOK_DETAIL] = value }
        }

        suspend fun setAllowSeekFromNotification(value: Boolean) {
            context.playerSettingsStore.edit { it[Keys.ALLOW_SEEK_FROM_NOTIFICATION] = value }
        }

        suspend fun setThemeMode(value: ThemeMode) {
            context.playerSettingsStore.edit { it[Keys.THEME_MODE] = value.name }
        }
    }
