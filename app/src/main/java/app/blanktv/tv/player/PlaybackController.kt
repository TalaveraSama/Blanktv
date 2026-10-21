package app.blanktv.tv.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.blanktv.tv.data.db.ChannelEntity
import okhttp3.OkHttpClient

/**
 * Owns the single [ExoPlayer] instance for the process.
 *
 * One player, reused by every screen: creating an ExoPlayer per screen leaks renderers and audio
 * focus, and a TV box cannot afford two decoders alive at once. Screens ask this controller to
 * play/stop; the media-session service exposes the same instance to transport keys.
 *
 * OkHttp backs the data source so provider quirks (redirect chains, missing content-length, plain
 * HTTP) are handled by the same client the playlist import already trusts.
 */
class PlaybackController(context: Context, http: OkHttpClient) {

    val player: ExoPlayer by lazy {
        val dataSourceFactory = OkHttpDataSource.Factory(http)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory),
            )
            // Live-first tuning: short ramp-up so a channel change shows picture quickly, enough
            // headroom to survive provider bitrate stalls.
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs = */ 8_000,
                        /* maxBufferMs = */ 30_000,
                        /* bufferForPlaybackMs = */ 1_500,
                        /* bufferForPlaybackAfterRebufferMs = */ 3_000,
                    )
                    .build(),
            )
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    fun play(channel: ChannelEntity) {
        val item = MediaItem.Builder()
            .setMediaId(channel.id.toString())
            .setUri(channel.url)
            .setDisplayTitle(channel.title)
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
    }
}
