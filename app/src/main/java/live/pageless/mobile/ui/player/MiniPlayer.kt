package live.pageless.mobile.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import live.pageless.mobile.core.TimeFormat

private const val OPEN_DRAG_THRESHOLD_PX = 80f

/**
 * Persistent bottom mini-player shown across screens whenever a book is loaded.
 * Shows the cover, title + current chapter, quick transport controls, and a
 * scrub-free progress bar. Tapping the body (not the buttons) opens the
 * full-screen player.
 */
@Composable
fun MiniPlayer(
    onOpen: () -> Unit,
    viewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coverUrl by viewModel.coverUrl.collectAsStateWithLifecycle()
    val chapterTitle by viewModel.currentChapterTitle.collectAsStateWithLifecycle()
    val window by viewModel.progressWindow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.connect() }

    if (!state.hasContent) return
    state.bookId ?: return

    // No own elevation/shadow: the mini-player is docked inside the shared
    // bottom-bar surface (see AppNavigation), so it should blend, not float.
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .swipeUpToOpen(onOpen)
                    .clickable { onOpen() }
                    // Keep content clear of gesture-nav / rounded corners / cutouts
                    // so times aren't clipped on devices with rounded screens.
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                        ),
                    ).padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cover thumbnail
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = state.title,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp)),
                    )
                } else {
                    Spacer(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        chapterTitle?.let { "${state.title ?: ""} | $it" } ?: (state.title ?: "Playing"),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    state.author?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Transport controls (stop click propagation to the row).
                IconButton(onClick = viewModel::skipBackward) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Skip backward")
                }
                FilledIconButton(onClick = viewModel::playPause) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                    )
                }
                IconButton(onClick = viewModel::skipForward) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Skip forward")
                }
            }

            Spacer(Modifier.height(6.dp))

            // Progress window is the chapter (when enabled) or the whole book.
            val (windowStart, windowEnd) = window
            val span = (windowEnd - windowStart).coerceAtLeast(1)
            val elapsed = (state.positionMs - windowStart).coerceIn(0, span)
            val fraction = elapsed.toFloat() / span

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                drawStopIndicator = {},
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(TimeFormat.clock(elapsed / 1000.0), style = MaterialTheme.typography.labelSmall)
                Text(
                    "-" + TimeFormat.clock((span - elapsed) / 1000.0),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun Modifier.swipeUpToOpen(onOpen: () -> Unit): Modifier =
    pointerInput(onOpen) {
        var dragAmount = 0f
        detectVerticalDragGestures(
            onDragStart = { dragAmount = 0f },
            onVerticalDrag = { _, dragDelta -> dragAmount += dragDelta },
            onDragEnd = {
                if (dragAmount < -OPEN_DRAG_THRESHOLD_PX) onOpen()
                dragAmount = 0f
            },
            onDragCancel = { dragAmount = 0f },
        )
    }
