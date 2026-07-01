package live.pageless.mobile.ui.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import live.pageless.mobile.core.DateTimeFormat
import live.pageless.mobile.core.TimeFormat
import live.pageless.mobile.data.local.PlaybackEventEntity
import live.pageless.mobile.ui.components.ConnectionStatusIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookHistoryScreen(
    onBack: () -> Unit,
    viewModel: BookHistoryViewModel = hiltViewModel(),
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("History")
                        Spacer(Modifier.width(8.dp))
                        ConnectionStatusIcon()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "History for ${book?.title ?: "Book"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            if (events.isEmpty()) {
                item {
                    Text(
                        "No listening history yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val grouped = events.groupBy { it.localDate() }
                grouped.forEach { (date, dayEvents) ->
                    item(key = "header-$date") {
                        Text(
                            date.dayLabel(dateFormat),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }

                    items(dayEvents, key = { it.id }) { event ->
                        HistoryEventRow(event, timeFormat)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEventRow(
    event: PlaybackEventEntity,
    timeFormat: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            DateTimeFormat.formatTime(event.timestamp, timeFormat).orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )

        Icon(
            event.icon(),
            contentDescription = null,
            tint = if (event.event == "Play") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                event.event,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.serverSyncSuccess == true) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = "Synced",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Text(
            TimeFormat.clock(event.positionSeconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun PlaybackEventEntity.icon(): ImageVector =
    when (event) {
        "Play" -> Icons.Default.PlayArrow
        "Pause" -> Icons.Default.Pause
        "Seek" -> Icons.Default.SwapHoriz
        "Save" -> Icons.Default.Save
        "Stop" -> Icons.Default.Stop
        else -> Icons.Default.Save
    }

private fun PlaybackEventEntity.instant(): Instant =
    runCatching { Instant.parse(timestamp) }
        .getOrElse { Instant.EPOCH }

private fun PlaybackEventEntity.localDate(): LocalDate = instant().atZone(ZoneId.systemDefault()).toLocalDate()

private fun LocalDate.dayLabel(dateFormat: String): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> DateTimeFormat.formatDate(toString(), dateFormat).orEmpty()
    }
}
