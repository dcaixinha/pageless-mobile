package live.pageless.mobile.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.pageless.mobile.data.local.BookEntity
import live.pageless.mobile.data.local.BookFacetEntity
import live.pageless.mobile.data.local.CachedLibraryEntity
import live.pageless.mobile.data.local.CollectionBookEntity
import live.pageless.mobile.data.local.CollectionEntity
import live.pageless.mobile.data.local.PlaylistBookEntity
import live.pageless.mobile.data.local.PlaylistEntity
import live.pageless.mobile.data.local.ProgressEntity
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.data.repository.BookmarkRepository
import live.pageless.mobile.data.repository.CollectionRepository
import live.pageless.mobile.data.repository.LibraryRepository
import live.pageless.mobile.data.repository.PlaybackHistoryRepository
import live.pageless.mobile.data.repository.PlaylistRepository
import live.pageless.mobile.data.repository.ProgressRepository
import live.pageless.mobile.data.repository.ShelfBook
import live.pageless.mobile.data.repository.coverModel
import javax.inject.Inject

data class LibraryUiState(
    val books: List<ShelfBook> = emptyList(),
    val totalBookCount: Int = 0,
    val filters: LibraryFilters = LibraryFilters(),
    val sortState: LibrarySortState = LibrarySortState(),
    val filterOptions: Map<LibraryFilterCategory, List<LibraryFilterOption>> = emptyMap(),
    val searchQuery: String = "",
    val searchBookCount: Int = 0,
    val searchBooks: List<ShelfBook> = emptyList(),
    val searchFacetGroups: List<LibrarySearchFacetGroup> = emptyList(),
    val refreshing: Boolean = false,
    val error: String? = null,
) {
    fun optionsFor(category: LibraryFilterCategory): List<LibraryFilterOption> = filterOptions[category].orEmpty()
}

private data class LocalCatalog(
    val books: List<BookEntity>,
    val progress: List<ProgressEntity>,
    val facets: List<BookFacetEntity>,
)

private data class CatalogWithLibraries(
    val catalog: LocalCatalog,
    val libraries: List<CachedLibraryEntity>,
)

private data class OrganizationCatalog(
    val collections: List<CollectionEntity>,
    val collectionMembers: List<CollectionBookEntity>,
    val playlists: List<PlaylistEntity>,
    val playlistMembers: List<PlaylistBookEntity>,
)

private data class FullCatalog(
    val catalog: LocalCatalog,
    val libraries: List<CachedLibraryEntity>,
    val organizations: OrganizationCatalog,
)

private data class CatalogIndex(
    val totalBookCount: Int,
    val filterData: List<LibraryBookFilterData>,
    val searchDataById: Map<String, LibraryBookSearchData>,
    val sortDataById: Map<String, LibraryBookSortData>,
    val shelfBooksById: Map<String, ShelfBook>,
    val filterOptions: Map<LibraryFilterCategory, List<LibraryFilterOption>>,
)

private data class RefreshState(
    val refreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val libraryRepository: LibraryRepository,
        private val progressRepository: ProgressRepository,
        private val bookmarkRepository: BookmarkRepository,
        private val playbackHistoryRepository: PlaybackHistoryRepository,
        private val collectionRepository: CollectionRepository,
        private val playlistRepository: PlaylistRepository,
        private val authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val filters =
            MutableStateFlow(
                initialFilters(
                    savedStateHandle["filterCategory"],
                    savedStateHandle["filterId"],
                ),
            )
        private val searchQuery = MutableStateFlow("")
        private val sortState = MutableStateFlow(LibrarySortState())
        private val refreshState = MutableStateFlow(RefreshState())

        private val localCatalog =
            combine(
                libraryRepository.observeBooks(),
                libraryRepository.observeAllProgress(),
                libraryRepository.observeBookFacets(),
            ) { books, progress, facets ->
                LocalCatalog(books, progress, facets)
            }

        private val catalogWithLibraries =
            combine(localCatalog, libraryRepository.observeLibraries()) { catalog, libraries ->
                CatalogWithLibraries(catalog, libraries)
            }

        private val organizationCatalog =
            combine(
                collectionRepository.observeAll(),
                collectionRepository.observeAllMembers(),
                playlistRepository.observeAll(),
                playlistRepository.observeAllMembers(),
            ) { collections, collectionMembers, playlists, playlistMembers ->
                OrganizationCatalog(collections, collectionMembers, playlists, playlistMembers)
            }

        private val fullCatalog =
            combine(catalogWithLibraries, organizationCatalog) { catalog, organizations ->
                FullCatalog(catalog.catalog, catalog.libraries, organizations)
            }

        private val catalogIndex =
            combine(fullCatalog, authRepository.serverUrl) { data, serverUrl ->
                buildCatalogIndex(data.catalog, data.libraries, data.organizations, serverUrl)
            }.flowOn(Dispatchers.Default)

        private val catalogState =
            combine(
                catalogIndex,
                filters,
                sortState,
                searchQuery,
                authRepository.ignorePrefixesWhenSorting,
            ) { index, selected, sorting, query, ignorePrefixesWhenSorting ->
                buildCatalogState(index, selected, sorting, query, ignorePrefixesWhenSorting)
            }.flowOn(Dispatchers.Default)

        val state: StateFlow<LibraryUiState> =
            combine(catalogState, refreshState) { catalog, refresh ->
                catalog.copy(refreshing = refresh.refreshing, error = refresh.error)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

        init {
            refresh()
        }

        fun toggleFilter(
            category: LibraryFilterCategory,
            id: String,
        ) {
            filters.update { it.toggle(category, id) }
            reshuffleIfRandom()
        }

        fun clearFilter(category: LibraryFilterCategory) {
            filters.update { it.clear(category) }
            reshuffleIfRandom()
        }

        fun clearFilters() {
            filters.value = LibraryFilters()
            reshuffleIfRandom()
        }

        fun updateSearchQuery(query: String) {
            searchQuery.value = query
        }

        fun clearSearch() {
            searchQuery.value = ""
        }

        fun selectSearchFacet(
            category: LibraryFilterCategory,
            id: String,
        ) {
            filters.value = LibraryFilters.only(category, id)
            searchQuery.value = ""
            reshuffleIfRandom()
        }

        fun selectSort(sort: LibrarySort) {
            sortState.update { current ->
                when {
                    sort == LibrarySort.RANDOM ->
                        LibrarySortState(
                            sort = LibrarySort.RANDOM,
                            direction = LibrarySort.RANDOM.defaultDirection,
                            randomSeed = current.randomSeed + 1,
                        )
                    sort == current.sort ->
                        current.copy(direction = current.direction.opposite())
                    else ->
                        LibrarySortState(sort = sort, direction = sort.defaultDirection, randomSeed = current.randomSeed)
                }
            }
        }

        fun toggleSortDirection() {
            sortState.update { current ->
                if (current.sort == LibrarySort.RANDOM) current else current.copy(direction = current.direction.opposite())
            }
        }

        private fun reshuffleIfRandom() {
            sortState.update { current ->
                if (current.sort == LibrarySort.RANDOM) current.copy(randomSeed = current.randomSeed + 1) else current
            }
        }

        fun refresh() {
            reshuffleIfRandom()
            refreshState.update { it.copy(refreshing = true, error = null) }
            viewModelScope.launch {
                val accountResult = authRepository.refreshCurrentUser()
                val progressResult = progressRepository.sync()
                val bookmarkResult = bookmarkRepository.sync()
                val historyResult = playbackHistoryRepository.sync()
                val result = libraryRepository.refreshBooks()
                val collectionResult = if (result.isSuccess) collectionRepository.refreshAll() else Result.success(Unit)
                val playlistResult = if (result.isSuccess) playlistRepository.refreshAll() else Result.success(Unit)
                val failure =
                    accountResult.exceptionOrNull()
                        ?: progressResult.exceptionOrNull()
                        ?: bookmarkResult.exceptionOrNull()
                        ?: historyResult.exceptionOrNull()
                        ?: collectionResult.exceptionOrNull()
                        ?: playlistResult.exceptionOrNull()
                        ?: result.exceptionOrNull()
                refreshState.update {
                    it.copy(
                        refreshing = false,
                        error = failure?.let { error -> error.message ?: "Sync failed" },
                    )
                }
            }
        }

        fun logout(onDone: () -> Unit) {
            viewModelScope.launch {
                authRepository.logout()
                onDone()
            }
        }

        private fun buildCatalogIndex(
            catalog: LocalCatalog,
            libraries: List<CachedLibraryEntity>,
            organizations: OrganizationCatalog,
            serverUrl: String,
        ): CatalogIndex {
            val progressByBook = catalog.progress.associateBy { it.bookId }
            val facetsByBook = catalog.facets.groupBy { it.bookId }
            val collectionIdsByBook = organizations.collectionMembers.groupBy { it.bookId }
            val playlistIdsByBook = organizations.playlistMembers.groupBy { it.bookId }

            val filterData =
                catalog.books.map { book ->
                    val bookFacets = facetsByBook[book.id].orEmpty()
                    LibraryBookFilterData(
                        id = book.id,
                        authorIds = bookFacets.idsFor("author"),
                        narratorIds = bookFacets.idsFor("narrator"),
                        genreIds = bookFacets.idsFor("genre"),
                        seriesIds = bookFacets.idsFor("series"),
                        collectionIds =
                            collectionIdsByBook[book.id].orEmpty().mapTo(mutableSetOf()) { it.collectionId },
                        playlistIds = playlistIdsByBook[book.id].orEmpty().mapTo(mutableSetOf()) { it.playlistId },
                        publisherIds = bookFacets.idsFor("publisher"),
                        languages = bookFacets.idsFor("language"),
                        libraryId = book.libraryId,
                        progressState = progressState(progressByBook[book.id]),
                    )
                }

            val searchDataById =
                catalog.books.associate { book ->
                    val facets = facetsByBook[book.id].orEmpty()
                    book.id to
                        LibraryBookSearchData(
                            id = book.id,
                            title = book.title,
                            subtitle = book.subtitle,
                            authorNames = facets.namesFor("author"),
                            narratorNames = facets.namesFor("narrator"),
                            publisherName = facets.namesFor("publisher").firstOrNull(),
                        )
                }

            val sortDataById =
                catalog.books.associate { book ->
                    val progress = progressByBook[book.id]
                    book.id to
                        LibraryBookSortData(
                            id = book.id,
                            title = book.title,
                            authorNames = facetsByBook[book.id].orEmpty().namesFor("author"),
                            publishedDate = book.publishedDate,
                            addedAt = book.addedAt,
                            size = book.size,
                            durationSeconds = book.durationSeconds,
                            fileModified = book.fileModified,
                            progressUpdatedAt = progress?.updatedAt,
                            progressStartedAt = progress?.startedAt,
                            progressFinishedAt = progress?.finishedAt,
                        )
                }

            val shelfBooksById =
                catalog.books.associate { book ->
                    val progress = progressByBook[book.id]
                    book.id to
                        ShelfBook(
                            id = book.id,
                            title = book.title,
                            author = book.authors,
                            coverUrl = book.coverModel(serverUrl),
                            finished = progress?.finished == true,
                            progressFraction = progressFraction(progress),
                        )
                }

            return CatalogIndex(
                totalBookCount = catalog.books.size,
                filterData = filterData,
                searchDataById = searchDataById,
                sortDataById = sortDataById,
                shelfBooksById = shelfBooksById,
                filterOptions = filterOptions(catalog.books, catalog.facets, libraries, organizations),
            )
        }

        private fun buildCatalogState(
            index: CatalogIndex,
            selected: LibraryFilters,
            sorting: LibrarySortState,
            query: String,
            ignorePrefixesWhenSorting: Boolean,
        ): LibraryUiState {
            val facetFilteredIds = LibraryFilterEngine.filter(index.filterData, selected)
            val searchData = facetFilteredIds.mapNotNull(index.searchDataById::get)
            val filteredIds = LibrarySearchEngine.filterBookIds(searchData, query)
            val sortData = filteredIds.mapNotNull(index.sortDataById::get)
            val sortedIds = LibrarySortEngine.sort(sortData, sorting, ignorePrefixesWhenSorting)
            val visibleBooks = sortedIds.mapNotNull(index.shelfBooksById::get)

            return LibraryUiState(
                books = visibleBooks,
                totalBookCount = index.totalBookCount,
                filters = selected,
                sortState = sorting,
                filterOptions = index.filterOptions,
                searchQuery = query,
                searchBookCount = visibleBooks.size,
                searchBooks =
                    if (LibrarySearchEngine.ready(query)) {
                        visibleBooks.take(LIBRARY_SEARCH_GROUP_LIMIT)
                    } else {
                        emptyList()
                    },
                searchFacetGroups = LibrarySearchEngine.facetGroups(index.filterOptions, query),
            )
        }
    }

internal fun initialFilters(
    categoryName: String?,
    id: String?,
): LibraryFilters {
    if (categoryName.isNullOrBlank() || id.isNullOrBlank()) return LibraryFilters()
    val category = LibraryFilterCategory.entries.firstOrNull { it.name == categoryName } ?: return LibraryFilters()
    return LibraryFilters().toggle(category, id)
}

private fun List<BookFacetEntity>.idsFor(category: String): Set<String> = asSequence().filter { it.category == category }.map { it.facetId }.toSet()

private fun List<BookFacetEntity>.namesFor(category: String): List<String> =
    asSequence()
        .filter { it.category == category }
        .sortedBy { it.position }
        .map { it.name }
        .toList()

private fun progressState(progress: ProgressEntity?): LibraryProgressState =
    when {
        progress == null -> LibraryProgressState.NOT_STARTED
        progress.finished -> LibraryProgressState.FINISHED
        else -> LibraryProgressState.IN_PROGRESS
    }

private fun progressFraction(progress: ProgressEntity?): Float? =
    progress?.takeIf { it.durationSeconds > 0 }?.let {
        (it.currentSeconds / it.durationSeconds).toFloat().coerceIn(0f, 1f)
    }

private fun filterOptions(
    books: List<BookEntity>,
    facets: List<BookFacetEntity>,
    libraries: List<CachedLibraryEntity>,
    organizations: OrganizationCatalog,
): Map<LibraryFilterCategory, List<LibraryFilterOption>> {
    val representedBookIds = books.mapTo(mutableSetOf()) { it.id }
    val representedLibraryIds = books.mapNotNullTo(mutableSetOf()) { it.libraryId }
    val representedCollectionIds =
        organizations.collectionMembers
            .filter { it.bookId in representedBookIds }
            .mapTo(mutableSetOf()) { it.collectionId }
    val representedPlaylistIds =
        organizations.playlistMembers
            .filter { it.bookId in representedBookIds }
            .mapTo(mutableSetOf()) { it.playlistId }
    val collectionCounts =
        organizations.collectionMembers
            .filter { it.bookId in representedBookIds }
            .groupingBy { it.collectionId }
            .eachCount()
    val playlistCounts =
        organizations.playlistMembers
            .filter { it.bookId in representedBookIds }
            .groupingBy { it.playlistId }
            .eachCount()
    val libraryCounts = books.mapNotNull { it.libraryId }.groupingBy { it }.eachCount()
    val representedFacets = facets.filter { it.bookId in representedBookIds }

    fun facetOptions(category: String): List<LibraryFilterOption> =
        representedFacets
            .filter { it.category == category }
            .groupBy { it.facetId }
            .map { (id, rows) ->
                LibraryFilterOption(id, rows.first().name, rows.map { it.bookId }.distinct().size)
            }.sortedBy { it.name.lowercase() }

    return mapOf(
        LibraryFilterCategory.AUTHORS to facetOptions("author"),
        LibraryFilterCategory.NARRATORS to facetOptions("narrator"),
        LibraryFilterCategory.GENRES to facetOptions("genre"),
        LibraryFilterCategory.SERIES to facetOptions("series"),
        LibraryFilterCategory.COLLECTIONS to
            organizations.collections
                .filter { it.id in representedCollectionIds }
                .map { collection ->
                    LibraryFilterOption(collection.id, collection.name, collectionCounts[collection.id] ?: 0)
                },
        LibraryFilterCategory.PLAYLISTS to
            organizations.playlists
                .filter { it.id in representedPlaylistIds }
                .map { playlist ->
                    LibraryFilterOption(playlist.id, playlist.name, playlistCounts[playlist.id] ?: 0)
                },
        LibraryFilterCategory.PUBLISHERS to facetOptions("publisher"),
        LibraryFilterCategory.LANGUAGES to facetOptions("language"),
        LibraryFilterCategory.LIBRARIES to
            libraries
                .filter { it.id in representedLibraryIds }
                .map { library ->
                    LibraryFilterOption(
                        library.id,
                        library.name,
                        libraryCounts[library.id] ?: 0,
                    )
                },
        LibraryFilterCategory.PROGRESS to
            LibraryProgressState.entries.map { LibraryFilterOption(it.id, it.label) },
    )
}
