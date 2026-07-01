package live.pageless.mobile.core

import java.time.Instant

/** UTC ISO-8601 timestamps matching the server's `DateTime.to_iso8601/1`. */
object Iso8601 {
    fun now(): String = Instant.now().toString()
}
