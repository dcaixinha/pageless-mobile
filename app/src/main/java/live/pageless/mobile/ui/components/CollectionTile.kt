package live.pageless.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * A square 2x2 cover-mosaic tile with a title + subtitle beneath, used on the
 * Series / Collections / Playlists index grids (mirrors the web app).
 */
@Composable
fun MosaicTile(
    title: String,
    subtitle: String?,
    coverUrls: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            // Up to four covers arranged as a 2x2 mosaic.
            val covers = coverUrls.take(4)
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    MosaicCell(covers.getOrNull(0), Modifier.weight(1f).fillMaxHeight())
                    MosaicCell(covers.getOrNull(1), Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    MosaicCell(covers.getOrNull(2), Modifier.weight(1f).fillMaxHeight())
                    MosaicCell(covers.getOrNull(3), Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MosaicCell(
    coverUrl: String?,
    modifier: Modifier,
) {
    if (coverUrl == null) {
        Box(modifier)
    } else {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}
