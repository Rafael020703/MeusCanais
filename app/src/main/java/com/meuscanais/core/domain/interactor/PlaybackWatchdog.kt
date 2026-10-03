package com.meuscanais.core.domain.interactor

import androidx.media3.common.Player
import com.meuscanais.domain.model.ContentType
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlaybackWatchdog responsible for detecting hangs during playback buffering and startup IDLE.
 * It monitors the player state/position and triggers a recovery callback if no progress is made within a timeout.
 */
@Singleton
class PlaybackWatchdog @Inject constructor() {

    companion object {
        const val STARTUP_IDLE_TIMEOUT_MS = 5000L
        const val LIVE_BUFFERING_TIMEOUT_MS = 12000L
        const val VOD_BUFFERING_TIMEOUT_MS = 25000L
        const val CHECK_INTERVAL_MS = 500L
    }

    var timeProvider: () -> Long = { System.currentTimeMillis() }

    private var watchdogJob: Job? = null
    @Volatile private var activeSessionId: Int? = null
    @Volatile private var activeStreamId: Int? = null
    @Volatile private var isStartupPhase: Boolean = false

    val currentSessionId: Int? get() = activeSessionId
    val currentStreamId: Int? get() = activeStreamId

    /**
     * Starts monitoring the playback for hangs (both buffering hangs and startup IDLE hangs).
     *
     * @param player The Media3 Player instance to monitor.
     * @param contentType The type of content (Live vs VOD) to determine buffering timeout.
     * @param sessionId Current session identifier for logging.
     * @param streamId Current stream identifier for session matching.
     * @param scope The CoroutineScope in which to run the monitoring loop.
     * @param onHangDetected Callback triggered when a hang is detected, passing the hang reason.
     */
    fun startMonitoring(
        player: Player,
        contentType: ContentType,
        sessionId: Int,
        streamId: Int,
        scope: CoroutineScope,
        onHangDetected: (reason: String) -> Unit
    ) {
        // If already actively monitoring the exact same session and stream, let it continue
        if (watchdogJob?.isActive == true && activeSessionId == sessionId && activeStreamId == streamId) {
            return
        }

        // Cancel any existing watchdog for safety
        stop("NEW_MONITORING_STARTED")

        activeSessionId = sessionId
        activeStreamId = streamId
        isStartupPhase = true

        val bufferingTimeout = if (contentType == ContentType.LIVE) LIVE_BUFFERING_TIMEOUT_MS else VOD_BUFFERING_TIMEOUT_MS

        watchdogJob = scope.launch {
            try {
                var idleStartTime: Long? = timeProvider()
                var lastRecordedPosition = withContext(Dispatchers.Main) {
                    try { player.currentPosition } catch (_: Exception) { 0L }
                }
                var lastProgressCheckTime = timeProvider()

                while (isActive) {
                    delay(CHECK_INTERVAL_MS)

                    val currentSession = activeSessionId
                    val currentStream = activeStreamId

                    if (currentSession != sessionId || currentStream != streamId) {
                        Timber.w("[STALE_WATCHDOG_IGNORED] session=$sessionId activeSession=$currentSession stream=$streamId currentStream=$currentStream")
                        break
                    }

                    val state = withContext(Dispatchers.Main) {
                        try { player.playbackState } catch (_: Exception) { Player.STATE_IDLE }
                    }
                    val playWhenReady = withContext(Dispatchers.Main) {
                        try { player.playWhenReady } catch (_: Exception) { false }
                    }

                    if (state == Player.STATE_READY) {
                        isStartupPhase = false
                        Timber.d("[WATCHDOG][READY] session=$sessionId stream=$streamId")
                        activeSessionId = null
                        activeStreamId = null
                        break
                    }

                    if (state == Player.STATE_IDLE) {
                        if (isStartupPhase) {
                            val now = timeProvider()
                            if (idleStartTime == null) {
                                idleStartTime = now
                            }
                            val elapsedMs = now - idleStartTime
                            Timber.d("[WATCHDOG][IDLE_MONITORING] session=$sessionId stream=$streamId elapsedMs=$elapsedMs")

                            if (elapsedMs >= STARTUP_IDLE_TIMEOUT_MS) {
                                if (activeSessionId == sessionId && activeStreamId == streamId) {
                                    Timber.w("[WATCHDOG][IDLE_TIMEOUT] session=$sessionId stream=$streamId elapsedMs=$elapsedMs")
                                    Timber.i("[RECOVERY][START] session=$sessionId stream=$streamId reason=STARTUP_IDLE_TIMEOUT")
                                    onHangDetected("STARTUP_IDLE_TIMEOUT")
                                } else {
                                    Timber.w("[STALE_WATCHDOG_IGNORED] session=$sessionId activeSession=$activeSessionId stream=$streamId activeStream=$activeStreamId")
                                }
                                break
                            }
                        } else {
                            Timber.d("[WATCHDOG][CANCELLED] session=$sessionId stream=$streamId reason=IDLE_AFTER_STARTUP")
                            break
                        }
                    } else {
                        // Not in IDLE (e.g., STATE_BUFFERING), reset startup idle timer
                        idleStartTime = null
                    }

                    if (state == Player.STATE_BUFFERING) {
                        if (!playWhenReady && !isStartupPhase) {
                            Timber.v("[WATCHDOG][CANCELLED] session=$sessionId stream=$streamId reason=NOT_PLAYING_WHEN_READY")
                            break
                        }

                        val now = timeProvider()
                        val currentPos = withContext(Dispatchers.Main) {
                            try { player.currentPosition } catch (_: Exception) { 0L }
                        }

                        if (currentPos == lastRecordedPosition) {
                            val elapsedMs = now - lastProgressCheckTime
                            if (elapsedMs >= bufferingTimeout) {
                                if (activeSessionId == sessionId && activeStreamId == streamId) {
                                    Timber.w("[WATCHDOG][BUFFERING_TIMEOUT] session=$sessionId stream=$streamId elapsedMs=$elapsedMs")
                                    Timber.i("[RECOVERY][START] session=$sessionId stream=$streamId reason=BUFFERING_TIMEOUT")
                                    onHangDetected("BUFFERING_TIMEOUT")
                                } else {
                                    Timber.w("[STALE_WATCHDOG_IGNORED] session=$sessionId activeSession=$activeSessionId stream=$streamId activeStream=$activeStreamId")
                                }
                                break
                            }
                        } else {
                            lastRecordedPosition = currentPos
                            lastProgressCheckTime = now
                        }
                    }
                }
            } catch (e: CancellationException) {
                Timber.d("[WATCHDOG][CANCELLED] session=$sessionId stream=$streamId reason=CANCELLED_BY_COROUTINE")
            }
        }
    }

    /**
     * Stops the current watchdog monitoring job.
     *
     * @param reason Explanation for logging why the watchdog was stopped.
     */
    fun stop(reason: String = "EXPLICIT_STOP") {
        val sess = activeSessionId
        val str = activeStreamId
        if (watchdogJob?.isActive == true && sess != null && str != null) {
            Timber.d("[WATCHDOG][CANCELLED] session=$sess stream=$str reason=$reason")
        }
        watchdogJob?.cancel()
        watchdogJob = null
        activeSessionId = null
        activeStreamId = null
        isStartupPhase = false
    }
}
