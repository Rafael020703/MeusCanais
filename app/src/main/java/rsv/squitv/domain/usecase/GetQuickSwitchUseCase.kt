package rsv.squitv.domain.usecase

import rsv.squitv.core.data.repository.CatalogRepository
import rsv.squitv.core.data.repository.EpgRepository
import rsv.squitv.data.model.EpgProgramme
import rsv.squitv.data.model.XtreamStream
import rsv.squitv.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

/**
 * UseCase to handle data for the Quick Switch (Zapping) sidebar.
 * Fetches the list of streams and provides a flow for loading their EPG in background.
 */
class GetQuickSwitchUseCase @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val epgRepository: EpgRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Fetches the list of live streams for a category (or all if null).
     */
    suspend fun getStreams(categoryId: String?): List<XtreamStream> {
        val credentials = settingsRepository.settingsFlow.first().credentials ?: return emptyList()
        return if (categoryId != null) {
            catalogRepository.getLiveStreams(credentials, categoryId)
        } else {
            catalogRepository.getLiveStreams(credentials)
        }
    }

    /**
     * Returns a flow that fetches current EPG for the first [limit] streams and emits them.
     */
    fun loadZappingEpg(streams: List<XtreamStream>, limit: Int = 20): Flow<Pair<Int, EpgProgramme>> = flow {
        val credentials = settingsRepository.settingsFlow.first().credentials ?: return@flow
        
        // Use supervisorScope to allow parallel fetching without failing the whole flow if one fails
        supervisorScope {
            streams.take(limit).forEach { stream ->
                val streamId = stream.streamId ?: return@forEach
                launch {
                    try {
                        val response = epgRepository.getShortEpg(credentials, streamId)
                        val now = System.currentTimeMillis() / 1000
                        response.epgListings?.find { ((it.startTimestamp ?: 0) <= now) && ((it.stopTimestamp ?: 0) > now) }?.let { listing ->
                            val prog = EpgProgramme(
                                start = listing.start ?: "",
                                stop = listing.end ?: "",
                                channelId = streamId.toString(),
                                title = listing.title,
                                description = listing.description
                            )
                            emit(streamId to prog)
                        }
                    } catch (_: Exception) {
                        // Individual fetch failure, ignore for zapping UI
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
