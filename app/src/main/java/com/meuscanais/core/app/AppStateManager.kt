package com.meuscanais.core.app

import com.meuscanais.core.domain.interactor.AuthManager
import com.meuscanais.core.domain.interactor.SecurityManager
import com.meuscanais.core.domain.interactor.SyncManager
import com.meuscanais.core.domain.state.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateManager @Inject constructor(
    private val authManager: AuthManager,
    private val syncManager: SyncManager,
    private val securityManager: SecurityManager
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val appState: StateFlow<AppState> = combine(
        authManager.isLoggedIn,
        syncManager.isContentReady,
        securityManager.isAppLocked
    ) { isLoggedIn, isContentReady, isAppLocked ->
        val state = when {
            isLoggedIn == null -> AppState.Initializing
            isLoggedIn == false -> AppState.LoginRequired
            isAppLocked -> AppState.Locked
            !isContentReady -> AppState.SyncRequired
            else -> AppState.Ready
        }
        Timber.i("AppStateManager: Calculated State -> $state (isLoggedIn=$isLoggedIn, isContentReady=$isContentReady, isAppLocked=$isAppLocked)")
        state
    }.stateIn(appScope, SharingStarted.Eagerly, AppState.Initializing)
}
