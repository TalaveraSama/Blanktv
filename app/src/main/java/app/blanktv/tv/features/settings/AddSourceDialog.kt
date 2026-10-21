package app.blanktv.tv.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.blanktv.tv.R
import app.blanktv.tv.domain.SourceKind

internal fun SourceKind.labelRes(): Int = when (this) {
    SourceKind.M3U -> R.string.source_m3u
    SourceKind.Xtream -> R.string.source_xtream
    SourceKind.Stalker -> R.string.source_stalker
}

/**
 * Add-source dialog covering the three supported protocols. The mode selector swaps the field set,
 * and the confirm button only enables once the fields that protocol actually needs are filled —
 * a Stalker portal without a MAC, or an Xtream panel without a password, cannot authenticate.
 */
@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onM3u: (name: String, url: String) -> Unit,
    onXtream: (name: String, baseUrl: String, username: String, password: String) -> Unit,
    onStalker: (name: String, portalUrl: String, mac: String) -> Unit,
) {
    var mode by remember { mutableIntStateOf(SourceKind.M3U.code) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var portal by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }

    val canConfirm = when (SourceKind.fromCode(mode)) {
        SourceKind.M3U -> url.isNotBlank()
        SourceKind.Xtream -> host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        SourceKind.Stalker -> portal.isNotBlank() && mac.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_add_playlist)) },
        text = {
            Column {
                Row {
                    SourceKind.entries.forEach { kind ->
                        TextButton(onClick = { mode = kind.code }) {
                            Text(
                                text = stringResource(kind.labelRes()),
                                color = if (mode == kind.code) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_playlist_name)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                when (SourceKind.fromCode(mode)) {
                    SourceKind.M3U -> OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.settings_playlist_url)) },
                        singleLine = true,
                    )

                    SourceKind.Xtream -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(stringResource(R.string.field_host)) },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(R.string.field_username)) },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.field_password)) },
                            singleLine = true,
                        )
                    }

                    SourceKind.Stalker -> {
                        OutlinedTextField(
                            value = portal,
                            onValueChange = { portal = it },
                            label = { Text(stringResource(R.string.field_portal)) },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mac,
                            onValueChange = { mac = it },
                            label = { Text(stringResource(R.string.field_mac)) },
                            singleLine = true,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (SourceKind.fromCode(mode)) {
                        SourceKind.M3U -> onM3u(name, url.trim())
                        SourceKind.Xtream -> onXtream(name, host.trim(), username.trim(), password)
                        SourceKind.Stalker -> onStalker(name, portal.trim(), mac.trim())
                    }
                },
                enabled = canConfirm,
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
