package com.meuscanais.core.domain.state

sealed class AppSyncStatus {
    data object Pending : AppSyncStatus()
    data object Fetching : AppSyncStatus()
    data object Syncing : AppSyncStatus()
    data object Success : AppSyncStatus()
    data object OfflineMode : AppSyncStatus()
    data class Done(val count: Int) : AppSyncStatus()
    data class Error(val message: String) : AppSyncStatus()
}

data class AppSyncProgress(
    val accountStatus: AppSyncStatus = AppSyncStatus.Pending,
    val liveStatus: AppSyncStatus = AppSyncStatus.Pending,
    val vodStatus: AppSyncStatus = AppSyncStatus.Pending,
    val seriesStatus: AppSyncStatus = AppSyncStatus.Pending,
    val isComplete: Boolean = false,
    val error: String? = null
)
