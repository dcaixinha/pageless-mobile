package live.pageless.mobile.ui.series

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import live.pageless.mobile.ui.components.BookGridDetail
import live.pageless.mobile.ui.components.DetailBook

@Composable
fun SeriesDetailScreen(
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
) {
    val series by viewModel.series.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()

    BookGridDetail(
        title = series?.name ?: "Series",
        emptyText = "No books in this series.",
        books = books.map { DetailBook(it.id, it.title, it.author, it.coverUrl, it.sequence) },
        onBack = onBack,
        onOpenBook = onOpenBook,
    )
}
