package live.pageless.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import live.pageless.mobile.BuildConfig
import live.pageless.mobile.core.DateTimeFormat
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "session")

/**
 * Persists the API bearer token, server base URL, and current user email.
 *
 * The token is a long-lived credential; on a rooted device DataStore is not a
 * hardware-backed secret store, but it is app-private. Encrypting at rest is a
 * future hardening step (EncryptedSharedPreferences / Keystore).
 */
@Singleton
class SessionStore
    @Inject
    constructor(
        private val context: Context,
    ) {
        private object Keys {
            val TOKEN = stringPreferencesKey("token")
            val SERVER_URL = stringPreferencesKey("server_url")
            val EMAIL = stringPreferencesKey("email")
            val FIRST_NAME = stringPreferencesKey("first_name")
            val IGNORE_PREFIXES_WHEN_SORTING = booleanPreferencesKey("ignore_prefixes_when_sorting")
            val DATE_FORMAT = stringPreferencesKey("date_format")
            val TIME_FORMAT = stringPreferencesKey("time_format")
        }

        val token: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }

        val serverUrl: Flow<String> =
            context.dataStore.data.map { it[Keys.SERVER_URL] ?: BuildConfig.DEFAULT_SERVER_URL }

        val email: Flow<String?> = context.dataStore.data.map { it[Keys.EMAIL] }
        val firstName: Flow<String?> = context.dataStore.data.map { it[Keys.FIRST_NAME] }
        val ignorePrefixesWhenSorting: Flow<Boolean> =
            context.dataStore.data.map { it[Keys.IGNORE_PREFIXES_WHEN_SORTING] ?: false }
        val dateFormat: Flow<String> =
            context.dataStore.data.map { it[Keys.DATE_FORMAT] ?: DateTimeFormat.DEFAULT_DATE_FORMAT }
        val timeFormat: Flow<String> =
            context.dataStore.data.map { it[Keys.TIME_FORMAT] ?: DateTimeFormat.DEFAULT_TIME_FORMAT }

        suspend fun currentToken(): String? = token.first()

        suspend fun currentServerUrl(): String = serverUrl.first()

        suspend fun saveSession(
            token: String,
            email: String,
            firstName: String?,
            serverUrl: String,
            ignorePrefixesWhenSorting: Boolean,
            dateFormat: String,
            timeFormat: String,
        ) {
            context.dataStore.edit {
                it[Keys.TOKEN] = token
                it[Keys.EMAIL] = email
                if (firstName.isNullOrBlank()) {
                    it.remove(Keys.FIRST_NAME)
                } else {
                    it[Keys.FIRST_NAME] = firstName.trim()
                }
                it[Keys.SERVER_URL] = serverUrl
                it[Keys.IGNORE_PREFIXES_WHEN_SORTING] = ignorePrefixesWhenSorting
                it[Keys.DATE_FORMAT] = dateFormat
                it[Keys.TIME_FORMAT] = timeFormat
            }
        }

        suspend fun setUserPreferences(
            ignorePrefixesWhenSorting: Boolean,
            dateFormat: String,
            timeFormat: String,
        ) {
            context.dataStore.edit {
                it[Keys.IGNORE_PREFIXES_WHEN_SORTING] = ignorePrefixesWhenSorting
                it[Keys.DATE_FORMAT] = dateFormat
                it[Keys.TIME_FORMAT] = timeFormat
            }
        }

        suspend fun setServerUrl(serverUrl: String) {
            context.dataStore.edit { it[Keys.SERVER_URL] = serverUrl }
        }

        suspend fun clear() {
            context.dataStore.edit {
                it.remove(Keys.TOKEN)
                it.remove(Keys.EMAIL)
                it.remove(Keys.FIRST_NAME)
                it.remove(Keys.IGNORE_PREFIXES_WHEN_SORTING)
                it.remove(Keys.DATE_FORMAT)
                it.remove(Keys.TIME_FORMAT)
            }
        }
    }
