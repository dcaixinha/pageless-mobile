package live.pageless.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mirrors the server's `Pageless.Format` doctests and tests. */
class TimeFormatTest {
    @Test
    fun duration() {
        assertEquals("0m", TimeFormat.duration(0))
        assertEquals("1m", TimeFormat.duration(95))
        assertEquals("1h 1m", TimeFormat.duration(3700))
        assertEquals("0m", TimeFormat.duration(null))
    }

    @Test
    fun shortDuration() {
        assertEquals("5s", TimeFormat.shortDuration(5))
        assertEquals("4m 43s", TimeFormat.shortDuration(283))
        assertEquals("1h 2m", TimeFormat.shortDuration(3725))
    }

    @Test
    fun clock() {
        assertEquals("0:00", TimeFormat.clock(0))
        assertEquals("1:05", TimeFormat.clock(65))
        assertEquals("1:01:01", TimeFormat.clock(3661))
    }

    @Test
    fun hms() {
        assertEquals("00:00:00", TimeFormat.hms(0))
        assertEquals("01:02:05", TimeFormat.hms(3725))
        assertEquals("00:00:00", TimeFormat.hms(null))
    }

    @Test
    fun parseHms() {
        assertEquals(3725.0, TimeFormat.parseHms("01:02:05")!!, 0.0)
        assertEquals(125.0, TimeFormat.parseHms("2:05")!!, 0.0)
        assertNull(TimeFormat.parseHms("bad"))
        assertNull(TimeFormat.parseHms(""))
        assertNull(TimeFormat.parseHms("1:2:3:4"))
        assertNull(TimeFormat.parseHms(null))
    }

    @Test
    fun parseHmsRoundTrips() {
        for (seconds in listOf(0L, 5L, 65L, 3725L, 55_121L)) {
            assertEquals(seconds.toDouble(), TimeFormat.parseHms(TimeFormat.hms(seconds))!!, 0.0)
        }
    }
}
