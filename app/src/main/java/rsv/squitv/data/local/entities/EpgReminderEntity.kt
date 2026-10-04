package rsv.squitv.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "epg_reminders")
data class EpgReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val streamId: Int,
    val programTitle: String,
    val startTime: Long,
    val channelName: String,
    val channelIcon: String?
)
