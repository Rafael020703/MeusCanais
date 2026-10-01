package com.meuscanais.domain.usecase

import com.meuscanais.core.data.repository.UserRepository
import com.meuscanais.data.repository.FirebaseRepository
import com.meuscanais.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import timber.log.Timber
import kotlin.math.abs

/**
 * UseCase for syncing watch progress both locally (Room) and remotely (Firebase).
 * Encapsulates throttling logic to avoid excessive network calls.
 */
class SyncWatchProgressUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseRepository: FirebaseRepository,
    private val settingsRepository: SettingsRepository
) {
    private var lastFirebaseSyncTime = 0L
    private var lastFirebaseSyncPosition = -1L
    private val MIN_SYNC_INTERVAL_MS = 60_000L // 1 minute threshold

    /**
     * Updates watch progress.
     * 
     * @param streamId The ID of the stream.
     * @param type The type of content (VOD, SERIES).
     * @param position Current playback position in ms.
     * @param duration Total duration in ms.
     * @param seriesId Optional series ID if it's an episode.
     * @param forceSync If true, bypasses throttling for Firebase sync.
     */
    suspend operator fun invoke(
        streamId: Int,
        type: String,
        position: Long,
        duration: Long,
        seriesId: Int?,
        forceSync: Boolean = false
    ) {
        val timestamp = System.currentTimeMillis()
        
        // Always save locally
        try {
            userRepository.saveWatchProgress(streamId, type, position, duration, seriesId, timestamp)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save local watch progress")
        }

        // Remote sync with throttling logic (Time threshold OR 5% progress jump)
        val shouldSyncFirebase = forceSync || 
                                 (timestamp - lastFirebaseSyncTime > MIN_SYNC_INTERVAL_MS) ||
                                 (abs(position - lastFirebaseSyncPosition) > duration / 20)

        if (shouldSyncFirebase && duration > 0) {
            lastFirebaseSyncTime = timestamp
            lastFirebaseSyncPosition = position
            
            try {
                val settings = settingsRepository.settingsFlow.first()
                val profileId = settings.activeProfileId
                val data = mapOf(
                    "position" to position,
                    "duration" to duration,
                    "type" to type,
                    "seriesId" to (seriesId ?: 0),
                    "timestamp" to timestamp
                )
                firebaseRepository.syncWatchProgress(profileId, streamId.toString(), data)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync watch progress to Firebase")
            }
        }
    }
}
