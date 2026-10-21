package app.blanktv.tv.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.blanktv.tv.R
import app.blanktv.tv.data.db.ChannelEntity
import app.blanktv.tv.ui.components.ChannelRow
import app.blanktv.tv.ui.components.EmptyState
import app.blanktv.tv.ui.components.PosterCard
import app.blanktv.tv.view.LibraryViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Landing screen: horizontal rails over the library. Intentionally read-only — everything here is
 * a doorway into the section screens.
 */
@Composable
fun HomeScreen(
    vm: LibraryViewModel,
    onOpenChannel: (ChannelEntity) -> Unit,
    onGoSettings: () -> Unit,
) {
    val live by vm.live.collectAsStateWithLifecycle()
    val movies by vm.movies.collectAsStateWithLifecycle()
    val favourites by vm.favourites.collectAsStateWithLifecycle()
    val favouriteIds by vm.favouriteIds.collectAsStateWithLifecycle()

    if (live.isEmpty() && movies.isEmpty() && favourites.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.empty_library_title),
            body = stringResource(R.string.empty_library_body),
            actionLabel = stringResource(R.string.settings_add_playlist),
            onAction = onGoSettings,
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(24.dp))

        if (favourites.isNotEmpty()) {
            Rail(
                title = stringResource(R.string.favourites_title),
                items = favourites,
            ) { channel ->
                PosterCard(
                    channel = channel,
                    isFavourite = true,
                    onClick = { onOpenChannel(channel) },
                )
            }
        }
        if (live.isNotEmpty()) {
            Rail(
                title = stringResource(R.string.live_title),
                items = live.take(RAIL_LIMIT),
            ) { channel ->
                ChannelRow(
                    channel = channel,
                    isFavourite = favouriteIds.contains(channel.id),
                    onClick = { onOpenChannel(channel) },
                    modifier = Modifier.width(LIVE_TILE_WIDTH),
                )
            }
        }
        if (movies.isNotEmpty()) {
            Rail(
                title = stringResource(R.string.movies_title),
                items = movies.take(RAIL_LIMIT),
            ) { channel ->
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
private fun <T> Rail(
    title: String,
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(items) { item -> itemContent(item) }
        }
    }
}

private const val RAIL_LIMIT = 24
private val LIVE_TILE_WIDTH = 420.dp
