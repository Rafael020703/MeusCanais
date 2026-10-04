package rsv.squitv.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val streamId: Int,
    val type: String, // VOD, SERIES
    val position: Long,
    val duration: Long,
    val lastWatched: Long = System.currentTimeMillis(),
    val seriesId: Int? = null
)
