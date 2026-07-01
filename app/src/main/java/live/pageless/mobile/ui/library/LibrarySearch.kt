package live.pageless.mobile.ui.library

const val LIBRARY_SEARCH_MIN_LENGTH = 2
const val LIBRARY_SEARCH_GROUP_LIMIT = 5

data class LibraryBookSearchData(
    val id: String,
    val title: String,
    val subtitle: String?,
    val authorNames: List<String>,
    val narratorNames: List<String>,
    val publisherName: String?,
)

data class LibrarySearchFacetGroup(
    val category: LibraryFilterCategory,
    val label: String,
    val total: Int,
    val items: List<LibraryFilterOption>,
)

object LibrarySearchEngine {
    val facetCategories =
        listOf(
            LibraryFilterCategory.AUTHORS,
            LibraryFilterCategory.NARRATORS,
            LibraryFilterCategory.SERIES,
            LibraryFilterCategory.COLLECTIONS,
            LibraryFilterCategory.PLAYLISTS,
            LibraryFilterCategory.GENRES,
            LibraryFilterCategory.PUBLISHERS,
            LibraryFilterCategory.LANGUAGES,
            LibraryFilterCategory.LIBRARIES,
        )

    fun ready(query: String): Boolean = query.trim().length >= LIBRARY_SEARCH_MIN_LENGTH

    fun filterBookIds(
        books: List<LibraryBookSearchData>,
        query: String,
    ): List<String> {
        val term = query.trim().lowercase()
        if (term.isEmpty()) return books.map { it.id }

        return books.filter { it.matches(term) }.map { it.id }
    }

    fun facetGroups(
        options: Map<LibraryFilterCategory, List<LibraryFilterOption>>,
        query: String,
    ): List<LibrarySearchFacetGroup> {
        if (!ready(query)) return emptyList()
        val term = query.trim().lowercase()

        return facetCategories.mapNotNull { category ->
            val matching = options[category].orEmpty().filter { it.name.lowercase().contains(term) }
            if (matching.isEmpty()) {
                null
            } else {
                LibrarySearchFacetGroup(
                    category = category,
                    label = category.label,
                    total = matching.size,
                    items = matching.take(LIBRARY_SEARCH_GROUP_LIMIT),
                )
            }
        }
    }

    private fun LibraryBookSearchData.matches(term: String): Boolean =
        title.lowercase().contains(term) ||
            subtitle?.lowercase()?.contains(term) == true ||
            authorNames.any { it.lowercase().contains(term) } ||
            narratorNames.any { it.lowercase().contains(term) } ||
            publisherName?.lowercase()?.contains(term) == true
}
