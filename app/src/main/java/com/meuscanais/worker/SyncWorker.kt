package com.meuscanais.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meuscanais.domain.usecase.SyncDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncDataUseCase: SyncDataUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val force = inputData.getBoolean("force", false)
        Timber.d("Iniciando SyncWorker (force=%b)", force)
        
        return try {
            syncDataUseCase(force).collect {
                // Podemos logar o progresso aqui se necessário
            }
            Timber.i("SyncWorker concluído com sucesso")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Erro no SyncWorker")
            Result.failure()
        }
    }
}
