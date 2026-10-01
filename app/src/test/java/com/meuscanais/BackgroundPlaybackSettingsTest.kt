package com.meuscanais

import com.meuscanais.data.repository.SettingsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundPlaybackSettingsTest {

    @Test
    fun appSettings_backgroundPlaybackDefault_isFalse() {
        val settings = SettingsRepository.AppSettings(
            credentials = null,
            lastSyncTimestamp = 0L,
            syncIntervalHours = 24
        )
        assertFalse(settings.backgroundPlaybackEnabled)
    }

    @Test
    fun appSettings_backgroundPlaybackCanBeEnabled() {
        val settings = SettingsRepository.AppSettings(
            credentials = null,
            lastSyncTimestamp = 0L,
            syncIntervalHours = 24,
            backgroundPlaybackEnabled = true
        )
        assertTrue(settings.backgroundPlaybackEnabled)
    }
}
