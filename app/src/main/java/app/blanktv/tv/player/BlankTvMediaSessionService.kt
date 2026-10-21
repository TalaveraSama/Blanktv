package app.blanktv.tv.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.blanktv.tv.BlankTvApp

/**
 * Publishes the shared player as a media session so the TV remote's transport keys and any
 * external controller (Google Assistant, cast surfaces) reach playback.
 */
class BlankTvMediaSessionService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = (application as BlankTvApp).container.playback.player
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // A TV app has no background life worth keeping a decoder alive for.
        session?.player?.stop()
        stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
