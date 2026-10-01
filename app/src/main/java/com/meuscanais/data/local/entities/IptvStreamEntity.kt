package com.meuscanais.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import com.meuscanais.domain.model.ContentType
import com.meuscanais.domain.model.IptvItem

@Entity(
    tableName = "iptv_streams",
    primaryKeys = ["id", "streamType"],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["streamType"]),
        Index(value = ["name"])
    ]
)
data class IptvStreamEntity(
    val id: Int,
    val name: String,
    val categoryId: String,
    val streamType: String, // LIVE, VOD, SERIES
    val url: String?,
    val logo: String?,
    val containerExtension: String? = "ts",
    val isFavorite: Boolean = false,
    val cast: String? = null,
    val director: String? = null,
    val rating: String? = null,
    val added: String? = null,
    val releaseDate: String? = null,
    val genre: String? = null,
    val plot: String? = null,
    val duration: String? = null,
    val backdrop: String? = null,
    val youtubeTrailer: String? = null
)
