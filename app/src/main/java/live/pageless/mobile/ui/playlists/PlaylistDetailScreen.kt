package live.pageless.mobile.ui.playlists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import live.pageless.mobile.ui.components.BookGridDetail
import live.pageless.mobile.ui.components.DetailBook

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()

    BookGridDetail(
        title = playlist?.name ?: "Playlist",
        emptyText = "This playlist is empty.",
        books = books.map { DetailBook(it.id, it.title, it.author, it.coverUrl) },
        onBack = onBack,
        onOpenBook = onOpenBook,
    )
}
