package live.pageless.mobile.core

/**
 * Pure singular/plural noun counting.
 *
 * DUPLICATED FROM SERVER — keep in sync with `Pageless.Format.count/2,3`.
 */
object Plural {
    /**
     * Formats a count with its noun, pluralizing for any count other than 1.
     * Defaults to appending "s"; pass an explicit [plural] for irregular nouns.
     *
     *   count(1, "book")            -> "1 book"
     *   count(3, "book")            -> "3 books"
     *   count(2, "series", "series") -> "2 series"
     */
    fun count(
        n: Int,
        singular: String,
        plural: String = singular + "s",
    ): String {
        val noun = if (n == 1) singular else plural
        return "$n $noun"
    }
}
