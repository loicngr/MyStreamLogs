package fr.loicnogier.mystreamlogs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

}