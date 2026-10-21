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
import app.blanktv.tv.data.parser.ParsedEntry
import app.blanktv.tv.data.stalker.StalkerClient
import app.blanktv.tv.data.xtream.XtreamClient
import app.blanktv.tv.domain.MediaKind
import app.blanktv.tv.domain.SourceKind
import kotlinx.coroutines.flow.Flow

/**
 * The single write/read path for the media library.
 *
 * Reads are cold [Flow]s straight off Room so every screen reflects an import the moment it
 * commits. Writes are suspend functions returning [Result] so a provider failure ("HTTP 401",
 * "handshake returned no token") surfaces as a message instead of an exception escaping into a
 * ViewModel.
 *
 * The three protocols all collapse into the same [ParsedEntry] list before [commit], which is what
 * keeps the database, the UI and the favourites table protocol-agnostic.
 */
class LibraryRepository(
    private val playlists: PlaylistDao,
    private val channels: ChannelDao,
    private val favourites: FavouriteDao,
    private val fetcher: PlaylistFetcher,
    private val xtream: XtreamClient,
    private val stalker: StalkerClient,
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

    // --- Writes ---------------------------------------------------------------------

    suspend fun addM3u(name: String, url: String): Result<Int> = runCatching {
        val id = playlists.insert(
            PlaylistEntity(
                name = name.ifBlank { "M3U playlist" },
                sourceKind = SourceKind.M3U.code,
                url = url,
                createdAt = System.currentTimeMillis(),
            ),
        )
        fetcher.withRemoteReader(url) { reader -> commit(id, M3uParser.parse(reader)) }
    }

    suspend fun addLocal(context: Context, name: String, uri: Uri): Result<Int> = runCatching {
        val id = playlists.insert(
            PlaylistEntity(
                name = name.ifBlank { uri.lastPathSegment ?: "Local playlist" },
                sourceKind = SourceKind.M3U.code,
                url = null,
                createdAt = System.currentTimeMillis(),
            ),
        )
        fetcher.withLocalReader(context, uri) { reader -> commit(id, M3uParser.parse(reader)) }
    }

    suspend fun addXtream(name: String, baseUrl: String, username: String, password: String): Result<Int> =
        runCatching {
            val id = playlists.insert(
                PlaylistEntity(
                    name = name.ifBlank { "Xtream panel" },
                    sourceKind = SourceKind.Xtream.code,
                    url = null,
                    baseUrl = baseUrl.trim(),
                    username = username,
                    password = password,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            val entries = xtream.fetchAll(baseUrl, username, password)
            commit(id, entries)
        }

    suspend fun addStalker(name: String, portalUrl: String, macAddress: String): Result<Int> =
        runCatching {
            val id = playlists.insert(
                PlaylistEntity(
                    name = name.ifBlank { "Stalker portal" },
                    sourceKind = SourceKind.Stalker.code,
                    url = null,
                    baseUrl = portalUrl.trim(),
                    macAddress = macAddress.trim(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
            val entries = stalker.fetchAll(portalUrl, macAddress)
            commit(id, entries)
        }

    /** Re-runs whichever protocol the row was created with. */
    suspend fun resync(id: Long): Result<Int> = runCatching {
        val playlist = playlists.find(id) ?: error("Source $id no longer exists")
        val entries = when (SourceKind.fromCode(playlist.sourceKind)) {
            SourceKind.M3U -> {
                val url = playlist.url ?: error("Local playlists cannot be re-synced")
                fetcher.withRemoteReader(url) { reader -> M3uParser.parse(reader) }
            }
            SourceKind.Xtream -> xtream.fetchAll(
                playlist.baseUrl ?: error("Xtream source has no base URL"),
                playlist.username.orEmpty(),
                playlist.password.orEmpty(),
            )
            SourceKind.Stalker -> stalker.fetchAll(
                playlist.baseUrl ?: error("Stalker source has no portal URL"),
                playlist.macAddress.orEmpty(),
            )
        }
        commit(id, entries)
    }

    suspend fun remove(id: Long) = playlists.delete(id)

    suspend fun setFavourite(channelId: Long, favourite: Boolean) {
        if (favourite) favourites.add(FavouriteRow(channelId)) else favourites.remove(channelId)
    }

    /**
     * Replace a source's entries wholesale. The delete + chunked insert run outside a single
     * transaction on purpose: a 50k-entry import inside one transaction holds the write lock for
     * minutes and blocks every reader (the UI) behind it. Chunking keeps the UI responsive at the
     * cost of a brief window where the old rows are gone before the new ones land, which the
     * loading states already cover.
     */
    private suspend fun commit(playlistId: Long, entries: List<ParsedEntry>): Int {
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
