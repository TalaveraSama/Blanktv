package app.blanktv.tv.features.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.blanktv.tv.R
import app.blanktv.tv.data.db.ChannelEntity
import app.blanktv.tv.ui.components.EmptyState
import app.blanktv.tv.ui.components.PosterCard
import app.blanktv.tv.view.LibraryViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FavouritesScreen(vm: LibraryViewModel, onOpenChannel: (ChannelEntity) -> Unit) {
    val favourites by vm.favourites.collectAsStateWithLifecycle()

    if (favourites.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.favourites_title),
            body = stringResource(R.string.favourites_empty),
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        Text(
            text = stringResource(R.string.favourites_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 176.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(favourites, key = { it.id }) { channel ->
                PosterCard(channel = channel, isFavourite = true, onClick = { onOpenChannel(channel) })
            }
        }
    }
}
