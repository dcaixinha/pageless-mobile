package live.pageless.mobile.data.remote

import kotlinx.coroutines.runBlocking
import live.pageless.mobile.data.local.SessionStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites each request's scheme/host/port to the user-configured server URL,
 * so the Retrofit base URL is a placeholder and the real host can change at
 * runtime (self-hosted servers vary per user).
 */
@Singleton
class BaseUrlInterceptor
    @Inject
    constructor(
        private val sessionStore: SessionStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val configured = runBlocking { sessionStore.currentServerUrl() }
            val base = configured.toHttpUrlOrNull() ?: return chain.proceed(chain.request())

            val original = chain.request()
            val newUrl =
                original.url
                    .newBuilder()
                    .scheme(base.scheme)
                    .host(base.host)
                    .port(base.port)
                    .build()

            return chain.proceed(original.newBuilder().url(newUrl).build())
        }
    }
