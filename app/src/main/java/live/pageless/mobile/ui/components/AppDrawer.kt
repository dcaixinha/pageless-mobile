package live.pageless.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Destinations reachable from the app drawer. */
enum class DrawerDestination { HOME, SERIES, COLLECTIONS, PLAYLISTS, ACCOUNT, SETTINGS }

/**
 * Contents of the modal navigation drawer: Home, Account, and Settings.
 */
@Composable
fun AppDrawerContent(
    username: String?,
    onSelect: (DrawerDestination) -> Unit,
) {
    // Tint the drawer items with the brand purple to match the app bar.
    val itemColors =
        NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.primary,
        )

    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = username?.let { "Welcome, $it" } ?: "Pageless",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp),
            )
            HorizontalDivider()
            Spacer(Modifier.padding(top = 8.dp))

            NavigationDrawerItem(
                label = { Text("Home") },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                selected = false,
                colors = itemColors,
                onClick = { onSelect(DrawerDestination.HOME) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Series") },
                icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) },
                selected = false,
                colors = itemColors,
                onClick = { onSelect(DrawerDestination.SERIES) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Collections") },
                icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                selected = false,
                colors = itemColors,
                onClick = { onSelect(DrawerDestination.COLLECTIONS) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Playlists") },
                icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                selected = false,
                colors = itemColors,
                onClick = { onSelect(DrawerDestination.PLAYLISTS) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Account") },
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                selected = false,
                colors = itemColors,
                onClick = { onSelect(DrawerDestination.ACCOUNT) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Settings") },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                selected = false,
                colors = itemColors,
                onClick = { onSelect(DrawerDestination.SETTINGS) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
