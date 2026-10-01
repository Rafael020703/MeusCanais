package com.meuscanais.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meuscanais.core.data.repository.EpgRepository
import com.meuscanais.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val epgRepository: EpgRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("Iniciando EpgSyncWorker")
        val creds = settingsRepository.settingsFlow.first().credentials ?: return Result.failure()
        
        return try {
            epgRepository.syncFullEpg(creds)
            Timber.i("EpgSyncWorker concluído")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Falha no EpgSyncWorker")
            Result.retry()
        }
    }
}
