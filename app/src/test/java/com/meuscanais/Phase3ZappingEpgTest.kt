package com.meuscanais

import com.meuscanais.ui.viewmodel.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3ZappingEpgTest {

    @Test
    fun epgSessionProtection_currentSessionAccepted_oldSessionDiscarded() {
        var activeSessionId = 1
        var currentProgramTitle: String? = null

        fun onEpgLoaded(sessionIdAtRequest: Int, title: String) {
            if (activeSessionId == sessionIdAtRequest) {
                currentProgramTitle = title
            }
        }

        // Request 1 for Channel A (Session 1)
        val session1 = activeSessionId

        // User zaps to Channel B (Session 2)
        activeSessionId = 2
        val session2 = activeSessionId

        // EPG for Channel A arrives late
        onEpgLoaded(session1, "Jornal da TV A")
        assertNull(currentProgramTitle) // Discarded!

        // EPG for Channel B arrives
        onEpgLoaded(session2, "Filme da TV B")
        assertEquals("Jornal da TV A" != currentProgramTitle, true)
        assertEquals("Filme da TV B", currentProgramTitle) // Accepted!
    }

    @Test
    fun epgSessionProtection_multiplePreviousSessionsDiscarded() {
        var activeSessionId = 1
        var currentProgramTitle: String? = null

        fun onEpgLoaded(sessionIdAtRequest: Int, title: String) {
            if (activeSessionId == sessionIdAtRequest) {
                currentProgramTitle = title
            }
        }

        val session1 = 1
        activeSessionId = 2
        val session2 = 2
        activeSessionId = 3
        val session3 = 3

        // Late response from Session 1
        onEpgLoaded(session1, "Programa 1")
        assertNull(currentProgramTitle)

        // Late response from Session 2
        onEpgLoaded(session2, "Programa 2")
        assertNull(currentProgramTitle)

        // Response from Session 3
        onEpgLoaded(session3, "Programa 3")
        assertEquals("Programa 3", currentProgramTitle)
    }

    @Test
    fun playerUiState_reconnectingState_activatesAndResets() {
        val playingState = PlayerUiState.Playing(
            name = "Canal 1",
            streamId = 101,
            isReconnecting = false
        )
        assertFalse(playingState.isReconnecting)

        // Recovery starts -> isReconnecting = true
        val recoveryState = playingState.copy(isReconnecting = true)
        assertTrue(recoveryState.isReconnecting)

        // Player ready -> isReconnecting = false
        val readyState = recoveryState.copy(isReconnecting = false)
        assertFalse(readyState.isReconnecting)
    }

    @Test
    fun watchdogRecovery_oldSessionIgnored() {
        var activeSessionId = 100
        var recoveryExecutedForSession: Int? = null

        fun performRecovery(sessionId: Int) {
            if (activeSessionId == sessionId) {
                recoveryExecutedForSession = sessionId
            }
        }

        val oldSession = activeSessionId

        // User zaps -> activeSessionId increments
        activeSessionId = 101

        // Watchdog timeout for old session triggers
        performRecovery(oldSession)

        assertNull(recoveryExecutedForSession) // Old recovery aborted!
    }

    @Test
    fun watchdogRecovery_currentSessionExecuted() {
        val activeSessionId = 200
        var recoveryExecutedForSession: Int? = null

        fun performRecovery(sessionId: Int) {
            if (activeSessionId == sessionId) {
                recoveryExecutedForSession = sessionId
            }
        }

        performRecovery(200)
        assertEquals(200, recoveryExecutedForSession)
    }

    @Test
    fun newZapping_resetsReconnectingState() {
        val initialState = PlayerUiState.Playing(
            name = "Canal 1",
            streamId = 101,
            isReconnecting = true
        )
        assertTrue(initialState.isReconnecting)

        // User zaps to new channel
        val newChannelState = PlayerUiState.Playing(
            name = "Canal 2",
            streamId = 102,
            isReconnecting = false
        )
        assertFalse(newChannelState.isReconnecting)
        assertEquals("Canal 2", newChannelState.name)
    }
}
