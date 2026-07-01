package live.pageless.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Mirror of the server's `Pageless.Format.count/2,3` tests. */
class PluralTest {
    @Test
    fun pluralizes_for_any_count_other_than_one() {
        assertEquals("0 books", Plural.count(0, "book"))
        assertEquals("1 book", Plural.count(1, "book"))
        assertEquals("2 books", Plural.count(2, "book"))
    }

    @Test
    fun uses_explicit_plural_for_irregular_nouns() {
        assertEquals("1 series", Plural.count(1, "series", "series"))
        assertEquals("3 series", Plural.count(3, "series", "series"))
    }
}
