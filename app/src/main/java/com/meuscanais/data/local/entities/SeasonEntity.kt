package com.meuscanais.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "seasons",
    primaryKeys = ["seriesId", "seasonNumber"],
    foreignKeys = [
        ForeignKey(
            entity = IptvStreamEntity::class,
            parentColumns = ["id", "streamType"],
            childColumns = ["seriesId", "streamType"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("seriesId", "streamType")]
)
data class SeasonEntity(
    val seriesId: Int,
    val seasonNumber: Int,
    val name: String,
    val cover: String?,
    val streamType: String = "SERIES"
)
