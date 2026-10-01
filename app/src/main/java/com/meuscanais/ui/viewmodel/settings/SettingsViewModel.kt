package com.meuscanais.ui.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meuscanais.data.repository.SettingsRepository
import com.meuscanais.data.model.XtreamCredentials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val firebaseRepository: com.meuscanais.data.repository.FirebaseRepository
) : ViewModel() {

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.AppSettings(null, lastSyncTimestamp = 0L, syncIntervalHours = 24)
    )

    fun updateSyncInterval(hours: Int) {
        viewModelScope.launch { settingsRepository.updateSyncInterval(hours) }
    }

    fun updatePin(pin: String?) {
        viewModelScope.launch { settingsRepository.updatePin(pin) }
    }

    fun updateAutoPlay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoPlay(enabled) }
    }

    fun updateShowDiagnostics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowDiagnostics(enabled) }
    }

    fun updateDefaultResizeMode(mode: Int) {
        viewModelScope.launch { settingsRepository.updateDefaultResizeMode(mode) }
    }

    fun updateHideBlockedCategories(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateHideBlockedCategories(enabled) }
    }

    fun updateAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAppLockEnabled(enabled) }
    }

    fun addAccount(credentials: XtreamCredentials) {
        viewModelScope.launch { settingsRepository.addAccount(credentials) }
    }

    fun removeAccount(index: Int) {
        viewModelScope.launch { settingsRepository.removeAccount(index) }
    }

    fun switchAccount(index: Int) {
        viewModelScope.launch { settingsRepository.switchAccount(index) }
    }

    fun updatePlayerEngine(engine: String) {
        viewModelScope.launch { settingsRepository.updatePlayerEngine(engine) }
    }

    fun updateBufferStrategy(strategy: String) {
        viewModelScope.launch { settingsRepository.updateBufferStrategy(strategy) }
    }

    fun updateUseOledTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateUseOledTheme(enabled) }
    }

    fun updateDataSaverMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDataSaverMode(enabled) }
    }

    fun updateUiZoom(zoom: Float) {
        viewModelScope.launch { settingsRepository.updateUiZoom(zoom) }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch { settingsRepository.updateLanguage(language) }
    }

    fun updateDownloadWifiOnly(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDownloadWifiOnly(enabled) }
    }

    fun updateSmartDownloads(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSmartDownloads(enabled) }
    }

    fun updateDetailedNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDetailedNotifications(enabled) }
    }

    fun updateProfilePicture(url: String) {
        viewModelScope.launch {
            firebaseRepository.updateProfilePicture(url)
        }
    }
}
