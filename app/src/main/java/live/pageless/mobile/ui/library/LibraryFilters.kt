package live.pageless.mobile.ui.library

enum class LibraryFilterCategory(
    val label: String,
) {
    AUTHORS("Authors"),
    NARRATORS("Narrators"),
    PROGRESS("Progress"),
    SERIES("Series"),
    COLLECTIONS("Collections"),
    PLAYLISTS("Playlists"),
    GENRES("Genres"),
    PUBLISHERS("Publishers"),
    LANGUAGES("Languages"),
    LIBRARIES("Libraries"),
}

enum class LibraryProgressState(
    val id: String,
    val label: String,
) {
    NOT_STARTED("not_started", "Not started"),
    IN_PROGRESS("in_progress", "In progress"),
    FINISHED("finished", "Finished"),
}

data class LibraryFilterOption(
    val id: String,
    val name: String,
    val bookCount: Int = 0,
)

data class LibraryFilters(
    val authorIds: Set<String> = emptySet(),
    val narratorIds: Set<String> = emptySet(),
    val genreIds: Set<String> = emptySet(),
    val seriesIds: Set<String> = emptySet(),
    val collectionIds: Set<String> = emptySet(),
    val playlistIds: Set<String> = emptySet(),
    val publisherIds: Set<String> = emptySet(),
    val languages: Set<String> = emptySet(),
    val libraryIds: Set<String> = emptySet(),
    val progressStates: Set<LibraryProgressState> = emptySet(),
) {
    companion object {
        fun only(
            category: LibraryFilterCategory,
            id: String,
        ): LibraryFilters = LibraryFilters().toggle(category, id)
    }

    val count: Int
        get() =
            authorIds.size + narratorIds.size + genreIds.size + seriesIds.size + collectionIds.size +
                playlistIds.size + publisherIds.size + languages.size + libraryIds.size + progressStates.size

    fun selected(category: LibraryFilterCategory): Set<String> =
        when (category) {
            LibraryFilterCategory.AUTHORS -> authorIds
            LibraryFilterCategory.NARRATORS -> narratorIds
            LibraryFilterCategory.GENRES -> genreIds
            LibraryFilterCategory.SERIES -> seriesIds
            LibraryFilterCategory.COLLECTIONS -> collectionIds
            LibraryFilterCategory.PLAYLISTS -> playlistIds
            LibraryFilterCategory.PUBLISHERS -> publisherIds
            LibraryFilterCategory.LANGUAGES -> languages
            LibraryFilterCategory.LIBRARIES -> libraryIds
            LibraryFilterCategory.PROGRESS -> progressStates.mapTo(mutableSetOf()) { it.id }
        }

    fun toggle(
        category: LibraryFilterCategory,
        id: String,
    ): LibraryFilters =
        when (category) {
            LibraryFilterCategory.AUTHORS -> copy(authorIds = authorIds.toggle(id))
            LibraryFilterCategory.NARRATORS -> copy(narratorIds = narratorIds.toggle(id))
            LibraryFilterCategory.GENRES -> copy(genreIds = genreIds.toggle(id))
            LibraryFilterCategory.SERIES -> copy(seriesIds = seriesIds.toggle(id))
            LibraryFilterCategory.COLLECTIONS -> copy(collectionIds = collectionIds.toggle(id))
            LibraryFilterCategory.PLAYLISTS -> copy(playlistIds = playlistIds.toggle(id))
            LibraryFilterCategory.PUBLISHERS -> copy(publisherIds = publisherIds.toggle(id))
            LibraryFilterCategory.LANGUAGES -> copy(languages = languages.toggle(id))
            LibraryFilterCategory.LIBRARIES -> copy(libraryIds = libraryIds.toggle(id))
            LibraryFilterCategory.PROGRESS -> {
                val state = LibraryProgressState.entries.firstOrNull { it.id == id } ?: return this
                copy(progressStates = progressStates.toggle(state))
            }
        }

    fun clear(category: LibraryFilterCategory): LibraryFilters =
        when (category) {
            LibraryFilterCategory.AUTHORS -> copy(authorIds = emptySet())
            LibraryFilterCategory.NARRATORS -> copy(narratorIds = emptySet())
            LibraryFilterCategory.GENRES -> copy(genreIds = emptySet())
            LibraryFilterCategory.SERIES -> copy(seriesIds = emptySet())
            LibraryFilterCategory.COLLECTIONS -> copy(collectionIds = emptySet())
            LibraryFilterCategory.PLAYLISTS -> copy(playlistIds = emptySet())
            LibraryFilterCategory.PUBLISHERS -> copy(publisherIds = emptySet())
            LibraryFilterCategory.LANGUAGES -> copy(languages = emptySet())
            LibraryFilterCategory.LIBRARIES -> copy(libraryIds = emptySet())
            LibraryFilterCategory.PROGRESS -> copy(progressStates = emptySet())
        }
}

data class LibraryBookFilterData(
    val id: String,
    val authorIds: Set<String>,
    val narratorIds: Set<String>,
    val genreIds: Set<String>,
    val seriesIds: Set<String>,
    val collectionIds: Set<String>,
    val playlistIds: Set<String>,
    val publisherIds: Set<String>,
    val languages: Set<String>,
    val libraryId: String?,
    val progressState: LibraryProgressState,
)

object LibraryFilterEngine {
    fun filter(
        books: List<LibraryBookFilterData>,
        filters: LibraryFilters,
    ): List<String> = books.filter { it.matches(filters) }.map { it.id }

    private fun LibraryBookFilterData.matches(filters: LibraryFilters): Boolean =
        authorIds.matchesAny(filters.authorIds) &&
            narratorIds.matchesAny(filters.narratorIds) &&
            genreIds.matchesAny(filters.genreIds) &&
            seriesIds.matchesAny(filters.seriesIds) &&
            collectionIds.matchesAny(filters.collectionIds) &&
            playlistIds.matchesAny(filters.playlistIds) &&
            publisherIds.matchesAny(filters.publisherIds) &&
            languages.matchesAny(filters.languages) &&
            (filters.libraryIds.isEmpty() || libraryId in filters.libraryIds) &&
            (filters.progressStates.isEmpty() || progressState in filters.progressStates)

    private fun Set<String>.matchesAny(selected: Set<String>): Boolean = selected.isEmpty() || any(selected::contains)
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) {
        this - value
    } else {
        this + value
    }
