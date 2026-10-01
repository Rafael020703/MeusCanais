package com.meuscanais

import com.meuscanais.data.model.XtreamStream
import org.junit.Assert.assertEquals
import org.junit.Test

class TvRemoteKeyZappingTest {

    private fun handleTvRemoteKey(
        keyCode: Int,
        isLive: Boolean,
        streams: List<XtreamStream>,
        currentStreamId: Int
    ): Pair<Boolean, Int?> {
        if (!isLive || streams.isEmpty()) return false to null

        val currentIndex = streams.indexOfFirst { it.streamId == currentStreamId }

        return when (keyCode) {
            166, 87 -> { // KEYCODE_CHANNEL_UP or KEYCODE_MEDIA_NEXT
                val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % streams.size
                true to streams[nextIndex].streamId
            }
            167, 88 -> { // KEYCODE_CHANNEL_DOWN or KEYCODE_MEDIA_PREVIOUS
                val prevIndex = if (currentIndex <= 0) streams.size - 1 else currentIndex - 1
                true to streams[prevIndex].streamId
            }
            else -> false to null
        }
    }

    @Test
    fun tvRemote_keyCode166_channelUp_triggersNextChannel() {
        val streams = listOf(
            XtreamStream(streamId = 101, name = "Canal 1"),
            XtreamStream(streamId = 102, name = "Canal 2"),
            XtreamStream(streamId = 103, name = "Canal 3")
        )

        val (handled, targetId) = handleTvRemoteKey(
            keyCode = 166, // KEYCODE_CHANNEL_UP
            isLive = true,
            streams = streams,
            currentStreamId = 101
        )

        assertEquals(true, handled)
        assertEquals(102, targetId)
    }

    @Test
    fun tvRemote_keyCode167_channelDown_triggersPreviousChannel() {
        val streams = listOf(
            XtreamStream(streamId = 101, name = "Canal 1"),
            XtreamStream(streamId = 102, name = "Canal 2"),
            XtreamStream(streamId = 103, name = "Canal 3")
        )

        val (handled, targetId) = handleTvRemoteKey(
            keyCode = 167, // KEYCODE_CHANNEL_DOWN
            isLive = true,
            streams = streams,
            currentStreamId = 101
        )

        assertEquals(true, handled)
        assertEquals(103, targetId) // Wraps around to last channel
    }

    @Test
    fun tvRemote_channelUpOnLastChannel_wrapsToFirstChannel() {
        val streams = listOf(
            XtreamStream(streamId = 101, name = "Canal 1"),
            XtreamStream(streamId = 102, name = "Canal 2")
        )

        val (handled, targetId) = handleTvRemoteKey(
            keyCode = 166, // KEYCODE_CHANNEL_UP
            isLive = true,
            streams = streams,
            currentStreamId = 102
        )

        assertEquals(true, handled)
        assertEquals(101, targetId)
    }

    @Test
    fun tvRemote_vodContent_ignoresChannelKeys() {
        val streams = listOf(
            XtreamStream(streamId = 101, name = "Filme 1")
        )

        val (handled, targetId) = handleTvRemoteKey(
            keyCode = 166, // KEYCODE_CHANNEL_UP
            isLive = false, // VOD / Movie
            streams = streams,
            currentStreamId = 101
        )

        assertEquals(false, handled)
        assertEquals(null, targetId)
    }
}
