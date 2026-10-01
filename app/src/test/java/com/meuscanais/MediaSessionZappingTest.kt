package com.meuscanais

import com.meuscanais.core.domain.interactor.PlaybackManager
import com.meuscanais.data.model.XtreamStream
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSessionZappingTest {

    private fun getNextIndex(streams: List<XtreamStream>, currentStreamId: Int): Int? {
        if (streams.isEmpty()) return null
        val currentIndex = streams.indexOfFirst { it.streamId == currentStreamId }
        return if (currentIndex == -1) 0 else (currentIndex + 1) % streams.size
    }

    private fun getPreviousIndex(streams: List<XtreamStream>, currentStreamId: Int): Int? {
        if (streams.isEmpty()) return null
        val currentIndex = streams.indexOfFirst { it.streamId == currentStreamId }
        return if (currentIndex <= 0) streams.size - 1 else currentIndex - 1
    }

    @Test
    fun zapping_nextChannel_wrapsAround() {
        val streams = listOf(
            XtreamStream(streamId = 101, name = "Canal 1", streamIcon = "logo1.png", epgChannelId = "epg1"),
            XtreamStream(streamId = 102, name = "Canal 2", streamIcon = "logo2.png", epgChannelId = "epg2"),
            XtreamStream(streamId = 103, name = "Canal 3", streamIcon = "logo3.png", epgChannelId = "epg3")
        )

        // 101 -> 102
        val idx1 = getNextIndex(streams, 101)!!
        assertEquals(1, idx1)
        assertEquals("Canal 2", streams[idx1].name)

        // 102 -> 103
        val idx2 = getNextIndex(streams, 102)!!
        assertEquals(2, idx2)
        assertEquals("Canal 3", streams[idx2].name)

        // 103 -> 101 (Wrap around)
        val idx3 = getNextIndex(streams, 103)!!
        assertEquals(0, idx3)
        assertEquals("Canal 1", streams[idx3].name)
    }

    @Test
    fun zapping_previousChannel_wrapsAround() {
        val streams = listOf(
            XtreamStream(streamId = 101, name = "Canal 1", streamIcon = "logo1.png", epgChannelId = "epg1"),
            XtreamStream(streamId = 102, name = "Canal 2", streamIcon = "logo2.png", epgChannelId = "epg2"),
            XtreamStream(streamId = 103, name = "Canal 3", streamIcon = "logo3.png", epgChannelId = "epg3")
        )

        // 101 -> 103 (Wrap around)
        val idx1 = getPreviousIndex(streams, 101)!!
        assertEquals(2, idx1)
        assertEquals("Canal 3", streams[idx1].name)

        // 103 -> 102
        val idx2 = getPreviousIndex(streams, 103)!!
        assertEquals(1, idx2)
        assertEquals("Canal 2", streams[idx2].name)

        // 102 -> 101
        val idx3 = getPreviousIndex(streams, 102)!!
        assertEquals(0, idx3)
        assertEquals("Canal 1", streams[idx3].name)
    }

    @Test
    fun zapping_singleChannel_returnsSameChannel() {
        val streams = listOf(
            XtreamStream(streamId = 101, name = "Canal Único", streamIcon = "logo1.png")
        )

        val nextIdx = getNextIndex(streams, 101)!!
        val prevIdx = getPreviousIndex(streams, 101)!!

        assertEquals(0, nextIdx)
        assertEquals(0, prevIdx)
        assertEquals("Canal Único", streams[nextIdx].name)
    }

    @Test
    fun zapping_emptyList_returnsNull() {
        val streams = emptyList<XtreamStream>()

        val nextIdx = getNextIndex(streams, 101)
        val prevIdx = getPreviousIndex(streams, 101)

        assertEquals(null, nextIdx)
        assertEquals(null, prevIdx)
    }

    @Test
    fun zapping_channelNotFoundInList_defaultsToFirstOrLast() {
        val streams = listOf(
            XtreamStream(streamId = 101, name = "Canal 1"),
            XtreamStream(streamId = 102, name = "Canal 2")
        )

        // Unknown stream ID 999 defaults to index 0 for next, index 1 (last) for previous
        val nextIdx = getNextIndex(streams, 999)!!
        val prevIdx = getPreviousIndex(streams, 999)!!

        assertEquals(0, nextIdx)
        assertEquals(1, prevIdx)
    }

    @Test
    fun playbackManager_eventTypes_areCorrect() {
        val nextEvent = PlaybackManager.Event.NextChannelRequested
        val prevEvent = PlaybackManager.Event.PreviousChannelRequested

        assertEquals(PlaybackManager.Event.NextChannelRequested, nextEvent)
        assertEquals(PlaybackManager.Event.PreviousChannelRequested, prevEvent)
    }
}
