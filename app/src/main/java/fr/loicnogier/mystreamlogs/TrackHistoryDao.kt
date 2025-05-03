package fr.loicnogier.mystreamlogs

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TrackStreamCount(
    @ColumnInfo(name = "track_title") val trackTitle: String,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "stream_count") val streamCount: Int
    // You could add albumName here too if needed, but it requires grouping by it as well
    // @ColumnInfo(name = "album_name") val albumName: String?,
)

@Dao
interface TrackHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trackHistory: TrackHistory)

    @Query("SELECT * FROM track_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TrackHistory>>

    @Query("DELETE FROM track_history")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM track_history
        WHERE track_title LIKE '%' || :query || '%'
           OR artist_name LIKE '%' || :query || '%'
           OR album_name LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchHistory(query: String): Flow<List<TrackHistory>>

    // New query to get most streamed tracks
    // Groups tracks by title and artist, counts them, and orders by count descending
    @Query("""
        SELECT track_title, artist_name, COUNT(*) as stream_count
        FROM track_history
        GROUP BY track_title, artist_name
        ORDER BY stream_count DESC
    """)
    fun getMostStreamedTracks(): Flow<List<TrackStreamCount>> // Return Flow of the new data class
}