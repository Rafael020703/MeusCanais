package com.meuscanais.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val streamId: Int,
    val streamType: String, // LIVE, VOD, SERIES
    val name: String = "",
    val logo: String? = null,
    val rating: String? = null,
    val releaseDate: String? = null,
    val url: String? = null, // epgChannelId for LIVE
    val containerExtension: String? = null,
    val categoryId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
