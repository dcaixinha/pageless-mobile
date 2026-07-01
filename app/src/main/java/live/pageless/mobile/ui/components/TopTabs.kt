package live.pageless.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The two top-level destinations, shown as a top tab row on Home and Library. */
enum class TopTab { HOME, LIBRARY }

/**
 * Top navigation tabs for the primary destinations. Rendered under each
 * screen's app bar so navigation lives at the top (the mini-player owns the
 * bottom of the screen).
 */
@Composable
fun TopTabs(
    selected: TopTab,
    onSelect: (TopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(selectedTabIndex = selected.ordinal, modifier = modifier) {
        Tab(
            selected = selected == TopTab.HOME,
            onClick = { onSelect(TopTab.HOME) },
            text = { Text("Home") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
        )
        Tab(
            selected = selected == TopTab.LIBRARY,
            onClick = { onSelect(TopTab.LIBRARY) },
            text = { Text("Library") },
            icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) },
        )
    }
}
