package app.blanktv.tv.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A playlist the user added: a remote M3U/M3U8 URL, or a one-off local import (url = null).
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long? = null,
    @ColumnInfo(name = "channel_count") val channelCount: Int = 0,
)

/**
 * One playable entry. [kind] is the [app.blanktv.tv.domain.MediaKind] code inferred at import time.
 */
@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlist_id"), Index("kind"), Index("group_name")],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    val title: String,
    val url: String,
    @ColumnInfo(name = "logo_url") val logoUrl: String? = null,
    @ColumnInfo(name = "tvg_id") val tvgId: String? = null,
    @ColumnInfo(name = "group_name") val groupName: String? = null,
    val kind: Int = 0,
    @ColumnInfo(name = "channel_number") val channelNumber: Int? = null,
)

/**
 * Join-table row marking a channel as a favourite. Kept as its own table rather than a boolean on
 * [ChannelEntity] so a re-sync of the playlist cannot silently drop the user's favourites.
 */
@Entity(tableName = "favourites", primaryKeys = ["channel_id"])
data class FavouriteRow(
    @ColumnInfo(name = "channel_id") val channelId: Long,
)
