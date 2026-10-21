package app.blanktv.tv.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.blanktv.tv.R
import app.blanktv.tv.data.db.PlaylistEntity
import app.blanktv.tv.di.AppContainer
import app.blanktv.tv.domain.SourceKind
import app.blanktv.tv.view.LibraryViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

/**
 * Playlist management and appearance. Adding a playlist is the one network write in the app, so
 * it reports progress (`busy`) and a human-readable outcome (`status`) instead of throwing.
 */
@Composable
fun SettingsScreen(vm: LibraryViewModel, container: AppContainer) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val dark by container.settings.darkTheme.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.settings_playlists), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            playlists.forEach { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    busy = busy,
                    onSync = { vm.resync(playlist.id) },
                    onRemove = { vm.removePlaylist(playlist.id) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { showAdd = true }, enabled = !busy) {
                Text(stringResource(R.string.settings_add_playlist))
            }
            status?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.settings_theme_dark),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = dark, onCheckedChange = { value ->
                scope.launch { container.settings.setDarkTheme(value) }
            })
        }

        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_about_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showAdd) {
        AddSourceDialog(
            onDismiss = { showAdd = false },
            onM3u = { name, url ->
                vm.addM3u(name, url)
                showAdd = false
            },
            onXtream = { name, host, username, password ->
                vm.addXtream(name, host, username, password)
                showAdd = false
            },
            onStalker = { name, portal, mac ->
                vm.addStalker(name, portal, mac)
                showAdd = false
            },
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistEntity,
    busy: Boolean,
    onSync: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(SourceKind.fromCode(playlist.sourceKind).labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        val detail = buildString {
            append(playlist.channelCount).append(" entries")
            playlist.lastSyncedAt?.let { at ->
                append(" · synced ").append(DateFormat.getDateTimeInstance().format(Date(at)))
            }
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        playlist.url?.let { url ->
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
            if (playlist.url != null) {
                OutlinedButton(onClick = onSync, enabled = !busy) {
                    Text(stringResource(R.string.action_sync))
                }
            }
            OutlinedButton(onClick = onRemove, enabled = !busy) {
                Text(stringResource(R.string.action_remove))
            }
        }
    }
}


