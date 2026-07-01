package live.pageless.mobile.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Audiobookshelf-compatible, server-synced date and time formatting. */
object DateTimeFormat {
    const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"
    const val DEFAULT_TIME_FORMAT = "HH:mm"

    private val shortMonths =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val longMonths =
        listOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December",
        )

    fun formatDate(
        isoDateOrInstant: String?,
        format: String = DEFAULT_DATE_FORMAT,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String? {
        if (isoDateOrInstant.isNullOrBlank()) return null

        val date =
            runCatching { LocalDate.parse(isoDateOrInstant) }.getOrNull()
                ?: runCatching { Instant.parse(isoDateOrInstant).atZone(zoneId).toLocalDate() }.getOrNull()
                ?: return null

        return format(date, format)
    }

    fun formatTime(
        isoInstant: String?,
        format: String = DEFAULT_TIME_FORMAT,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String? {
        if (isoInstant.isNullOrBlank()) return null

        val time = runCatching { Instant.parse(isoInstant).atZone(zoneId).toLocalTime() }.getOrNull() ?: return null
        val minute = time.minute.toString().padStart(2, '0')

        return when (format) {
            "h:mma" -> {
                val hour =
                    when (val value = time.hour % 12) {
                        0 -> 12
                        else -> value
                    }
                val period = if (time.hour < 12) "AM" else "PM"
                "$hour:$minute$period"
            }

            else -> "${time.hour.toString().padStart(2, '0')}:$minute"
        }
    }

    private fun format(
        date: LocalDate,
        format: String,
    ): String {
        val day = date.dayOfMonth.toString().padStart(2, '0')
        val month = date.monthValue.toString().padStart(2, '0')
        val shortMonth = shortMonths[date.monthValue - 1]
        val longMonth = longMonths[date.monthValue - 1]

        return when (format) {
            "MM/dd/yyyy" -> "$month/$day/${date.year}"
            "dd.MM.yyyy" -> "$day.$month.${date.year}"
            "yyyy-MM-dd" -> "${date.year}-$month-$day"
            "MMM do, yyyy" -> "$shortMonth ${ordinal(date.dayOfMonth)}, ${date.year}"
            "MMMM do, yyyy" -> "$longMonth ${ordinal(date.dayOfMonth)}, ${date.year}"
            "dd MMM yyyy" -> "$day $shortMonth ${date.year}"
            "dd MMMM yyyy" -> "$day $longMonth ${date.year}"
            else -> "$day/$month/${date.year}"
        }
    }

    private fun ordinal(day: Int): String {
        val suffix =
            if (day % 100 in 11..13) {
                "th"
            } else {
                when (day % 10) {
                    1 -> "st"
                    2 -> "nd"
                    3 -> "rd"
                    else -> "th"
                }
            }
        return "$day$suffix"
    }
}
