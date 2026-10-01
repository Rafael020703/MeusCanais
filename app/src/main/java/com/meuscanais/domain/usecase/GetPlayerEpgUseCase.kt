package com.meuscanais.domain.usecase

import com.meuscanais.core.data.repository.EpgRepository
import com.meuscanais.data.model.EpgListing
import com.meuscanais.data.model.EpgProgramme
import com.meuscanais.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * UseCase to fetch and process EPG for a specific stream.
 * Returns the current program, next upcoming programs, and the full list of listings.
 */
class GetPlayerEpgUseCase @Inject constructor(
    private val epgRepository: EpgRepository,
    private val settingsRepository: SettingsRepository
) {
    data class Result(
        val current: EpgProgramme?,
        val next: List<EpgProgramme>,
        val allListings: List<EpgListing>
    )

    suspend operator fun invoke(streamId: Int): Result {
        val credentials = settingsRepository.settingsFlow.first().credentials 
            ?: return Result(null, emptyList(), emptyList())

        val response = epgRepository.getShortEpg(credentials, streamId)
        val listings = response.epgListings ?: emptyList()
        val nowTimestamp = System.currentTimeMillis() / 1000

        val current = listings.find { prog ->
            val start = prog.startTimestamp ?: 0L
            val stop = prog.stopTimestamp ?: 0L
            start != 0L && stop != 0L && nowTimestamp >= start && nowTimestamp < stop
        }?.let {
            EpgProgramme(it.start ?: "", it.end ?: "", streamId.toString(), it.title, it.description)
        }

        val next = listings.filter { (it.startTimestamp ?: 0L) > nowTimestamp }
            .sortedBy { it.startTimestamp }
            .take(5)
            .map {
                EpgProgramme(it.start ?: "", it.end ?: "", streamId.toString(), it.title, it.description)
            }

        return Result(current, next, listings)
    }
}
