package rsv.squitv.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "episodes",
    primaryKeys = ["seriesId", "episodeId"],
    foreignKeys = [
        ForeignKey(
            entity = IptvStreamEntity::class,
            parentColumns = ["id", "streamType"],
            childColumns = ["seriesId", "streamType"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("seriesId", "streamType"),
        Index("seriesId", "seasonNumber"),
        Index("title")
    ]
)
data class EpisodeEntity(
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeId: Int,
    val episodeNum: Int,
    val title: String,
    val streamId: Int,
    val containerExtension: String?,
    val image: String? = null,
    val plot: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val streamType: String = "SERIES"
)
