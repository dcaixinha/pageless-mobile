package live.pageless.mobile.ui.library

enum class LibrarySort(
    val label: String,
    val defaultDirection: LibrarySortDirection,
) {
    TITLE("Title", LibrarySortDirection.ASCENDING),
    AUTHOR_FIRST("Author (First Last)", LibrarySortDirection.ASCENDING),
    AUTHOR_LAST("Author (Last, First)", LibrarySortDirection.ASCENDING),
    PUBLISHED("Publish year", LibrarySortDirection.DESCENDING),
    ADDED("Added at", LibrarySortDirection.DESCENDING),
    SIZE("Size", LibrarySortDirection.DESCENDING),
    DURATION("Duration", LibrarySortDirection.DESCENDING),
    MODIFIED("File modified", LibrarySortDirection.DESCENDING),
    PROGRESS_UPDATED("Progress: Last updated", LibrarySortDirection.DESCENDING),
    PROGRESS_STARTED("Progress: Started", LibrarySortDirection.DESCENDING),
    PROGRESS_FINISHED("Progress: Finished", LibrarySortDirection.DESCENDING),
    RANDOM("Randomly", LibrarySortDirection.DESCENDING),
}

enum class LibrarySortDirection {
    ASCENDING,
    DESCENDING,
    ;

    fun opposite(): LibrarySortDirection =
        if (this == ASCENDING) {
            DESCENDING
        } else {
            ASCENDING
        }
}

data class LibrarySortState(
    val sort: LibrarySort = LibrarySort.TITLE,
    val direction: LibrarySortDirection = LibrarySortDirection.ASCENDING,
    val randomSeed: Int = 0,
)

data class LibraryBookSortData(
    val id: String,
    val title: String,
    val authorNames: List<String>,
    val publishedDate: String?,
    val addedAt: String?,
    val size: Long?,
    val durationSeconds: Double,
    val fileModified: String?,
    val progressUpdatedAt: String?,
    val progressStartedAt: String?,
    val progressFinishedAt: String?,
)

object LibrarySortEngine {
    fun sort(
        books: List<LibraryBookSortData>,
        state: LibrarySortState,
        ignorePrefixesWhenSorting: Boolean = false,
    ): List<String> =
        books.sortedWith(comparator(state, ignorePrefixesWhenSorting)).map { it.id }

    private fun comparator(
        state: LibrarySortState,
        ignorePrefixesWhenSorting: Boolean,
    ): Comparator<LibraryBookSortData> =
        Comparator { left, right ->
            val primary = comparePrimary(left, right, state, ignorePrefixesWhenSorting)
            if (primary != 0) {
                primary
            } else {
                val title =
                    titleSortKey(left.title, ignorePrefixesWhenSorting)
                        .compareTo(titleSortKey(right.title, ignorePrefixesWhenSorting))
                if (title != 0) title else left.title.trim(' ').compareTo(right.title.trim(' '), ignoreCase = true)
            }
        }

    private fun comparePrimary(
        left: LibraryBookSortData,
        right: LibraryBookSortData,
        state: LibrarySortState,
        ignorePrefixesWhenSorting: Boolean,
    ): Int =
        when (state.sort) {
            LibrarySort.TITLE ->
                compareValues(
                    titleSortKey(left.title, ignorePrefixesWhenSorting),
                    titleSortKey(right.title, ignorePrefixesWhenSorting),
                    state.direction,
                )
            LibrarySort.AUTHOR_FIRST ->
                compareNullable(left.firstAuthorKey(), right.firstAuthorKey(), state.direction)
            LibrarySort.AUTHOR_LAST ->
                compareNullable(left.lastAuthorKey(), right.lastAuthorKey(), state.direction)
            LibrarySort.PUBLISHED -> compareNullable(left.publishedDate, right.publishedDate, state.direction)
            LibrarySort.ADDED -> compareNullable(left.addedAt, right.addedAt, state.direction)
            LibrarySort.SIZE -> compareNullable(left.size, right.size, state.direction)
            LibrarySort.DURATION -> compareValues(left.durationSeconds, right.durationSeconds, state.direction)
            LibrarySort.MODIFIED -> compareNullable(left.fileModified, right.fileModified, state.direction)
            LibrarySort.PROGRESS_UPDATED ->
                compareNullable(left.progressUpdatedAt, right.progressUpdatedAt, state.direction)
            LibrarySort.PROGRESS_STARTED ->
                compareNullable(left.progressStartedAt, right.progressStartedAt, state.direction)
            LibrarySort.PROGRESS_FINISHED ->
                compareNullable(left.progressFinishedAt, right.progressFinishedAt, state.direction)
            LibrarySort.RANDOM -> randomKey(left.id, state.randomSeed).compareTo(randomKey(right.id, state.randomSeed))
        }

    private fun LibraryBookSortData.firstAuthorKey(): String? = authorNames.minOfOrNull { it.lowercase() }

    private fun LibraryBookSortData.lastAuthorKey(): String? = authorNames.minOfOrNull(::lastNameKey)

    private fun lastNameKey(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (parts.size < 2) return name.lowercase()

        val particles = setOf("da", "de", "do", "dos", "das", "van", "von", "le", "la", "del", "der", "den")
        var surnameStart = parts.lastIndex
        while (surnameStart > 0 && parts[surnameStart - 1].lowercase() in particles) {
            surnameStart -= 1
        }
        val surname = parts.drop(surnameStart).joinToString(" ")
        val given = parts.take(surnameStart).joinToString(" ")
        return "$surname, $given".lowercase()
    }

    private fun randomKey(
        id: String,
        seed: Int,
    ): Int {
        var hash = id.hashCode() xor (seed * -0x61c88647)
        hash = (hash xor (hash ushr 16)) * -0x7a143595
        hash = (hash xor (hash ushr 13)) * -0x3d4d51cb
        return hash xor (hash ushr 16)
    }

    private fun <T : Comparable<T>> compareNullable(
        left: T?,
        right: T?,
        direction: LibrarySortDirection,
    ): Int =
        when {
            left == null && right == null -> 0
            left == null -> 1
            right == null -> -1
            else -> compareValues(left, right, direction)
        }

    private fun <T : Comparable<T>> compareValues(
        left: T,
        right: T,
        direction: LibrarySortDirection,
    ): Int {
        val compared = left.compareTo(right)
        return if (direction == LibrarySortDirection.ASCENDING) compared else -compared
    }
}

private val ignoredTitlePrefix = Regex("^(?:a|an|the)\\s+", RegexOption.IGNORE_CASE)

internal fun titleSortKey(
    title: String,
    ignorePrefixesWhenSorting: Boolean,
): String {
    val trimmed = title.trim(' ')
    return if (ignorePrefixesWhenSorting) trimmed.replace(ignoredTitlePrefix, "").lowercase() else trimmed.lowercase()
}
