package com.meuscanais.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "iptv_streams_fts")
@Fts4(contentEntity = IptvStreamEntity::class)
data class IptvStreamFtsEntity(
    val name: String,
    val categoryId: String
)
