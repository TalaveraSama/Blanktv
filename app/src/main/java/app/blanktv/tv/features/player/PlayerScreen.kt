package app.blanktv.tv.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import app.blanktv.tv.di.AppContainer
import app.blanktv.tv.view.PlayerViewModel
import kotlinx.coroutines.delay

/**
 * Full-bleed playback with a TV-appropriate HUD.
 *
 * PlayerView's stock controller is built for touch, so it is disabled and a lean overlay stands
 * in: title + group while the HUD is up, a buffering spinner, and a play/pause badge. The remote
 * drives everything through [onPreviewKeyEvent]:
 *   DPAD_CENTER / OK  toggle playback and wake the HUD
 *   DPAD_UP / DOWN    wake the HUD
 *   CHANNEL_UP / DOWN zap through the live list
 *   BACK              handled by navigation (pops to the previous screen)
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channelId: Long,
    vm: PlayerViewModel,
    container: AppContainer,
    onBack: () -> Unit,
) {
    val channel by vm.current.collectAsStateWithLifecycle()
    val playback = container.playback
    val player = playback.player

    var buffering by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var hudVisible by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(channelId) { vm.open(channelId) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // The HUD fades on its own so it never sits over the picture.
    LaunchedEffect(hudVisible) {
        if (hudVisible) {
            delay(HUD_TIMEOUT_MS)
            hudVisible = false
        }
    }

    // Leaving the player releases the decoder; re-entering starts clean.
    DisposableEffect(Unit) {
        onDispose { playback.stop() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.ChannelUp -> {
                        vm.step(1)
                        true
                    }

                    Key.ChannelDown -> {
                        vm.step(-1)
                        true
                    }

                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        hudVisible = true
                        if (player.isPlaying) player.pause() else player.play()
                        true
                    }

                    Key.DirectionUp, Key.DirectionDown -> {
                        hudVisible = true
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    player = playback.player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (hudVisible) {
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)),
                    )
                    .padding(24.dp),
            ) {
                Text(
                    text = channel?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                channel?.groupName?.let { group ->
                    Text(
                        text = group,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Row(Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        if (buffering) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center).size(56.dp),
            )
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

private const val HUD_TIMEOUT_MS = 3_500L
