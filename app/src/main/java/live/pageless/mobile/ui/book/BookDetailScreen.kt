package live.pageless.mobile.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import live.pageless.mobile.core.Chapters
import live.pageless.mobile.core.DateTimeFormat
import live.pageless.mobile.core.TimeFormat
import live.pageless.mobile.data.local.BookmarkEntity
import live.pageless.mobile.data.local.ProgressEntity
import live.pageless.mobile.ui.components.ConnectionStatusIcon
import live.pageless.mobile.ui.components.PagelessRefreshIndicator
import live.pageless.mobile.ui.library.LibraryFilterCategory
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenLibraryFilter: (LibraryFilterCategory, String) -> Unit = { _, _ -> },
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    val series by viewModel.series.collectAsStateWithLifecycle()
    val filterMetadata by viewModel.filterMetadata.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val playerSettings by viewModel.playerSettings.collectAsStateWithLifecycle()
    val coverUrl by viewModel.coverUrl.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.connectPlayer()
        viewModel.refreshBookmarks()
        onDispose { }
    }

    LaunchedEffect(book?.id) {
        if (book != null) viewModel.preload()
    }

    // Bookmarks and chapters are collapsible, open by default.
    var bookmarksExpanded by rememberSaveable { mutableStateOf(true) }
    var chaptersExpanded by rememberSaveable { mutableStateOf(true) }
    var showAddBookmark by rememberSaveable { mutableStateOf(false) }
    var selectedBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }

    val bookId = book?.id
    val isActive = bookId != null && player.bookId == bookId && !player.isPreview
    val bookmarkPositionLabel =
        TimeFormat.clock(
            if (isActive) player.positionMs / 1000.0 else progress?.currentSeconds ?: 0.0,
        )

    // Highlight the chapter under the live playhead, or saved progress when not active.
    val chapterPositionSeconds = if (isActive) player.positionMs / 1000.0 else progress?.currentSeconds ?: 0.0
    // A finished book has no "current" chapter (don't highlight the first one
    // when there's no saved position, e.g. imported as finished), unless we're
    // actively playing it.
    val currentChapterIndex =
        if (chapters.isNotEmpty() && !(progress?.finished == true && !isActive)) {
            val spans =
                chapters.map {
                    object : Chapters.Span {
                        override val startSeconds = it.startSeconds
                        override val endSeconds = it.endSeconds
                    }
                }
            Chapters.currentIndex(spans, chapterPositionSeconds)
        } else {
            null
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(book?.title ?: "Book")
                        androidx.compose.foundation.layout
                            .Spacer(Modifier.width(8.dp))
                        ConnectionStatusIcon()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Listening history")
                    }
                },
            )
        },
    ) { padding ->
        val refreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = {
                viewModel.refresh()
                viewModel.refreshBookmarks()
            },
            state = refreshState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            indicator = {
                PagelessRefreshIndicator(
                    state = refreshState,
                    isRefreshing = state.loading,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(16.dp),
            ) {
                book?.let { b ->
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CoverArt(url = coverUrl, title = b.title)
                            Text(
                                b.title,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                            b.subtitle?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            b.authors?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    item {
                        // Transport controls live in the mini/full-screen player;
                        // the detail page just starts (or pauses) playback.
                        val playing = isActive && player.isPlaying
                        val label =
                            when {
                                playing -> "Pause"
                                isActive -> "Play"
                                (progress?.currentSeconds ?: 0.0) > 0 -> "Resume"
                                else -> "Play"
                            }
                        Button(
                            onClick = viewModel::playPause,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        ) {
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                            )
                            Text(label, modifier = Modifier.padding(start = 6.dp))
                        }
                    }

                    progress?.let { p ->
                        item {
                            ProgressPill(
                                finished = p.finished,
                                percent = progressPercent(p, isActive, player.positionMs),
                                remaining = remainingLabel(p, b.durationSeconds, isActive, player.positionMs),
                                currentChapterTitle =
                                    if (!p.finished) {
                                        chapters
                                            .getOrNull(currentChapterIndex ?: -1)
                                            ?.let { it.title ?: "Chapter ${it.index + 1}" }
                                    } else {
                                        null
                                    },
                                startedAt = DateTimeFormat.formatDate(p.startedAt, dateFormat, ZoneId.of("UTC")),
                                finishedAt = DateTimeFormat.formatDate(p.finishedAt, dateFormat, ZoneId.of("UTC")),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }

                    item {
                        DownloadControl(
                            completed = downloadStatus.completed,
                            downloading = downloadStatus.running,
                            fraction = downloadStatus.progressPercent?.let { it / 100f },
                            onDownload = viewModel::startDownload,
                            onCancel = viewModel::cancelDownload,
                            onDelete = viewModel::deleteDownload,
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            MetadataLinksRow(
                                "Authors",
                                filterMetadata[LibraryFilterCategory.AUTHORS].orEmpty(),
                            ) { onOpenLibraryFilter(LibraryFilterCategory.AUTHORS, it) }
                            MetadataLinksRow(
                                "Narrators",
                                filterMetadata[LibraryFilterCategory.NARRATORS].orEmpty(),
                            ) { onOpenLibraryFilter(LibraryFilterCategory.NARRATORS, it) }
                            MetadataLinksRow(
                                "Series",
                                if (series.isNotEmpty()) {
                                    series.map { BookMetadataLink(it.id, seriesLabel(it.name, it.sequence)) }
                                } else {
                                    filterMetadata[LibraryFilterCategory.SERIES].orEmpty()
                                },
                            ) { onOpenLibraryFilter(LibraryFilterCategory.SERIES, it) }
                            MetadataLinksRow(
                                "Collections",
                                filterMetadata[LibraryFilterCategory.COLLECTIONS].orEmpty(),
                            ) { onOpenLibraryFilter(LibraryFilterCategory.COLLECTIONS, it) }
                            MetadataLinksRow(
                                "Playlists",
                                filterMetadata[LibraryFilterCategory.PLAYLISTS].orEmpty(),
                            ) { onOpenLibraryFilter(LibraryFilterCategory.PLAYLISTS, it) }
                            MetadataLinksRow(
                                "Genres",
                                filterMetadata[LibraryFilterCategory.GENRES].orEmpty(),
                            ) { onOpenLibraryFilter(LibraryFilterCategory.GENRES, it) }
                            MetadataLinksRow(
                                "Publisher",
                                filterMetadata[LibraryFilterCategory.PUBLISHERS].orEmpty(),
                            ) { onOpenLibraryFilter(LibraryFilterCategory.PUBLISHERS, it) }
                            MetadataLinksRow(
                                "Language",
                                filterMetadata[LibraryFilterCategory.LANGUAGES].orEmpty(),
                            ) { onOpenLibraryFilter(LibraryFilterCategory.LANGUAGES, it) }
                            MetadataRow("Duration", TimeFormat.duration(b.durationSeconds))
                            MetadataRow("Publish year", publishYear(b.publishedDate, b.publishedYear))
                        }
                    }

                    b.description?.takeIf { it.isNotBlank() }?.let { html ->
                        item {
                            DescriptionText(html = html, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { bookmarksExpanded = !bookmarksExpanded }
                                .padding(top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Bookmarks (${bookmarks.size})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showAddBookmark = true }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = "Add bookmark")
                            }
                            Icon(
                                if (bookmarksExpanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription =
                                    if (bookmarksExpanded) {
                                        "Collapse bookmarks"
                                    } else {
                                        "Expand bookmarks"
                                    },
                            )
                        }
                    }
                }
                if (bookmarksExpanded) {
                    if (bookmarks.isEmpty()) {
                        item {
                            Text(
                                "No bookmarks yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    } else {
                        groupedBookmarks(bookmarks, chapters).forEach { group ->
                            item(key = "bookmark-chapter-${group.chapterKey}") {
                                Text(
                                    group.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(group.bookmarks, key = { it.id }) { bm ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedBookmark = bm }
                                            .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        TimeFormat.clock(bm.positionSeconds),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(end = 12.dp),
                                    )
                                    Text(
                                        bm.note ?: "Bookmark",
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { viewModel.deleteBookmark(bm.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete bookmark",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                if (chapters.isNotEmpty()) {
                    item {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { chaptersExpanded = !chaptersExpanded }
                                    .padding(top = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Chapters (${chapters.size})",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Icon(
                                if (chaptersExpanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription =
                                    if (chaptersExpanded) {
                                        "Collapse chapters"
                                    } else {
                                        "Expand chapters"
                                    },
                            )
                        }
                    }
                    if (chaptersExpanded) {
                        item(key = "chapter-header") {
                            ChapterTableHeader(
                                showStart = playerSettings.showChapterStartOnBookDetail,
                                showDuration = playerSettings.showChapterDurationOnBookDetail,
                            )
                        }

                        itemsIndexed(chapters, key = { _, ch -> ch.id }) { index, ch ->
                            val isCurrent = index == currentChapterIndex
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isCurrent) {
                                                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            } else {
                                                Modifier
                                            },
                                        ).clickable {
                                            if (isActive) {
                                                viewModel.seekTo((ch.startSeconds * 1000).toLong())
                                            } else {
                                                viewModel.playFrom((ch.startSeconds * 1000).toLong())
                                            }
                                        }.padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    ch.title ?: "Chapter ${ch.index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        if (isCurrent) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                )
                                ChapterTiming(
                                    startSeconds = ch.startSeconds,
                                    endSeconds = ch.endSeconds,
                                    current = isCurrent,
                                    showStart = playerSettings.showChapterStartOnBookDetail,
                                    showDuration = playerSettings.showChapterDurationOnBookDetail,
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showAddBookmark) {
        AddBookmarkDialog(
            positionLabel = bookmarkPositionLabel,
            onDismiss = { showAddBookmark = false },
            onAdd = { note ->
                viewModel.addBookmark(note)
                showAddBookmark = false
            },
        )
    }

    selectedBookmark?.let { bookmark ->
        bookId?.let { id ->
            val chapterTitle =
                chapters
                    .getOrNull(
                        Chapters.currentIndex(
                            chapters.map { chapter ->
                                object : Chapters.Span {
                                    override val startSeconds = chapter.startSeconds
                                    override val endSeconds = chapter.endSeconds
                                }
                            },
                            bookmark.positionSeconds,
                        ) ?: -1,
                    )?.let { it.title ?: "Chapter ${it.index + 1}" }
            BookmarkActionDialog(
                bookId = id,
                bookmark = bookmark,
                chapterTitle = chapterTitle,
                onDismiss = { selectedBookmark = null },
                onPlay = {
                    viewModel.playBookmark(bookmark.positionSeconds)
                    selectedBookmark = null
                },
            )
        }
    }
}

@Composable
private fun ChapterTableHeader(
    showStart: Boolean,
    showDuration: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Title",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        if (showStart) {
            Text(
                "Start",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(58.dp),
            )
        }
        if (showDuration) {
            Text(
                "Duration",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(68.dp),
            )
        }
    }
}

@Composable
private fun ChapterTiming(
    startSeconds: Double,
    endSeconds: Double,
    current: Boolean,
    showStart: Boolean,
    showDuration: Boolean,
) {
    if (!showStart && !showDuration) return

    val color =
        if (current) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier =
            Modifier.width(
                when {
                    showStart && showDuration -> 126.dp
                    showStart -> 58.dp
                    else -> 68.dp
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showStart) {
            Text(
                TimeFormat.clock(startSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.width(58.dp),
            )
        }
        if (showDuration) {
            Text(
                TimeFormat.shortDuration((endSeconds - startSeconds).coerceAtLeast(0.0)),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.width(68.dp),
            )
        }
    }
}

@Composable
private fun AddBookmarkDialog(
    positionLabel: String,
    onDismiss: () -> Unit,
    onAdd: (String?) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add bookmark at $positionLabel") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(note.ifBlank { null }) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun BookmarkActionDialog(
    bookId: String,
    bookmark: BookmarkEntity,
    chapterTitle: String?,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    previewViewModel: BookmarkPreviewViewModel = hiltViewModel(),
) {
    val previewState by previewViewModel.state.collectAsStateWithLifecycle()
    val startPositionMs =
        ((bookmark.positionSeconds - previewState.bookmarkContextSeconds) * 1000)
            .toLong()
            .coerceAtLeast(0)

    LaunchedEffect(bookId, bookmark.id, previewState.bookmarkContextSeconds) {
        previewViewModel.prepare(bookId, startPositionMs)
    }

    DisposableEffect(Unit) {
        onDispose { previewViewModel.stop() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bookmark at ${TimeFormat.clock(bookmark.positionSeconds)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bookmark.note?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    chapterTitle ?: "Preview this bookmark without changing book progress.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { previewViewModel.seekBy(-previewState.jumpBackwardSeconds * 1000L) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Jump preview backward ${previewState.jumpBackwardSeconds}s",
                            )
                        }
                        Text("${previewState.jumpBackwardSeconds}s", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledIconButton(
                        onClick = previewViewModel::playPause,
                        enabled = previewState.error == null,
                    ) {
                        Icon(
                            if (previewState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (previewState.isPlaying) "Pause preview" else "Play preview",
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { previewViewModel.seekBy(previewState.jumpForwardSeconds * 1000L) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Jump preview forward ${previewState.jumpForwardSeconds}s",
                            )
                        }
                        Text("${previewState.jumpForwardSeconds}s", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Text(
                    TimeFormat.clock(previewState.positionMs / 1000.0),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                val duration = previewState.durationMs.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { (previewState.positionMs.toFloat() / duration).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    drawStopIndicator = {},
                )

                previewState.error?.let { error ->
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onPlay) { Text("Play from here") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private data class BookmarkChapterGroup(
    val chapterKey: String,
    val title: String,
    val bookmarks: List<BookmarkEntity>,
)

private fun groupedBookmarks(
    bookmarks: List<BookmarkEntity>,
    chapters: List<live.pageless.mobile.data.local.ChapterEntity>,
): List<BookmarkChapterGroup> {
    val sortedBookmarks = bookmarks.sortedBy { it.positionSeconds }
    if (chapters.isEmpty()) {
        return listOf(BookmarkChapterGroup("unknown", "Bookmarks", sortedBookmarks))
    }

    val spans =
        chapters.map {
            object : Chapters.Span {
                override val startSeconds = it.startSeconds
                override val endSeconds = it.endSeconds
            }
        }

    return sortedBookmarks
        .groupBy { bookmark -> Chapters.currentIndex(spans, bookmark.positionSeconds) }
        .toSortedMap(compareBy(nullsLast()) { it })
        .map { (chapterIndex, groupBookmarks) ->
            val chapter = chapterIndex?.let { chapters.getOrNull(it) }
            BookmarkChapterGroup(
                chapterKey = chapter?.id ?: "unknown",
                title = chapter?.let { it.title ?: "Chapter ${it.index + 1}" } ?: "Unknown chapter",
                bookmarks = groupBookmarks,
            )
        }
}

@Composable
private fun DownloadControl(
    completed: Boolean,
    downloading: Boolean,
    fraction: Float?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            downloading -> {
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        drawStopIndicator = {},
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        fraction?.let { "Downloading ${(it * 100).toInt()}%" } ?: "Downloading…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Text("Cancel")
                    }
                }
            }

            completed -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text("Downloaded", modifier = Modifier.padding(start = 6.dp))
                    }
                    TextButton(onClick = onDelete) { Text("Remove") }
                }
            }

            else -> {
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text("Download for offline", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/** Book cover artwork, or a placeholder tile with the title when unavailable. */
@Composable
private fun CoverArt(
    url: String?,
    title: String,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier =
            Modifier
                .padding(vertical = 8.dp)
                .size(220.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A label/value row in the metadata table; renders nothing when value is blank. */
@Composable
private fun MetadataRow(
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) return
    // Give the row a fixed height and center each cell's content within it, so
    // the small label and larger value sit on the same middle line. The label
    // Box uses contentAlignment to center the caps regardless of glyph metrics.
    val noFontPadding = PlatformTextStyle(includeFontPadding = false)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(110.dp)
                    .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(platformStyle = noFontPadding),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(platformStyle = noFontPadding),
            )
        }
    }
}

/** A metadata row with individually tappable normalized values. */
@Composable
private fun MetadataLinksRow(
    label: String,
    values: List<BookMetadataLink>,
    onClick: (String) -> Unit,
) {
    if (values.isEmpty()) return
    val noFontPadding = PlatformTextStyle(includeFontPadding = false)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.width(110.dp).height(36.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(platformStyle = noFontPadding),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            values.forEach { value ->
                Text(
                    value.name,
                    modifier = Modifier.fillMaxWidth().clickable { onClick(value.id) }.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(platformStyle = noFontPadding),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Series label "Name #sequence" (or just the name when no sequence). */
internal fun seriesLabel(
    name: String,
    sequence: String?,
): String = if (sequence.isNullOrBlank()) name else "$name #$sequence"

/** The year to display: from the ISO published date, falling back to the year int. */
internal fun publishYear(
    publishedDate: String?,
    publishedYear: Int?,
): String? {
    val fromDate = publishedDate?.take(4)?.toIntOrNull()
    return (fromDate ?: publishedYear)?.toString()
}

/** Listening percentage (0..100), full when finished. */
internal fun progressPercent(
    progress: ProgressEntity,
    isActive: Boolean,
    playerPositionMs: Long,
): Int {
    if (progress.finished) return 100
    val duration = progress.durationSeconds
    if (duration <= 0.0) return 0
    val current = if (isActive) playerPositionMs / 1000.0 else progress.currentSeconds
    return ((current / duration) * 100).toInt().coerceIn(0, 100)
}

/** "N remaining" label; empty when finished or duration unknown. */
internal fun remainingLabel(
    progress: ProgressEntity,
    durationSeconds: Double,
    isActive: Boolean,
    playerPositionMs: Long,
): String? {
    if (progress.finished || durationSeconds <= 0.0) return null
    val current = if (isActive) playerPositionMs / 1000.0 else progress.currentSeconds
    val remaining = (durationSeconds - current).coerceAtLeast(0.0)
    return "${TimeFormat.duration(remaining)} remaining"
}

/** A progress panel mirroring the web app: finished badge or percent + bar. */
@Composable
private fun ProgressPill(
    finished: Boolean,
    percent: Int,
    remaining: String?,
    currentChapterTitle: String?,
    startedAt: String?,
    finishedAt: String?,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val finishedColor =
        androidx.compose.ui.graphics
            .Color(0xFF22C55E)

    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (finished) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = finishedColor,
                        modifier = Modifier.size(18.dp),
                    )
                    androidx.compose.foundation.layout
                        .Spacer(Modifier.width(6.dp))
                    Text("Finished", style = MaterialTheme.typography.titleSmall, color = finishedColor)
                    finishedAt?.let {
                        androidx.compose.foundation.layout
                            .Spacer(Modifier.width(6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = muted)
                    }
                }
            } else {
                Text(
                    "Your Progress: $percent%",
                    style = MaterialTheme.typography.titleSmall,
                )
                remaining?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = muted)
                }
                currentChapterTitle?.let {
                    Text(
                        "Current chapter: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }

            startedAt?.let {
                Text("Started $it", style = MaterialTheme.typography.bodyMedium, color = muted)
            }

            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(50)),
                drawStopIndicator = {},
            )
        }
    }
}

/**
 * Renders an HTML book description, collapsed to [collapsedMaxLines] with a
 * "Read more" / "Read less" toggle when the text overflows.
 */
@Composable
private fun DescriptionText(
    html: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 5,
) {
    val text = remember(html) { AnnotatedString.fromHtml(html) }
    var expanded by remember(html) { mutableStateOf(false) }
    var hasOverflow by remember(html) { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                // Only latch overflow while collapsed, so toggling back to
                // collapsed keeps the control visible.
                if (!expanded) hasOverflow = result.hasVisualOverflow
            },
        )
        if (hasOverflow || expanded) {
            Text(
                text = if (expanded) "Read less" else "Read more",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .clickable { expanded = !expanded },
            )
        }
    }
}
