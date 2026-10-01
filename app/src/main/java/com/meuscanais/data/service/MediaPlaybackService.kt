package com.meuscanais.data.service

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @Inject
    lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player ?: player
        try {
            p.stop()
            p.clearMediaItems()
        } catch (_: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            try {
                player.stop()
                player.clearMediaItems()
            } catch (_: Exception) {}
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
