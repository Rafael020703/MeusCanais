package com.meuscanais.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "pending_syncs",
    primaryKeys = ["type", "providerHash", "contentId"]
)
data class PendingSyncEntity(
    val type: String, // FAVORITE_ADD, FAVORITE_REMOVE, WATCH_PROGRESS, HISTORY_ADD
    val providerHash: String,
    val contentId: String,
    val payload: String, // JSON representation of the item/progress
    val timestamp: Long = System.currentTimeMillis()
)
