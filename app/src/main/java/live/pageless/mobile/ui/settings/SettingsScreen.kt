package live.pageless.mobile.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import live.pageless.mobile.data.local.ThemeMode
import live.pageless.mobile.ui.components.ConnectionStatusIcon
import live.pageless.mobile.ui.components.PrivacyPolicyRow

private val JUMP_OPTIONS = listOf(5, 10, 15, 20, 30, 45, 60)
private val BOOKMARK_CONTEXT_OPTIONS = listOf(0, 5, 10, 15, 20, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Settings")
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
        ) {
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )

            ThemeModeRow(
                selected = settings.themeMode,
                onSelect = viewModel::setThemeMode,
            )
            HorizontalDivider()

            Text(
                "Player",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )

            SwitchRow(
                label = "Use chapter track in mini-player",
                description = "Show mini-player progress within the current chapter.",
                checked = settings.useChapterTrack,
                onChange = viewModel::setUseChapterTrack,
            )
            HorizontalDivider()

            SwitchRow(
                label = "Show total track on Now Playing",
                description = "Show the whole-book progress bar on the full player.",
                checked = settings.showTotalTrackOnNowPlaying,
                onChange = viewModel::setShowTotalTrackOnNowPlaying,
            )
            HorizontalDivider()

            SwitchRow(
                label = "Show chapter track on Now Playing",
                description = "Show the current-chapter progress bar on the full player.",
                checked = settings.showChapterTrackOnNowPlaying,
                onChange = viewModel::setShowChapterTrackOnNowPlaying,
            )
            HorizontalDivider()

            JumpRow(
                label = "Jump forward amount",
                seconds = settings.jumpForwardSeconds,
                onSelect = viewModel::setJumpForward,
            )
            HorizontalDivider()

            JumpRow(
                label = "Jump backward amount",
                seconds = settings.jumpBackwardSeconds,
                onSelect = viewModel::setJumpBackward,
            )
            HorizontalDivider()

            SwitchRow(
                label = "Allow position seeking on media notification controls",
                checked = settings.allowSeekFromNotification,
                onChange = viewModel::setAllowSeekFromNotification,
            )
            HorizontalDivider()

            Text(
                "Chapters",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )

            SwitchRow(
                label = "Show chapter start time",
                description = "Show each chapter's start timestamp on book details.",
                checked = settings.showChapterStartOnBookDetail,
                onChange = viewModel::setShowChapterStartOnBookDetail,
            )
            HorizontalDivider()

            SwitchRow(
                label = "Show chapter duration",
                description = "Show each chapter's duration on book details.",
                checked = settings.showChapterDurationOnBookDetail,
                onChange = viewModel::setShowChapterDurationOnBookDetail,
            )
            HorizontalDivider()

            Text(
                "Bookmarks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )

            JumpRow(
                label = "Bookmark context time",
                seconds = settings.bookmarkContextSeconds,
                description = "Preview playback starts this many seconds before the bookmark.",
                options = BOOKMARK_CONTEXT_OPTIONS,
                onSelect = viewModel::setBookmarkContext,
            )
            HorizontalDivider()

            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )

            PrivacyPolicyRow()
            HorizontalDivider()
        }
    }
}

@Composable
private fun ThemeModeRow(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Theme", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Choose how Pageless should look on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(999.dp),
                    ).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ThemeModeOption(
                label = "System",
                icon = Icons.Default.BrightnessAuto,
                selected = selected == ThemeMode.SYSTEM,
                onClick = { onSelect(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f),
            )
            ThemeModeOption(
                label = "Dark",
                icon = Icons.Default.DarkMode,
                selected = selected == ThemeMode.DARK,
                onClick = { onSelect(ThemeMode.DARK) },
                modifier = Modifier.weight(1f),
            )
            ThemeModeOption(
                label = "Light",
                icon = Icons.Default.LightMode,
                selected = selected == ThemeMode.LIGHT,
                onClick = { onSelect(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) colors.primary.copy(alpha = 0.22f) else androidx.compose.ui.graphics.Color.Transparent,
        contentColor = if (selected) colors.primary else colors.onSurface,
        border = if (selected) BorderStroke(1.dp, colors.primary.copy(alpha = 0.45f)) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
        )
    }
}

@Composable
private fun JumpRow(
    label: String,
    seconds: Int,
    description: String? = null,
    options: List<Int> = JUMP_OPTIONS,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text("$seconds seconds")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("$option seconds") },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
