package com.meuscanais.core.domain.interactor

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.meuscanais.data.service.MediaPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * PlaybackManager responsible for the technical lifecycle of the MediaController.
 * It handles connecting to the MediaPlaybackService and releasing the controller.
 */
@UnstableApi
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    sealed class Event {
        data class PlaybackStateChanged(val state: Int) : Event()
        data class TracksChanged(val tracks: Tracks) : Event()
        data class IsPlayingChanged(val isPlaying: Boolean) : Event()
        data class PlayerError(val error: PlaybackException) : Event()
    }

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    private val _playerState = MutableStateFlow<Player?>(null)
    val playerState: StateFlow<Player?> = _playerState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    private var _player: Player? = null
    val player: Player? get() = _player

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _events.tryEmit(Event.PlaybackStateChanged(playbackState))
        }

        override fun onTracksChanged(tracks: Tracks) {
            _events.tryEmit(Event.TracksChanged(tracks))
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _events.tryEmit(Event.IsPlayingChanged(isPlaying))
        }

        override fun onPlayerError(error: PlaybackException) {
            _events.tryEmit(Event.PlayerError(error))
        }
    }

    /**
     * Connects to the MediaPlaybackService and retrieves the MediaController.
     * 
     * @param onConnected Callback triggered when the controller is ready.
     */
    fun connect(onConnected: (Player) -> Unit) {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(context, ComponentName(context, MediaPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get() ?: return@addListener
                _player = controller
                _playerState.value = controller
                controller.addListener(playerListener)
                onConnected(controller)
            } catch (e: Exception) {
                Timber.e(e, "Failed to connect to MediaSession")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Releases the MediaController connection.
     */
    fun release() {
        _player?.removeListener(playerListener)
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
        _player = null
        _playerState.value = null
    }

    // --- Technical Transport Commands ---

    fun play() {
        _player?.play()
    }

    fun pause() {
        _player?.pause()
    }

    fun stop() {
        _player?.stop()
    }

    fun clearMediaItems() {
        _player?.clearMediaItems()
    }

    fun setMediaItem(item: MediaItem) {
        _player?.setMediaItem(item)
    }

    fun prepare() {
        _player?.prepare()
    }

    fun seekTo(positionMs: Long) {
        _player?.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        _player?.setPlaybackSpeed(speed)
    }

    fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        _player?.trackSelectionParameters = parameters
    }

    fun clearTrackOverrides() {
        _player?.let { p ->
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .clearOverrides()
                .build()
        }
    }
}
