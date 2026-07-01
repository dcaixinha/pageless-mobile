package live.pageless.mobile.data.remote

import kotlinx.coroutines.runBlocking
import live.pageless.mobile.data.local.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds the `Authorization: Bearer <token>` header to outbound requests when a
 * token is present. The public login endpoint carries no token, which the
 * server accepts.
 */
@Singleton
class AuthInterceptor
    @Inject
    constructor(
        private val sessionStore: SessionStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = runBlocking { sessionStore.currentToken() }
            val request = chain.request()
            val authed =
                if (token.isNullOrBlank()) {
                    request
                } else {
                    request.newBuilder().header("Authorization", "Bearer $token").build()
                }
            return chain.proceed(authed)
        }
    }
