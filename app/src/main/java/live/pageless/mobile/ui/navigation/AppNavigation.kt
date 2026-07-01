package live.pageless.mobile.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import live.pageless.mobile.data.repository.AuthRepository
import live.pageless.mobile.ui.account.AccountScreen
import live.pageless.mobile.ui.book.BookDetailScreen
import live.pageless.mobile.ui.book.BookHistoryScreen
import live.pageless.mobile.ui.collections.CollectionDetailScreen
import live.pageless.mobile.ui.collections.CollectionsScreen
import live.pageless.mobile.ui.components.AppDrawerContent
import live.pageless.mobile.ui.components.DrawerDestination
import live.pageless.mobile.ui.components.TopTab
import live.pageless.mobile.ui.home.HomeScreen
import live.pageless.mobile.ui.library.LibraryFilterCategory
import live.pageless.mobile.ui.library.LibraryScreen
import live.pageless.mobile.ui.login.LoginScreen
import live.pageless.mobile.ui.player.MiniPlayer
import live.pageless.mobile.ui.player.NowPlayingScreen
import live.pageless.mobile.ui.playlists.PlaylistDetailScreen
import live.pageless.mobile.ui.playlists.PlaylistsScreen
import live.pageless.mobile.ui.series.SeriesDetailScreen
import live.pageless.mobile.ui.series.SeriesScreen
import live.pageless.mobile.ui.settings.SettingsScreen
import javax.inject.Inject

/** How long the "press back again to exit" window stays open, in ms. */
private const val BACK_EXIT_WINDOW_MS = 2000L

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val LIBRARY_ROUTE = "library?filterCategory={filterCategory}&filterId={filterId}"
    const val BOOK = "book/{bookId}"
    const val BOOK_HISTORY = "book/{bookId}/history"
    const val SERIES = "series"
    const val SERIES_DETAIL = "series/{seriesId}"
    const val COLLECTIONS = "collections"
    const val COLLECTION_DETAIL = "collections/{collectionId}"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlists/{playlistId}"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"

    fun book(bookId: String) = "book/$bookId"

    fun bookHistory(bookId: String) = "book/$bookId/history"

    fun libraryFilter(
        category: LibraryFilterCategory,
        id: String,
    ) = "library?filterCategory=${category.name}&filterId=${Uri.encode(id)}"

    fun seriesDetail(id: String) = "series/$id"

    fun collectionDetail(id: String) = "collections/$id"

    fun playlistDetail(id: String) = "playlists/$id"
}

private fun TopTab.route(): String =
    when (this) {
        TopTab.HOME -> Routes.HOME
        TopTab.LIBRARY -> Routes.LIBRARY
    }

sealed interface AuthGate {
    data object Loading : AuthGate

    data object LoggedOut : AuthGate

    data object LoggedIn : AuthGate
}

@HiltViewModel
class RootViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
    ) : ViewModel() {
        val username: StateFlow<String?> =
            authRepository.displayName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val gate: StateFlow<AuthGate> =
            authRepository.token
                .map { if (it.isNullOrBlank()) AuthGate.LoggedOut else AuthGate.LoggedIn }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthGate.Loading)
    }

@Composable
fun AppNavigation(rootViewModel: RootViewModel = hiltViewModel()) {
    val gate by rootViewModel.gate.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    when (gate) {
        AuthGate.Loading ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        else -> {
            val start = if (gate == AuthGate.LoggedIn) Routes.HOME else Routes.LOGIN
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route
            val openBookId = backStack?.arguments?.getString("bookId")

            fun openBook(bookId: String) {
                // Avoid stacking duplicate detail screens for the same book.
                if (currentRoute == Routes.BOOK && openBookId == bookId) return
                navController.navigate(Routes.book(bookId)) { launchSingleTop = true }
            }

            fun openBookHistory(bookId: String) {
                navController.navigate(Routes.bookHistory(bookId)) { launchSingleTop = true }
            }

            fun switchTab(route: String) {
                if (currentRoute == route || (route == Routes.LIBRARY && currentRoute == Routes.LIBRARY_ROUTE)) return
                navController.navigate(route) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            fun logout() {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }

            // The full-screen player owns the whole screen (no mini-player).
            val fullScreen = currentRoute == Routes.PLAYER || currentRoute == Routes.LOGIN
            // Drawer-reachable top-level screens (drawer gesture + hamburger).
            val tabScreen =
                currentRoute in
                    setOf(
                        Routes.HOME,
                        Routes.LIBRARY_ROUTE,
                        Routes.SERIES,
                        Routes.COLLECTIONS,
                        Routes.PLAYLISTS,
                    )

            fun openPlayer() {
                if (currentRoute == Routes.PLAYER) return
                navController.navigate(Routes.PLAYER) { launchSingleTop = true }
            }

            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val drawerScope = rememberCoroutineScope()
            val username by rootViewModel.username.collectAsStateWithLifecycle()

            fun closeDrawer() = drawerScope.launch { drawerState.close() }

            LaunchedEffect(tabScreen) {
                if (!tabScreen && drawerState.isOpen) drawerState.close()
            }

            // Double-tap / swipe back on the start destination to exit: the
            // first back shows a warning, a second within the window quits.
            val context = LocalContext.current
            val lastBackPress = remember { mutableLongStateOf(0L) }
            BackHandler(enabled = currentRoute == start) {
                when {
                    drawerState.isOpen -> closeDrawer()

                    System.currentTimeMillis() - lastBackPress.longValue < BACK_EXIT_WINDOW_MS -> {
                        (context as? android.app.Activity)?.finish()
                    }

                    else -> {
                        lastBackPress.longValue = System.currentTimeMillis()
                        Toast
                            .makeText(context, "Press back again to exit", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                // Only allow the drawer on the primary tab screens.
                gesturesEnabled = tabScreen,
                drawerContent = {
                    AppDrawerContent(
                        username = username,
                        onSelect = { dest ->
                            drawerScope.launch {
                                drawerState.close()
                                when (dest) {
                                    DrawerDestination.HOME ->
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.HOME) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    DrawerDestination.SERIES ->
                                        navController.navigate(Routes.SERIES) { launchSingleTop = true }
                                    DrawerDestination.COLLECTIONS ->
                                        navController.navigate(Routes.COLLECTIONS) { launchSingleTop = true }
                                    DrawerDestination.PLAYLISTS ->
                                        navController.navigate(Routes.PLAYLISTS) { launchSingleTop = true }
                                    DrawerDestination.ACCOUNT ->
                                        navController.navigate(Routes.ACCOUNT) { launchSingleTop = true }
                                    DrawerDestination.SETTINGS ->
                                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                                }
                            }
                        },
                    )
                },
            ) {
                Scaffold(
                    // Don't apply window insets here; each screen's own Scaffold/
                    // TopAppBar handles the status-bar (top) inset. Applying them at
                    // both levels double-counts the status bar and leaves a large
                    // gap at the top of every screen.
                    contentWindowInsets = WindowInsets(0),
                    bottomBar = {
                        // The mini-player owns the bottom; navigation lives at the
                        // top of the Home/Library screens (see their tab row).
                        if (!fullScreen) {
                            MiniPlayer(onOpen = { openPlayer() })
                        }
                    },
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = start,
                        modifier = Modifier.padding(padding),
                    ) {
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onLoggedIn = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Routes.HOME) {
                            HomeScreen(
                                onOpenBook = { bookId -> openBook(bookId) },
                                onOpenDrawer = { drawerScope.launch { drawerState.open() } },
                                onSelectTab = { tab -> switchTab(tab.route()) },
                            )
                        }
                        composable(
                            route = Routes.LIBRARY_ROUTE,
                            arguments =
                                listOf(
                                    navArgument("filterCategory") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("filterId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                ),
                        ) {
                            LibraryScreen(
                                onOpenBook = { bookId -> openBook(bookId) },
                                onOpenDrawer = { drawerScope.launch { drawerState.open() } },
                                onSelectTab = { tab -> switchTab(tab.route()) },
                            )
                        }
                        composable(
                            route = Routes.BOOK,
                            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
                        ) {
                            val bookId = checkNotNull(it.arguments?.getString("bookId"))
                            BookDetailScreen(
                                onBack = { navController.popBackStack() },
                                onOpenHistory = { openBookHistory(bookId) },
                                onOpenLibraryFilter = { category, id ->
                                    navController.navigate(Routes.libraryFilter(category, id)) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                        composable(Routes.SERIES) {
                            SeriesScreen(
                                onOpenSeries = { id ->
                                    navController.navigate(Routes.seriesDetail(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenDrawer = { drawerScope.launch { drawerState.open() } },
                            )
                        }
                        composable(
                            route = Routes.SERIES_DETAIL,
                            arguments = listOf(navArgument("seriesId") { type = NavType.StringType }),
                        ) {
                            SeriesDetailScreen(
                                onBack = { navController.popBackStack() },
                                onOpenBook = { bookId -> openBook(bookId) },
                            )
                        }
                        composable(Routes.COLLECTIONS) {
                            CollectionsScreen(
                                onOpenCollection = { id ->
                                    navController.navigate(Routes.collectionDetail(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenDrawer = { drawerScope.launch { drawerState.open() } },
                            )
                        }
                        composable(
                            route = Routes.COLLECTION_DETAIL,
                            arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
                        ) {
                            CollectionDetailScreen(
                                onBack = { navController.popBackStack() },
                                onOpenBook = { bookId -> openBook(bookId) },
                            )
                        }
                        composable(Routes.PLAYLISTS) {
                            PlaylistsScreen(
                                onOpenPlaylist = { id ->
                                    navController.navigate(Routes.playlistDetail(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenDrawer = { drawerScope.launch { drawerState.open() } },
                            )
                        }
                        composable(
                            route = Routes.PLAYLIST_DETAIL,
                            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                        ) {
                            PlaylistDetailScreen(
                                onBack = { navController.popBackStack() },
                                onOpenBook = { bookId -> openBook(bookId) },
                            )
                        }
                        composable(
                            route = Routes.BOOK_HISTORY,
                            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
                        ) {
                            BookHistoryScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = Routes.PLAYER,
                            enterTransition = { slideInVertically(animationSpec = tween(220)) { it } },
                            exitTransition = { slideOutVertically(animationSpec = tween(220)) { it } },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { slideOutVertically(animationSpec = tween(220)) { it } },
                        ) {
                            NowPlayingScreen(onClose = { navController.popBackStack() })
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.ACCOUNT) {
                            AccountScreen(
                                onBack = { navController.popBackStack() },
                                onSwitchServer = { logout() },
                            )
                        }
                    }
                }
            }
        }
    }
}
