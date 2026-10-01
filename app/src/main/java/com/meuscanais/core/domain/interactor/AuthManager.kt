package com.meuscanais.core.domain.interactor

import com.meuscanais.data.api.XtreamService
import com.meuscanais.data.model.UserInfo
import com.meuscanais.data.model.XtreamResponse
import com.meuscanais.data.repository.FirebaseRepository
import com.meuscanais.data.repository.SettingsRepository
import com.meuscanais.data.model.XtreamCredentials
import com.meuscanais.domain.model.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val xtreamService: XtreamService,
    private val settingsRepository: SettingsRepository,
    private val firebaseRepository: FirebaseRepository,
    private val dnsManager: DnsManager
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    private var _credentials: XtreamCredentials? = null
    val credentials get() = _credentials

    private val _accountInfo = MutableStateFlow<UserInfo?>(null)
    val accountInfo: StateFlow<UserInfo?> = _accountInfo.asStateFlow()

    private val _sessionStatus = MutableStateFlow(SessionStatus.UNKNOWN)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    private val _showExpiryWarning = MutableStateFlow(false)
    val showExpiryWarning: StateFlow<Boolean> = _showExpiryWarning.asStateFlow()

    init {
        managerScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val hasCredentials = settings.credentials != null
                _isLoggedIn.value = hasCredentials
                _credentials = settings.credentials
                
                if (settings.cachedUserInfo != null) {
                    _accountInfo.value = settings.cachedUserInfo
                    checkAccountExpiry(settings.cachedUserInfo)
                }
                
                if (hasCredentials) {
                    dnsManager.updateBaseUrl(settings.credentials!!.baseUrl)
                }
            }
        }
    }

    fun logout() {
        Timber.i("AuthManager: Logout initiated. Clearing session states.")
        _isLoggedIn.value = false
        _sessionStatus.value = SessionStatus.UNKNOWN
        managerScope.launch {
            settingsRepository.clearCredentials()
            Timber.i("AuthManager: Logout completed. Persistent storage cleared.")
        }
    }

    suspend fun validateSession(credentials: XtreamCredentials): SessionStatus {
        return try {
            val response = xtreamService.login(credentials.username, credentials.password)
            val status = if (response.userInfo?.auth == 1) {
                val expDate = response.userInfo.expDate?.toLongOrNull() ?: 0L
                val now = System.currentTimeMillis() / 1000
                if (expDate in 1..now) SessionStatus.EXPIRED else SessionStatus.VALID
            } else {
                SessionStatus.INVALID
            }
            _sessionStatus.value = status
            status
        } catch (e: Exception) {
            SessionStatus.OFFLINE
        }
    }

    suspend fun login(credentials: XtreamCredentials): XtreamResponse {
        Timber.d("Tentando login para usuário: ${credentials.username} em ${credentials.baseUrl}")
        try {
            val response = xtreamService.login(credentials.username, credentials.password)
            val status = if (response.userInfo?.auth == 1) {
                val expDate = response.userInfo.expDate?.toLongOrNull() ?: 0L
                val now = System.currentTimeMillis() / 1000
                if (expDate in 1..now) SessionStatus.EXPIRED else SessionStatus.VALID
            } else {
                SessionStatus.INVALID
            }
            _sessionStatus.value = status
            
            if (response.userInfo?.auth == 0) {
                Timber.e("Falha na autenticação: Servidor recusou usuário/senha")
            } else {
                Timber.i("Login OK. Status: $status. Conexões: ${response.userInfo?.activeCons}/${response.userInfo?.maxConnections}")
            }
            return response
        } catch (e: Exception) {
            _sessionStatus.value = SessionStatus.OFFLINE
            Timber.e(e, "Erro de rede/servidor no login")
            throw e
        }
    }

    fun setSessionStatus(status: SessionStatus) {
        _sessionStatus.value = status
    }

    fun updateCredentials(newCreds: XtreamCredentials) {
        managerScope.launch {
            settingsRepository.saveCredentials(newCreds)
        }
    }
    
    fun refreshAccountInfo(creds: XtreamCredentials) {
        managerScope.launch {
            try {
                val loginResponse = xtreamService.login(creds.username, creds.password)
                _accountInfo.value = loginResponse.userInfo
                loginResponse.userInfo?.let { 
                    settingsRepository.updateAccountCache(it) 
                    checkAccountExpiry(it)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh account info")
            }
        }
    }

    private fun checkAccountExpiry(userInfo: UserInfo?) {
        val expDate = userInfo?.expDate?.toLongOrNull() ?: return
        if (expDate <= 0L) {
            _showExpiryWarning.value = false
            return
        }
        val currentTime = System.currentTimeMillis() / 1000
        val threeDaysInSeconds = 3 * 24 * 60 * 60
        _showExpiryWarning.value = (expDate - currentTime) in 0..threeDaysInSeconds
    }

    fun switchAccount(index: Int) {
        managerScope.launch { settingsRepository.switchAccount(index) }
    }

    fun removeAccount(index: Int) {
        managerScope.launch { settingsRepository.removeAccount(index) }
    }
}
