package app.blanktv.tv.di

import android.content.Context
import app.blanktv.tv.data.db.BlankTvDatabase
import app.blanktv.tv.data.net.PlaylistFetcher
import app.blanktv.tv.data.repo.LibraryRepository
import app.blanktv.tv.data.repo.SettingsRepository
import app.blanktv.tv.player.PlaybackController
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * Hand-rolled dependency graph.
 *
 * A DI framework was a deliberate omission for the first release: the graph is five objects deep,
 * every edge is created lazily exactly once, and reading the wiring top-to-bottom is the fastest
 * way to understand the app. Revisit if constructor injection starts needing scopes or qualifiers.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val database: BlankTvDatabase by lazy { BlankTvDatabase.create(appContext) }

    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }

    val library: LibraryRepository by lazy {
        LibraryRepository(
            playlists = database.playlistDao(),
            channels = database.channelDao(),
            favourites = database.favouriteDao(),
            fetcher = PlaylistFetcher(http),
        )
    }

    val playback: PlaybackController by lazy { PlaybackController(appContext, http) }
}
