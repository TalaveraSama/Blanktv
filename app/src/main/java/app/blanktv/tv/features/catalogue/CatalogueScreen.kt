package app.blanktv.tv.features.catalogue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.blanktv.tv.R
import app.blanktv.tv.data.db.ChannelEntity
import app.blanktv.tv.domain.MediaKind
import app.blanktv.tv.ui.components.EmptyState
import app.blanktv.tv.ui.components.FocusBox
import app.blanktv.tv.ui.components.PosterCard
import app.blanktv.tv.view.LibraryViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Movies and Series share one screen shape: a group rail on the left, a poster grid on the right.
 * Groups come from the playlist's `group-title`; selecting one narrows the grid, "All" clears it.
 */
@Composable
fun CatalogueScreen(
    vm: LibraryViewModel,
    kind: MediaKind,
    onOpenChannel: (ChannelEntity) -> Unit,
) {
    val isMovie = kind == MediaKind.Movie
    val groups by (if (isMovie) vm.movieGroups else vm.seriesGroups).collectAsStateWithLifecycle()
    val selected by (if (isMovie) vm.movieGroup else vm.seriesGroup).collectAsStateWithLifecycle()
    val items by (if (isMovie) vm.movies else vm.series).collectAsStateWithLifecycle()
    val favouriteIds by vm.favouriteIds.collectAsStateWithLifecycle()
    val titleRes = if (isMovie) R.string.movies_title else R.string.series_title
    val select: (String?) -> Unit = { group ->
        if (isMovie) vm.setMovieGroup(group) else vm.setSeriesGroup(group)
    }

    if (items.isEmpty() && groups.isEmpty()) {
        EmptyState(
            title = stringResource(titleRes),
            body = stringResource(R.string.empty_library_body),
        )
        return
    }

    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .width(GROUP_RAIL_WIDTH)
                .fillMaxHeight()
                .padding(start = 32.dp, end = 8.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            item {
                GroupChip(
                    label = stringResource(R.string.action_all_groups),
                    selected = selected == null,
                    onClick = { select(null) },
                )
            }
            items(groups) { group ->
                GroupChip(
                    label = group,
                    selected = selected == group,
                    onClick = { select(group) },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 176.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 32.dp, top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            gridItems(items, key = { it.id }) { channel ->
                PosterCard(
                    channel = channel,
                    isFavourite = favouriteIds.contains(channel.id),
                    onClick = { onOpenChannel(channel) },
                )
            }
        }
    }
}

@Composable
private fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FocusBox(onClick = onClick, modifier = Modifier.fillMaxWidth()) { _ ->
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

private val GROUP_RAIL_WIDTH = 248.dp
