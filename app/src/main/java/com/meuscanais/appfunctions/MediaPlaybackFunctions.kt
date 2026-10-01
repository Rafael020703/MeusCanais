package com.meuscanais.appfunctions

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import com.meuscanais.core.data.repository.CatalogRepository
import com.meuscanais.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppFunctions to expose media playback and channel switching to the system.
 */
@Singleton
class MediaPlaybackFunctions @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val settingsRepository: SettingsRepository,
    private val actionBus: AppFunctionActionBus
) {
    /**
     * Plays a live channel by name.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun playChannel(
        @Suppress("UNUSED_PARAMETER") context: AppFunctionContext,
        channelName: String
    ): String {
        val settings = settingsRepository.settingsFlow.first()
        if (settings.credentials == null) return "Error: No credentials configured. Please log in first."
        
        val streams = catalogRepository.searchStreams(channelName, "LIVE")
        val channel = streams.firstOrNull() ?: return "Error: Channel '$channelName' not found."
        
        actionBus.emit(AppFunctionActionBus.Action.PlayMedia(
            streamId = channel.id,
            name = channel.name,
            type = "live",
            epgId = channel.url
        ))
        
        return "Now playing ${channel.name}"
    }

    /**
     * Plays a movie by name.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun playMovie(
        @Suppress("UNUSED_PARAMETER") context: AppFunctionContext,
        movieName: String
    ): String {
        val settings = settingsRepository.settingsFlow.first()
        if (settings.credentials == null) return "Error: No credentials configured. Please log in first."
        
        val streams = catalogRepository.searchStreams(movieName, "VOD")
        val movie = streams.firstOrNull() ?: return "Error: Movie '$movieName' not found."
        
        actionBus.emit(AppFunctionActionBus.Action.PlayMedia(
            streamId = movie.id,
            name = movie.name,
            type = "movie",
            container = movie.containerExtension
        ))
        
        return "Starting movie ${movie.name}"
    }

    /**
     * Pauses the current playback.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun pausePlayback(@Suppress("UNUSED_PARAMETER") context: AppFunctionContext): String {
        actionBus.emit(AppFunctionActionBus.Action.Pause)
        return "Playback paused"
    }

    /**
     * Resumes the current playback.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun resumePlayback(@Suppress("UNUSED_PARAMETER") context: AppFunctionContext): String {
        actionBus.emit(AppFunctionActionBus.Action.Resume)
        return "Playback resumed"
    }
}
