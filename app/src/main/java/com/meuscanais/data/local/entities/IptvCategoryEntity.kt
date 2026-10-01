package com.meuscanais.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "iptv_categories",
    primaryKeys = ["id", "type"]
)
data class IptvCategoryEntity(
    val id: String,
    val name: String,
    val type: String, // LIVE, VOD, SERIES, GENRE
    val isLocked: Boolean = false,
    val isPinned: Boolean = false
)
