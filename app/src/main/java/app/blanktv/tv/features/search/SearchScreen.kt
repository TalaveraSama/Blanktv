package app.blanktv.tv.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.blanktv.tv.R
import app.blanktv.tv.data.db.ChannelEntity
import app.blanktv.tv.ui.components.PosterCard
import app.blanktv.tv.view.LibraryViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Title search across every kind. The query is debounced in the ViewModel so a fast typer on an
 * on-screen keyboard does not fire a LIKE query per keystroke.
 */
@Composable
fun SearchScreen(vm: LibraryViewModel, onOpenChannel: (ChannelEntity) -> Unit) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val favouriteIds by vm.favouriteIds.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        Text(
            text = stringResource(R.string.nav_search),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        when {
            query.isBlank() -> Unit
            results.isEmpty() -> Text(
                text = stringResource(R.string.search_no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 176.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(results, key = { it.id }) { channel ->
                    PosterCard(
                        channel = channel,
                        isFavourite = favouriteIds.contains(channel.id),
                        onClick = { onOpenChannel(channel) },
                    )
                }
            }
        }
    }
}
