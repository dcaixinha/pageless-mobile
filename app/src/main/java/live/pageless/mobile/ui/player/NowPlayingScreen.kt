package live.pageless.mobile.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import live.pageless.mobile.core.Chapters
import live.pageless.mobile.core.TimeFormat
import live.pageless.mobile.data.local.BookmarkEntity
import live.pageless.mobile.data.local.ChapterEntity
import live.pageless.mobile.ui.book.BookmarkPreviewViewModel

private val SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
private const val CLOSE_DRAG_THRESHOLD_PX = 120f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onClose: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val coverUrl by viewModel.coverUrl.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showChapters by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showAddBookmark by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var selectedBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }

    val currentChapter = chapters.getOrNull(viewModel.currentChapterIndex() ?: -1)

    Scaffold(
        topBar = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .swipeDownToClose(onClose)
                        .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close player")
                }
                Text(
                    state.title ?: "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Spacer(Modifier.size(48.dp))
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // Cover
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .swipeDownToClose(onClose)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = state.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        state.title ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                currentChapter?.let { it.title ?: "Chapter ${it.index + 1}" } ?: (state.title ?: ""),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            state.author?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Chapter-relative progress bar (when enabled and there's a chapter).
            if (settings.showChapterTrackOnNowPlaying && currentChapter != null) {
                val chStart = (currentChapter.startSeconds * 1000).toLong()
                val chEnd = (currentChapter.endSeconds * 1000).toLong()
                ScrubBar(
                    positionMs = state.positionMs,
                    startMs = chStart,
                    endMs = chEnd,
                    onSeek = { viewModel.seekTo(it) },
                )
            }

            // Whole-book progress bar.
            if (settings.showTotalTrackOnNowPlaying) {
                ScrubBar(
                    positionMs = state.positionMs,
                    startMs = 0,
                    endMs = state.durationMs,
                    onSeek = { viewModel.seekTo(it) },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { viewModel.jumpChapter(forward = false) },
                    enabled = chapters.isNotEmpty(),
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous chapter", modifier = Modifier.size(28.dp))
                }
                SkipButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    seconds = settings.jumpBackwardSeconds,
                    contentDescription = "Back ${settings.jumpBackwardSeconds}s",
                    onClick = viewModel::skipBackward,
                )
                FilledIconButton(
                    onClick = viewModel::playPause,
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp),
                    )
                }
                SkipButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    seconds = settings.jumpForwardSeconds,
                    contentDescription = "Forward ${settings.jumpForwardSeconds}s",
                    onClick = viewModel::skipForward,
                )
                IconButton(
                    onClick = { viewModel.jumpChapter(forward = true) },
                    enabled = chapters.isNotEmpty(),
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next chapter", modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom action row: bookmark · speed · chapters
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showBookmarks = true }) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks")
                }
                IconButton(onClick = { showSpeed = true }) {
                    Text(
                        "${state.speed.cleanFormat()}x",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                IconButton(onClick = { showChapters = true }, enabled = chapters.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Chapters")
                }
            }
        }
    }

    if (showChapters) {
        ChaptersSheet(
            chapters = chapters,
            currentIndex = viewModel.currentChapterIndex(),
            showStart = settings.showChapterStartOnBookDetail,
            showDuration = settings.showChapterDurationOnBookDetail,
            onSelect = { ch ->
                viewModel.seekTo((ch.startSeconds * 1000).toLong())
                showChapters = false
            },
            onDismiss = { showChapters = false },
        )
    }

    if (showBookmarks) {
        BookmarksSheet(
            bookmarks = bookmarks,
            chapters = chapters,
            onSelect = { bm ->
                selectedBookmark = bm
                showBookmarks = false
            },
            onDelete = { viewModel.deleteBookmark(it.id) },
            onAdd = { showAddBookmark = true },
            onDismiss = { showBookmarks = false },
        )
    }

    if (showAddBookmark) {
        AddBookmarkDialog(
            positionLabel = TimeFormat.clock(state.positionMs / 1000.0),
            onDismiss = { showAddBookmark = false },
            onAdd = { note ->
                viewModel.addBookmark(note)
                showAddBookmark = false
            },
        )
    }

    selectedBookmark?.let { bookmark ->
        state.bookId?.let { bookId ->
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
                bookId = bookId,
                bookmark = bookmark,
                chapterTitle = chapterTitle,
                onDismiss = { selectedBookmark = null },
                onPlay = {
                    viewModel.seekTo((bookmark.positionSeconds * 1000).toLong())
                    selectedBookmark = null
                },
            )
        }
    }

    if (showSpeed) {
        SpeedSheet(
            current = state.speed,
            onSelect = {
                viewModel.setSpeed(it)
                showSpeed = false
            },
            onDismiss = { showSpeed = false },
        )
    }
}

/** A skip button showing a replay/forward icon with the configured seconds under it. */
@Composable
private fun SkipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    seconds: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(30.dp))
        }
        Text("${seconds}s", style = MaterialTheme.typography.labelSmall)
    }
}

private fun Modifier.swipeDownToClose(onClose: () -> Unit): Modifier =
    pointerInput(onClose) {
        var dragAmount = 0f
        detectVerticalDragGestures(
            onDragStart = { dragAmount = 0f },
            onVerticalDrag = { _, dragDelta -> dragAmount += dragDelta },
            onDragEnd = {
                if (dragAmount > CLOSE_DRAG_THRESHOLD_PX) onClose()
                dragAmount = 0f
            },
            onDragCancel = { dragAmount = 0f },
        )
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScrubBar(
    positionMs: Long,
    startMs: Long,
    endMs: Long,
    onSeek: (Long) -> Unit,
) {
    val span = (endMs - startMs).coerceAtLeast(1)
    val elapsed = (positionMs - startMs).coerceIn(0, span)
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableStateOf(0f) }
    val fraction = if (scrubbing) scrubValue else (elapsed.toFloat() / span)

    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Slider(
            value = fraction.coerceIn(0f, 1f),
            onValueChange = {
                scrubbing = true
                scrubValue = it
            },
            onValueChangeFinished = {
                scrubbing = false
                onSeek(startMs + (scrubValue * span).toLong())
            },
            interactionSource = interactionSource,
            // Remove the M3 track "stop indicator" dot at the end of the bar.
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    drawStopIndicator = null,
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(TimeFormat.clock(elapsed / 1000.0), style = MaterialTheme.typography.labelSmall)
            Text("-" + TimeFormat.clock((span - elapsed) / 1000.0), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChaptersSheet(
    chapters: List<ChapterEntity>,
    currentIndex: Int?,
    showStart: Boolean,
    showDuration: Boolean,
    onSelect: (ChapterEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Text(
            "Chapters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item(key = "chapter-header") {
                ChapterTableHeader(showStart = showStart, showDuration = showDuration)
            }

            itemsIndexed(chapters, key = { _, ch -> ch.id }) { index, ch ->
                val isCurrent = index == currentIndex
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
                            ).clickable { onSelect(ch) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        ch.title ?: "Chapter ${ch.index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    ChapterTiming(
                        startSeconds = ch.startSeconds,
                        endSeconds = ch.endSeconds,
                        current = isCurrent,
                        showStart = showStart,
                        showDuration = showDuration,
                    )
                }
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(16.dp))
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksSheet(
    bookmarks: List<BookmarkEntity>,
    chapters: List<ChapterEntity>,
    onSelect: (BookmarkEntity) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Bookmarks", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = "Add bookmark")
            }
        }
        if (bookmarks.isEmpty()) {
            Text(
                "No bookmarks yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                groupedBookmarks(bookmarks, chapters).forEach { group ->
                    item(key = "chapter-${group.chapterKey}") {
                        Text(
                            group.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(group.bookmarks, key = { it.id }) { bm ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(bm) }
                                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                TimeFormat.clock(bm.positionSeconds),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Text(
                                bm.note ?: "Bookmark",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onDelete(bm) }) {
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
        Spacer(Modifier.height(16.dp))
    }
}

private data class BookmarkChapterGroup(
    val chapterKey: String,
    val title: String,
    val bookmarks: List<BookmarkEntity>,
)

private fun groupedBookmarks(
    bookmarks: List<BookmarkEntity>,
    chapters: List<ChapterEntity>,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Text(
            "Playback speed",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SPEEDS.forEach { s ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(s) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    "${s.cleanFormat()}x",
                    color = if (s == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (s == current) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
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
        confirmButton = { TextButton(onClick = { onAdd(note.ifBlank { null }) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Formats a speed like 1.0 -> "1", 1.25 -> "1.25". */
private fun Float.cleanFormat(): String = if (this % 1f == 0f) toInt().toString() else toString().trimEnd('0').trimEnd('.')
