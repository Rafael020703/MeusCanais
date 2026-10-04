package rsv.squitv.data.local.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "epg_programs",
    primaryKeys = ["channelId", "startTime"],
    indices = [
        Index("channelId"),
        Index("startTime"),
        Index("stopTime")
    ]
)
data class EpgProgramEntity(
    val channelId: String,
    val startTime: Long, // Epoch seconds
    val stopTime: Long,  // Epoch seconds
    val title: String,
    val description: String?,
    val category: String? = null
)
