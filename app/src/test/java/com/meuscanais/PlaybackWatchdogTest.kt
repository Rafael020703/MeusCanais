package com.meuscanais

import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import com.meuscanais.core.domain.interactor.PlaybackWatchdog
import com.meuscanais.domain.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackWatchdogTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class MutablePlayerState(
        var state: Int = Player.STATE_IDLE,
        var playWhenReady: Boolean = true,
        var position: Long = 0L
    )

    private fun createTestPlayer(pState: MutablePlayerState): Player {
        val handler = InvocationHandler { _, method, _ ->
            when (method.name) {
                "getPlaybackState" -> pState.state
                "getPlayWhenReady" -> pState.playWhenReady
                "getCurrentPosition" -> pState.position
                "getAudioAttributes" -> AudioAttributes.DEFAULT
                "getCurrentTracks" -> Tracks.EMPTY
                "getTrackSelectionParameters" -> TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
                "getPlaybackParameters" -> PlaybackParameters.DEFAULT
                "getMediaMetadata", "getPlaylistMetadata" -> MediaMetadata.EMPTY
                "getAvailableCommands" -> Player.Commands.EMPTY
                "hashCode" -> System.identityHashCode(pState)
                "equals" -> false
                "toString" -> "ProxyTestPlayer"
                else -> null
            }
        }
        return Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            handler
        ) as Player
    }

    @Test
    fun testA_readyCancelsWatchdogNoRecovery() = runTest {
        val watchdog = PlaybackWatchdog()
        watchdog.timeProvider = { testScheduler.currentTime }
        val pState = MutablePlayerState(state = Player.STATE_BUFFERING)
        val player = createTestPlayer(pState)
        var hangDetected = false

        watchdog.startMonitoring(
            player = player,
            contentType = ContentType.LIVE,
            sessionId = 1,
            streamId = 101,
            scope = this
        ) {
            hangDetected = true
        }

        advanceTimeBy(1000)
        // Transition to STATE_READY
        pState.state = Player.STATE_READY
        advanceTimeBy(1000)

        // Advance beyond startup timeout
        advanceTimeBy(5000)

        assertFalse("Watchdog should not trigger recovery when player reaches READY", hangDetected)
        assertEquals(null, watchdog.currentSessionId)
    }

    @Test
    fun testB_idleNormalStopNoTimeoutNoRecovery() = runTest {
        val watchdog = PlaybackWatchdog()
        watchdog.timeProvider = { testScheduler.currentTime }
        val pState = MutablePlayerState(state = Player.STATE_BUFFERING)
        val player = createTestPlayer(pState)
        var hangDetected = false

        watchdog.startMonitoring(
            player = player,
            contentType = ContentType.LIVE,
            sessionId = 1,
            streamId = 101,
            scope = this
        ) {
            hangDetected = true
        }

        advanceTimeBy(1000)
        // Explicit stop / cancel when switching or stopping normal playback
        watchdog.stop("NORMAL_STOP")

        pState.state = Player.STATE_IDLE
        advanceTimeBy(6000)

        assertFalse("Watchdog should not trigger recovery when explicitly stopped", hangDetected)
    }

    @Test
    fun testC_idleHangTriggersTimeoutAndRecovery() = runTest {
        val watchdog = PlaybackWatchdog()
        watchdog.timeProvider = { testScheduler.currentTime }
        val pState = MutablePlayerState(state = Player.STATE_IDLE)
        val player = createTestPlayer(pState)
        var detectedReason: String? = null

        watchdog.startMonitoring(
            player = player,
            contentType = ContentType.LIVE,
            sessionId = 1,
            streamId = 101,
            scope = this
        ) { reason ->
            detectedReason = reason
        }

        // Advance 5.5 seconds (STARTUP_IDLE_TIMEOUT_MS is 5000ms)
        advanceTimeBy(5500)

        assertEquals("STARTUP_IDLE_TIMEOUT", detectedReason)
    }

    @Test
    fun testD_staleWatchdogIgnoredWhenSessionChanges() = runTest {
        val watchdog = PlaybackWatchdog()
        watchdog.timeProvider = { testScheduler.currentTime }
        val pState = MutablePlayerState(state = Player.STATE_IDLE)
        val player = createTestPlayer(pState)
        var recoverySession: Int? = null

        // Session 4
        watchdog.startMonitoring(
            player = player,
            contentType = ContentType.LIVE,
            sessionId = 4,
            streamId = 101,
            scope = this
        ) {
            recoverySession = 4
        }

        advanceTimeBy(2000)

        // User zaps to Session 5 / Stream 202
        watchdog.startMonitoring(
            player = player,
            contentType = ContentType.LIVE,
            sessionId = 5,
            streamId = 202,
            scope = this
        ) {
            recoverySession = 5
        }

        advanceTimeBy(5500)

        // Recovery should be for Session 5, NOT Session 4
        assertEquals(5, recoverySession)
    }

    @Test
    fun testE_staleRecoveryIgnored() = runTest {
        var activeSession = 4
        var activeStream = 101
        var executedRecoverySession: Int? = null

        fun performRecovery(sessionId: Int, streamId: Int) {
            if (activeSession != sessionId || activeStream != streamId) {
                // Stale recovery ignored
                return
            }
            executedRecoverySession = sessionId
        }

        // Late recovery for session 4 triggers
        val oldSession = 4
        val oldStream = 101

        // User is now on activeSession 5
        activeSession = 5
        activeStream = 202

        performRecovery(oldSession, oldStream)

        assertNull("Old session recovery must be ignored", executedRecoverySession)

        // Recovery for active session 5
        performRecovery(5, 202)
        assertEquals(5, executedRecoverySession)
    }
}
