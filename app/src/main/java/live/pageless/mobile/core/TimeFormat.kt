package live.pageless.mobile.core

/**
 * Pure duration/timestamp formatting.
 *
 * DUPLICATED FROM SERVER — keep in sync with `Pageless.Format`
 * (`duration/1`, `short_duration/1`, `clock/1`, `hms/1`, `parse_hms/1`).
 */
object TimeFormat {
    /** Human-friendly duration, e.g. "1h 1m" or "5m". */
    fun duration(seconds: Number?): String {
        val total = (seconds?.toDouble() ?: return "0m").toLong()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    /** Short duration with seconds precision, e.g. "4m 43s", "1h 2m", "5s". */
    fun shortDuration(seconds: Number?): String {
        val total = (seconds?.toDouble() ?: return "0s").toLong()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }

    /** Clock timestamp: "H:MM:SS" or "M:SS". */
    fun clock(seconds: Number?): String {
        val total = (seconds?.toDouble() ?: return "0:00").toLong()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60
        return if (hours > 0) "$hours:${pad(minutes)}:${pad(secs)}" else "$minutes:${pad(secs)}"
    }

    /** Zero-padded "HH:MM:SS". */
    fun hms(seconds: Number?): String {
        val total = (seconds?.toDouble() ?: return "00:00:00").toLong()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60
        return "${pad(hours)}:${pad(minutes)}:${pad(secs)}"
    }

    /** Parses "HH:MM:SS", "MM:SS", or plain seconds into a Double, or null. */
    fun parseHms(input: String?): Double? {
        val trimmed = input?.trim() ?: return null
        val parts = trimmed.split(":")
        if (parts.isEmpty() || parts.any { it.isEmpty() || !it.all(Char::isDigit) }) return null

        val (h, m, s) =
            when (parts.size) {
                1 -> Triple(0L, 0L, parts[0].toLong())
                2 -> Triple(0L, parts[0].toLong(), parts[1].toLong())
                3 -> Triple(parts[0].toLong(), parts[1].toLong(), parts[2].toLong())
                else -> return null
            }
        return (h * 3600 + m * 60 + s).toDouble()
    }

    private fun pad(n: Long): String = if (n < 10) "0$n" else "$n"
}
