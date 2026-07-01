package live.pageless.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class DateTimeFormatTest {
    @Test
    fun formatsEverySupportedDateFormatInEnglish() {
        val date = "2026-07-04"

        assertEquals("07/04/2026", DateTimeFormat.formatDate(date, "MM/dd/yyyy"))
        assertEquals("04/07/2026", DateTimeFormat.formatDate(date, "dd/MM/yyyy"))
        assertEquals("04.07.2026", DateTimeFormat.formatDate(date, "dd.MM.yyyy"))
        assertEquals("2026-07-04", DateTimeFormat.formatDate(date, "yyyy-MM-dd"))
        assertEquals("Jul 4th, 2026", DateTimeFormat.formatDate(date, "MMM do, yyyy"))
        assertEquals("July 4th, 2026", DateTimeFormat.formatDate(date, "MMMM do, yyyy"))
        assertEquals("04 Jul 2026", DateTimeFormat.formatDate(date, "dd MMM yyyy"))
        assertEquals("04 July 2026", DateTimeFormat.formatDate(date, "dd MMMM yyyy"))
    }

    @Test
    fun formatsOrdinalBoundaries() {
        val expected =
            mapOf(
                1 to "1st",
                2 to "2nd",
                3 to "3rd",
                4 to "4th",
                11 to "11th",
                12 to "12th",
                13 to "13th",
                21 to "21st",
                22 to "22nd",
                23 to "23rd",
                31 to "31st",
            )

        expected.forEach { (day, ordinal) ->
            val date = "2026-01-${day.toString().padStart(2, '0')}"
            assertEquals("Jan $ordinal, 2026", DateTimeFormat.formatDate(date, "MMM do, yyyy"))
        }
    }

    @Test
    fun formatsTwelveAndTwentyFourHourBoundaries() {
        assertEquals("12:00AM", DateTimeFormat.formatTime("2026-07-04T00:00:00Z", "h:mma", ZoneId.of("UTC")))
        assertEquals("12:00PM", DateTimeFormat.formatTime("2026-07-04T12:00:00Z", "h:mma", ZoneId.of("UTC")))
        assertEquals("1:05PM", DateTimeFormat.formatTime("2026-07-04T13:05:00Z", "h:mma", ZoneId.of("UTC")))
        assertEquals("00:00", DateTimeFormat.formatTime("2026-07-04T00:00:00Z", "HH:mm", ZoneId.of("UTC")))
        assertEquals("12:00", DateTimeFormat.formatTime("2026-07-04T12:00:00Z", "HH:mm", ZoneId.of("UTC")))
        assertEquals("23:59", DateTimeFormat.formatTime("2026-07-04T23:59:00Z", "HH:mm", ZoneId.of("UTC")))
    }

    @Test
    fun appliesProvidedZoneToInstantsButNotDateOnlyValues() {
        val losAngeles = ZoneId.of("America/Los_Angeles")

        assertEquals("03/07/2026", DateTimeFormat.formatDate("2026-07-04T00:30:00Z", zoneId = losAngeles))
        assertEquals("17:30", DateTimeFormat.formatTime("2026-07-04T00:30:00Z", zoneId = losAngeles))
        assertEquals("04/07/2026", DateTimeFormat.formatDate("2026-07-04", zoneId = losAngeles))
    }

    @Test
    fun defaultsUnknownFormatsAndRejectsInvalidValues() {
        assertEquals("04/07/2026", DateTimeFormat.formatDate("2026-07-04", "unknown"))
        assertEquals("13:05", DateTimeFormat.formatTime("2026-07-04T13:05:00Z", "unknown", ZoneId.of("UTC")))
        assertNull(DateTimeFormat.formatDate("not-a-date"))
        assertNull(DateTimeFormat.formatTime("not-an-instant"))
        assertNull(DateTimeFormat.formatDate(null))
        assertNull(DateTimeFormat.formatTime(""))
    }
}
