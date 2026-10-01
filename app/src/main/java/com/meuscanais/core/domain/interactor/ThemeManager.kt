package com.meuscanais.core.domain.interactor

import com.meuscanais.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    val useOledTheme: Flow<Boolean> = settingsRepository.settingsFlow.map { it.useOledTheme }
    val uiZoom: Flow<Float> = settingsRepository.settingsFlow.map { it.uiZoom }
    val language: Flow<String> = settingsRepository.settingsFlow.map { it.language }
}
