package app.blanktv.tv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, ChannelEntity::class, FavouriteRow::class],
    version = 2,
    exportSchema = false,
)
abstract class BlankTvDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun favouriteDao(): FavouriteDao

    companion object {
        private const val NAME = "blanktv.db"

        fun create(context: Context): BlankTvDatabase =
            Room.databaseBuilder(context.applicationContext, BlankTvDatabase::class.java, NAME)
                // Schema is not published yet; a dev bump should not brick an install. Replace with
                // real migrations before the first public release.
                .fallbackToDestructiveMigration()
                .build()
    }
}
