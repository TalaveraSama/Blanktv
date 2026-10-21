package app.blanktv.tv.data.repo

import android.content.Context
import android.net.Uri
import app.blanktv.tv.data.db.ChannelDao
import app.blanktv.tv.data.db.ChannelEntity
import app.blanktv.tv.data.db.FavouriteDao
import app.blanktv.tv.data.db.FavouriteRow
import app.blanktv.tv.data.db.PlaylistDao
import app.blanktv.tv.data.db.PlaylistEntity
import app.blanktv.tv.data.net.PlaylistFetcher
import app.blanktv.tv.data.parser.M3uParser
import app.blanktv.tv.domain.MediaKind
import java.io.BufferedReader
import kotlinx.coroutines.flow.Flow

/**
 * The single write/read path for the media library.
 *
 * Reads are cold [Flow]s straight off Room so every screen reflects an import the moment it
 * commits; writes are suspend functions returning [Result] so callers can surface provider
 * failures ("HTTP 401", "unreachable host") without exceptions escaping into a ViewModel.
 */
class LibraryRepository(
    private val playlists: PlaylistDao,
    private val channels: ChannelDao,
    private val favourites: FavouriteDao,
    private val fetcher: PlaylistFetcher,
) {

    fun playlists(): Flow<List<PlaylistEntity>> = playlists.observeAll()

    fun channels(kind: MediaKind): Flow<List<ChannelEntity>> =
        channels.observeAllByKind(kind.code)

    fun groups(kind: MediaKind): Flow<List<String>> = channels.observeGroups(kind.code)

    fun byGroup(kind: MediaKind, group: String): Flow<List<ChannelEntity>> =
        channels.observeByGroup(kind.code, group)

    fun favourites(): Flow<List<ChannelEntity>> = channels.observeFavourites()

    fun favouriteIds(): Flow<List<Long>> = favourites.observeIds()

    fun search(query: String): Flow<List<ChannelEntity>> = channels.search(query)

    suspend fun findChannel(id: Long): ChannelEntity? = channels.find(id)

    suspend fun addRemote(name: String, url: String): Result<Int> = runCatching {
        val id = playlists.insert(
            PlaylistEntity(
                name = name.ifBlank { "Playlist" },
                url = url,
                createdAt = System.currentTimeMillis(),
            ),
        )
        fetcher.withRemoteReader(url) { reader -> import(id, reader) }
    }

    suspend fun addLocal(context: Context, name: String, uri: Uri): Result<Int> = runCatching {
        val id = playlists.insert(
            PlaylistEntity(
                name = name.ifBlank { uri.lastPathSegment ?: "Local playlist" },
                url = null,
                createdAt = System.currentTimeMillis(),
            ),
        )
        fetcher.withLocalReader(context, uri) { reader -> import(id, reader) }
    }

    suspend fun resync(id: Long): Result<Int> = runCatching {
        val playlist = playlists.find(id) ?: error("Playlist $id no longer exists")
        val url = playlist.url ?: error("Local playlists cannot be re-synced")
        fetcher.withRemoteReader(url) { reader -> import(id, reader) }
    }

    suspend fun remove(id: Long) = playlists.delete(id)

    suspend fun setFavourite(channelId: Long, favourite: Boolean) {
        if (favourite) favourites.add(FavouriteRow(channelId)) else favourites.remove(channelId)
    }

    /**
     * Replace a playlist's entries wholesale. The delete + chunked insert run outside a single
     * transaction on purpose: a 50k-entry import inside one transaction holds the write lock for
     * minutes and blocks every reader (the UI) behind it. Chunking keeps the UI responsive at the
     * cost of a brief window where the old rows are gone before the new ones land, which the
     * loading states already cover.
     */
    private suspend fun import(playlistId: Long, reader: BufferedReader): Int {
        val entries = M3uParser.parse(reader)
        channels.deleteForPlaylist(playlistId)
        entries.chunked(INSERT_CHUNK).forEach { chunk ->
            channels.insertAll(
                chunk.map { e ->
                    ChannelEntity(
                        playlistId = playlistId,
                        title = e.title,
                        url = e.url,
                        logoUrl = e.logoUrl,
                        tvgId = e.tvgId,
                        groupName = e.groupName,
                        kind = e.kind.code,
                        channelNumber = e.channelNumber,
                    )
                },
            )
        }
        val count = channels.countForPlaylist(playlistId)
        playlists.markSynced(playlistId, System.currentTimeMillis(), count)
        return count
    }

    private companion object {
        /** Keeps each INSERT below SQLite's 999-host-parameter ceiling with room to spare. */
        const val INSERT_CHUNK = 400
    }
}
