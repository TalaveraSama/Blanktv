package app.blanktv.tv.features.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import app.blanktv.tv.view.LibraryViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The channel list. Ordering follows the playlist's own channel numbers where present, falling
 * back to alphabetical, so a provider's numbering survives the import.
 */
@Composable
fun LiveScreen(vm: LibraryViewModel, onOpenChannel: (ChannelEntity) -> Unit) {
    val live by vm.live.collectAsStateWithLifecycle()
    val favouriteIds by vm.favouriteIds.collectAsStateWithLifecycle()

    if (live.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.live_title),
            body = stringResource(R.string.empty_library_body),
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        Text(
            text = stringResource(R.string.live_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(live, key = { it.id }) { channel ->
                ChannelRow(
                    channel = channel,
                    isFavourite = favouriteIds.contains(channel.id),
                    onClick = { onOpenChannel(channel) },
                )
            }
        }
    }
}
