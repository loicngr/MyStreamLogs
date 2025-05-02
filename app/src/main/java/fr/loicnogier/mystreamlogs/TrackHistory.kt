package fr.loicnogier.mystreamlogs

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_history")
data class TrackHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "track_title")
    val trackTitle: String,

    @ColumnInfo(name = "artist_name")
    val artistName: String,

    @ColumnInfo(name = "album_name")
    val albumName: String?,

    @ColumnInfo(name = "album_art_url")
    val albumArtUrl: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)