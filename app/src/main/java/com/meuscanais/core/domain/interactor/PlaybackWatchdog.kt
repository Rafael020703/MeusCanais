package com.meuscanais.core.domain.interactor

import androidx.media3.common.Player
import com.meuscanais.domain.model.ContentType
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlaybackWatchdog responsible for detecting hangs during playback buffering.
 * It monitors the player position and triggers a recovery callback if no progress is made within a timeout.
 */
@Singleton
class PlaybackWatchdog @Inject constructor() {
    private var watchdogJob: Job? = null

    /**
     * Starts monitoring the playback for hangs.
     *
     * @param player The Media3 Player instance to monitor.
     * @param contentType The type of content (Live vs VOD) to determine the timeout.
     * @param sessionId Current session identifier for logging.
     * @param scope The CoroutineScope in which to run the monitoring loop.
     * @param onHangDetected Callback triggered when a hang is detected.
     */
    fun startMonitoring(
        player: Player,
        contentType: ContentType,
        sessionId: Int,
        scope: CoroutineScope,
        onHangDetected: () -> Unit
    ) {
        // Cancel any existing watchdog for safety
        stop()

        val timeout = if (contentType == ContentType.LIVE) 12000L else 25000L

        watchdogJob = scope.launch {
            try {
                delay(3000) // Initial stabilization period
                
                var lastRecordedPosition = player.currentPosition
                var lastProgressCheckTime = System.currentTimeMillis()

                while (isActive) {
                    delay(timeout / 2)
                    
                    // Stop monitoring if player is no longer buffering or not supposed to play
                    if (player.playbackState != Player.STATE_BUFFERING) {
                        Timber.v("Watchdog [$sessionId]: Stopping monitoring, player state is ${player.playbackState}")
                        break
                    }
                    if (!player.playWhenReady) {
                        Timber.v("Watchdog [$sessionId]: Stopping monitoring, playWhenReady is false")
                        break
                    }

                    val now = System.currentTimeMillis()
                    val currentPos = player.currentPosition

                    // If position hasn't moved and it's been too long
                    if (currentPos == lastRecordedPosition) {
                        if (now - lastProgressCheckTime >= timeout) {
                            Timber.w("Watchdog [$sessionId]: Hang detected at position $currentPos")
                            onHangDetected()
                            break
                        }
                    } else {
                        // There is some progress, reset timer and position
                        lastRecordedPosition = currentPos
                        lastProgressCheckTime = now
                    }
                }
            } catch (e: CancellationException) {
                // Monitoring cancelled, expected behavior during zapping or stop
            }
        }
    }

    /**
     * Stops the current watchdog monitoring job.
     */
    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
    }
}
