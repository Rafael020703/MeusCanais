package rsv.squitv.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import rsv.squitv.IptvApplication

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
@UnstableApi
class DownloadTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TAG = "DownloadTracker"

    fun startDownload(mediaItem: MediaItem, displayName: String) {
        Log.d(TAG, "Starting download for: $displayName ID: ${mediaItem.mediaId}")
        
        val downloadRequest = DownloadRequest.Builder(mediaItem.mediaId, mediaItem.localConfiguration?.uri ?: Uri.EMPTY)
            .setData(displayName.toByteArray(Charsets.UTF_8))
            .build()
        
        try {
            DownloadService.sendAddDownload(
                context,
                IptvDownloadService::class.java,
                downloadRequest,
                /* foreground= */ true
            )
            Log.d(TAG, "Download request sent to service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download", e)
        }
    }

    fun removeDownload(mediaId: String) {
        Log.d(TAG, "Removing and stopping download: $mediaId")
        // Force stop the download before removing it
        DownloadService.sendSetStopReason(
            context,
            IptvDownloadService::class.java,
            mediaId,
            Download.STOP_REASON_NONE,
            /* foreground= */ true
        )
        DownloadService.sendRemoveDownload(
            context,
            IptvDownloadService::class.java,
            mediaId,
            /* foreground= */ true
        )
    }
}
