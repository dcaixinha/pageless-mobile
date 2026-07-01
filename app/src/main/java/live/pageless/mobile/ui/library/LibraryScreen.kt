package live.pageless.mobile.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import live.pageless.mobile.R
import live.pageless.mobile.core.Plural
import live.pageless.mobile.ui.components.BookCard
import live.pageless.mobile.ui.components.ConnectionStatusIcon
import live.pageless.mobile.ui.components.PagelessRefreshIndicator
import live.pageless.mobile.ui.components.TopTab
import live.pageless.mobile.ui.components.TopTabs
import live.pageless.mobile.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenBook: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onSelectTab: (TopTab) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(LibraryFilterCategory.AUTHORS) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun closeSearch() {
        searchExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BackHandler(enabled = searchExpanded) { closeSearch() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_brand),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                libraryTitle(state),
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            ConnectionStatusIcon()
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                )
                TopTabs(selected = TopTab.LIBRARY, onSelect = onSelectTab)
            }
        },
    ) { padding ->
        val refreshState = rememberPullToRefreshState()
        Box(Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshBox(
                isRefreshing = state.refreshing && !searchExpanded,
                onRefresh = { if (!searchExpanded) viewModel.refresh() },
                state = refreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PagelessRefreshIndicator(
                        state = refreshState,
                        isRefreshing = state.refreshing && !searchExpanded,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    )
                },
            ) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        LibrarySearchField(
                            query = state.searchQuery,
                            onQueryChange = {
                                viewModel.updateSearchQuery(it)
                                searchExpanded = LibrarySearchEngine.ready(it)
                            },
                            onFocus = { if (LibrarySearchEngine.ready(state.searchQuery)) searchExpanded = true },
                            onClear = {
                                viewModel.clearSearch()
                                searchExpanded = false
                            },
                            onImeSearch = { keyboardController?.hide() },
                            onEscape = { closeSearch() },
                        )

                        FilterToolbar(
                            state = state,
                            onOpenFilters = { showFilters = true },
                            onOpenSort = { showSort = true },
                            onOpenCategory = {
                                category = it
                                showFilters = true
                            },
                            onClearCategory = viewModel::clearFilter,
                        )

                        Box(Modifier.weight(1f)) {
                            when {
                                state.totalBookCount == 0 && state.refreshing ->
                                    CenterText("Loading your library…")

                                state.totalBookCount == 0 && state.error != null ->
                                    CenterText("Couldn't reach the server.\n${state.error}")

                                state.totalBookCount == 0 ->
                                    CenterText("No books yet.")

                                state.books.isEmpty() ->
                                    CenterText("No books match the current search and filters.")

                                else ->
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 150.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        items(state.books, key = { it.id }) { book ->
                                            BookCard(
                                                title = book.title,
                                                author = book.author,
                                                coverUrl = book.coverUrl,
                                                finished = book.finished,
                                                progressFraction = book.progressFraction,
                                                width = null,
                                                onClick = { onOpenBook(book.id) },
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }
            }

            if (searchExpanded && LibrarySearchEngine.ready(state.searchQuery)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 72.dp)
                        .zIndex(1f)
                        .semantics { contentDescription = "Dismiss search results" }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { closeSearch() },
                        ),
                )
                LibrarySearchResults(
                    state = state,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(start = 16.dp, top = 72.dp, end = 16.dp)
                            .zIndex(2f),
                    onOpenBook = { id ->
                        closeSearch()
                        onOpenBook(id)
                    },
                    onSelectFacet = { selectedCategory, id ->
                        closeSearch()
                        viewModel.selectSearchFacet(selectedCategory, id)
                    },
                )
            }
        }
    }

    if (showFilters) {
        LibraryFilterSheet(
            state = state,
            category = category,
            onCategoryChange = { category = it },
            onToggle = viewModel::toggleFilter,
            onClearCategory = viewModel::clearFilter,
            onClearAll = viewModel::clearFilters,
            onDismiss = { showFilters = false },
        )
    }

    if (showSort) {
        LibrarySortSheet(
            state = state.sortState,
            onSelect = viewModel::selectSort,
            onToggleDirection = viewModel::toggleSortDirection,
            onDismiss = { showSort = false },
        )
    }
}

@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocus: () -> Unit,
    onClear: () -> Unit,
    onImeSearch: () -> Unit,
    onEscape: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
                .onFocusChanged { if (it.isFocused) onFocus() }
                .onPreviewKeyEvent {
                    if (it.key == Key.Escape) {
                        onEscape()
                        true
                    } else {
                        false
                    }
                },
        placeholder = { Text("Search books and library metadata") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onImeSearch() }),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
    )
}

@Composable
private fun LibrarySearchResults(
    state: LibraryUiState,
    modifier: Modifier = Modifier,
    onOpenBook: (String) -> Unit,
    onSelectFacet: (LibraryFilterCategory, String) -> Unit,
) {
    val empty = state.searchBooks.isEmpty() && state.searchFacetGroups.isEmpty()

    Surface(
        modifier = modifier.fillMaxWidth().heightIn(max = 560.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 14.dp,
    ) {
        if (empty) {
            Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No matching books or metadata",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                if (state.searchBooks.isNotEmpty()) {
                    item { SearchGroupHeader("Books", state.searchBookCount) }
                    items(state.searchBooks, key = { "book-${it.id}" }) { book ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenBook(book.id) }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                book.coverUrl?.let { cover ->
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(book.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                book.author?.takeIf { it.isNotBlank() }?.let { author ->
                                    Text(
                                        "by $author",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                state.searchFacetGroups.forEach { group ->
                    item(key = "heading-${group.category}") {
                        SearchGroupHeader(group.label, group.total)
                    }
                    items(group.items, key = { "${group.category}-${it.id}" }) { option ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectFacet(group.category, option.id) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(option.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Plural.count(option.bookCount, "book"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGroupHeader(
    label: String,
    total: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        Text(
            total.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilterToolbar(
    state: LibraryUiState,
    onOpenFilters: () -> Unit,
    onOpenSort: () -> Unit,
    onOpenCategory: (LibraryFilterCategory) -> Unit,
    onClearCategory: (LibraryFilterCategory) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onOpenFilters) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (state.filters.count == 0) "Filters" else "Filters · ${state.filters.count}")
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onOpenSort, modifier = Modifier.widthIn(max = 190.dp)) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    state.sortState.sort.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.sortState.sort != LibrarySort.RANDOM) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (state.sortState.direction == LibrarySortDirection.ASCENDING) {
                            Icons.Default.ArrowUpward
                        } else {
                            Icons.Default.ArrowDownward
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        val activeCategories = LibraryFilterCategory.entries.filter { state.filters.selected(it).isNotEmpty() }
        if (activeCategories.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activeCategories) { activeCategory ->
                    InputChip(
                        selected = true,
                        onClick = { onOpenCategory(activeCategory) },
                        colors =
                            InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedTrailingIconColor = MaterialTheme.colorScheme.primary,
                            ),
                        label = {
                            Text("${activeCategory.label} · ${state.filters.selected(activeCategory).size}")
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear ${activeCategory.label}",
                                modifier =
                                    Modifier
                                        .size(16.dp)
                                        .clickable { onClearCategory(activeCategory) },
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySortSheet(
    state: LibrarySortState,
    onSelect: (LibrarySort) -> Unit,
    onToggleDirection: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Sort library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Choose a field and direction",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.sort != LibrarySort.RANDOM) {
                OutlinedButton(
                    onClick = onToggleDirection,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                ) {
                    Icon(
                        if (state.direction == LibrarySortDirection.ASCENDING) {
                            Icons.Default.ArrowUpward
                        } else {
                            Icons.Default.ArrowDownward
                        },
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.direction == LibrarySortDirection.ASCENDING) {
                            "Ascending"
                        } else {
                            "Descending"
                        },
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .weight(1f, fill = false),
            ) {
                items(LibrarySort.entries) { sort ->
                    val selected = state.sort == sort
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ).clickable {
                                    onSelect(sort)
                                    onDismiss()
                                }.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            sort.label,
                            modifier = Modifier.weight(1f),
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryFilterSheet(
    state: LibraryUiState,
    category: LibraryFilterCategory,
    onCategoryChange: (LibraryFilterCategory) -> Unit,
    onToggle: (LibraryFilterCategory, String) -> Unit,
    onClearCategory: (LibraryFilterCategory) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    var optionQuery by remember(category) { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Filter library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose multiple values to narrow your books",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.filters.count > 0) {
                    TextButton(onClick = onClearAll) { Text("Clear all") }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LibraryFilterCategory.entries) { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { onCategoryChange(item) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        label = {
                            val count = state.filters.selected(item).size
                            Text(if (count == 0) item.label else "${item.label} · $count")
                        },
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(category.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (state.filters.selected(category).isNotEmpty()) {
                    TextButton(onClick = { onClearCategory(category) }) { Text("Clear") }
                }
            }

            OutlinedTextField(
                value = optionQuery,
                onValueChange = { optionQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                placeholder = { Text("Search ${category.label.lowercase()}") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            val options =
                state.optionsFor(category).filter {
                    optionQuery.isBlank() || it.name.contains(optionQuery.trim(), ignoreCase = true)
                }
            if (options.isEmpty()) {
                Text(
                    "No options available",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .weight(1f, fill = false),
                ) {
                    items(options, key = { it.id }) { option ->
                        val selected = option.id in state.filters.selected(category)
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggle(category, option.id) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { onToggle(category, option.id) },
                            )
                            Text(option.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

private fun libraryTitle(state: LibraryUiState): String =
    when {
        state.totalBookCount == 0 -> "Library"
        state.filters.count > 0 || state.searchQuery.isNotBlank() ->
            "Library · ${state.books.size}/${state.totalBookCount}"
        else -> "Library · ${Plural.count(state.totalBookCount, "book")}"
    }

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
