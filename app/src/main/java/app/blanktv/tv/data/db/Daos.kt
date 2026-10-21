package app.blanktv.tv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY created_at ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun find(id: Long): PlaylistEntity?

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE playlists SET last_synced_at = :at, channel_count = :count WHERE id = :id")
    suspend fun markSynced(id: Long, at: Long, count: Int)
}

@Dao
interface ChannelDao {

    @Query(
        "SELECT * FROM channels WHERE kind = :kind " +
            "ORDER BY channel_number ASC, title COLLATE NOCASE ASC",
    )
    fun observeAllByKind(kind: Int): Flow<List<ChannelEntity>>

    @Query(
        "SELECT * FROM channels WHERE kind = :kind AND group_name = :group " +
            "ORDER BY title COLLATE NOCASE ASC",
    )
    fun observeByGroup(kind: Int, group: String): Flow<List<ChannelEntity>>

    @Query(
        "SELECT DISTINCT group_name FROM channels " +
            "WHERE kind = :kind AND group_name IS NOT NULL AND group_name != '' " +
            "ORDER BY group_name COLLATE NOCASE ASC",
    )
    fun observeGroups(kind: Int): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE id IN (SELECT channel_id FROM favourites)")
    fun observeFavourites(): Flow<List<ChannelEntity>>

    @Query(
        "SELECT * FROM channels WHERE title LIKE '%' || :query || '%' " +
            "ORDER BY title COLLATE NOCASE ASC LIMIT 80",
    )
    fun search(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun find(id: Long): ChannelEntity?

    @Insert
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlist_id = :playlistId")
    suspend fun deleteForPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) FROM channels WHERE playlist_id = :playlistId")
    suspend fun countForPlaylist(playlistId: Long): Int
}

@Dao
interface FavouriteDao {

    @Query("SELECT channel_id FROM favourites")
    fun observeIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(row: FavouriteRow)

    @Query("DELETE FROM favourites WHERE channel_id = :channelId")
    suspend fun remove(channelId: Long)
}
