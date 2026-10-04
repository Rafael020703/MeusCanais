package rsv.squitv.domain.usecase

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import rsv.squitv.core.data.repository.CatalogRepository
import rsv.squitv.core.data.repository.StreamRepository
import rsv.squitv.core.data.repository.UserRepository
import rsv.squitv.data.local.entities.DownloadEntity
import rsv.squitv.data.repository.SettingsRepository
import rsv.squitv.data.service.DownloadTracker
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase for the Smart Download logic.
 * Decides if the next episode of a series should be downloaded based on user settings
 * and initiates the download if necessary.
 */
@UnstableApi
class SmartDownloadUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val catalogRepository: CatalogRepository,
    private val userRepository: UserRepository,
    private val streamRepository: StreamRepository,
    private val downloadTracker: DownloadTracker
) {
    /**
     * Checks and downloads the next episode of a series.
     * 
     * @param seriesId The ID of the current series.
     * @param currentSeason The current season number.
     * @param currentStreamId The ID of the current episode stream.
     */
    suspend operator fun invoke(seriesId: Int, currentSeason: Int, currentStreamId: Int) {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.smartDownloadsEnabled) return

        val creds = settings.credentials ?: return
        val (_, episodesMap) = catalogRepository.getSeriesInfo(creds, seriesId)
        val episodes = episodesMap[currentSeason] ?: return
        
        val currentIndex = episodes.indexOfFirst { it.streamId == currentStreamId }
        if (currentIndex != -1 && currentIndex < episodes.size - 1) {
            val nextEpisode = episodes[currentIndex + 1]
            
            // Check if already downloading or downloaded
            val download = userRepository.getDownload(nextEpisode.streamId)
            if (download == null) {
                Timber.i("Smart Download: Iniciando download do próximo episódio: ${nextEpisode.title}")
                
                val url = streamRepository.getStreamUrl(creds, nextEpisode.streamId, "series", nextEpisode.containerExtension)
                val mediaItem = MediaItem.Builder().setMediaId(nextEpisode.streamId.toString()).setUri(url).build()
                
                val downloadEntity = DownloadEntity(
                    streamId = nextEpisode.streamId, 
                    name = nextEpisode.title, 
                    type = "series", 
                    icon = nextEpisode.image,
                    localUri = url, 
                    status = "DOWNLOADING", 
                    container = nextEpisode.containerExtension,
                    duration = nextEpisode.duration, 
                    seriesId = seriesId, 
                    seasonNumber = currentSeason
                )
                userRepository.insertDownload(downloadEntity)
                downloadTracker.startDownload(mediaItem, nextEpisode.title)
            }
        }
    }
}
