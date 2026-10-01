package com.meuscanais.data.local.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "epg_programs_staging",
    primaryKeys = ["channelId", "startTime"],
    indices = [
        Index("channelId"),
        Index("startTime"),
        Index("stopTime")
    ]
)
data class EpgProgramStagingEntity(
    val channelId: String,
    val startTime: Long,
    val stopTime: Long,
    val title: String,
    val description: String?,
    val category: String? = null
)

fun EpgProgramEntity.toStaging() = EpgProgramStagingEntity(
    channelId = channelId,
    startTime = startTime,
    stopTime = stopTime,
    title = title,
    description = description,
    category = category
)
