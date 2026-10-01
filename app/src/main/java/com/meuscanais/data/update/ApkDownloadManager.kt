package com.meuscanais.data.update

import android.content.Context
import com.meuscanais.di.ApiOkHttpClient
import com.meuscanais.domain.model.AppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadStatus
    data object VerifyingIntegrity : DownloadStatus
    data class Completed(val apkFile: File, val updateInfo: AppUpdateInfo) : DownloadStatus
    data class Failed(val errorMessage: String) : DownloadStatus
}

@Singleton
class ApkDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApiOkHttpClient private val okHttpClient: OkHttpClient
) {
    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val updatesDir: File
        get() {
            val dir = File(context.cacheDir, "updates")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun downloadApk(updateInfo: AppUpdateInfo) = withContext(Dispatchers.IO) {
        val cleanName = updateInfo.apkName.removeSuffix(".apk")
        val targetFile = File(updatesDir, "${cleanName}_${updateInfo.versionName}.apk")
        val tempFile = File(updatesDir, "${targetFile.name}.tmp")

        // Clean up any older APK downloads except current target
        cleanOldUpdates(targetFile)

        // If file exists and is valid, return immediately
        if (targetFile.exists() && targetFile.length() > 0) {
            _downloadStatus.value = DownloadStatus.VerifyingIntegrity
            if (verifySha256(targetFile, updateInfo.sha256Digest)) {
                _downloadStatus.value = DownloadStatus.Completed(targetFile, updateInfo)
                return@withContext
            } else {
                targetFile.delete()
            }
        }

        try {
            _downloadStatus.value = DownloadStatus.Downloading(0, 0L, updateInfo.apkSize)

            val request = Request.Builder()
                .url(updateInfo.apkUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _downloadStatus.value = DownloadStatus.Failed("Erro HTTP ao baixar APK: ${response.code}")
                return@withContext
            }

            val body = response.body ?: run {
                _downloadStatus.value = DownloadStatus.Failed("Resposta vazia ao baixar APK")
                return@withContext
            }

            val totalBytes = if (body.contentLength() > 0) body.contentLength() else updateInfo.apkSize
            var bytesDownloaded = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
                        _downloadStatus.value = DownloadStatus.Downloading(
                            progressPercent = progress.coerceIn(0, 100),
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes
                        )
                    }
                    output.flush()
                }
            }

            _downloadStatus.value = DownloadStatus.VerifyingIntegrity

            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                if (tempFile.renameTo(targetFile)) {
                    if (verifySha256(targetFile, updateInfo.sha256Digest)) {
                        _downloadStatus.value = DownloadStatus.Completed(targetFile, updateInfo)
                    } else {
                        targetFile.delete()
                        _downloadStatus.value = DownloadStatus.Failed("Não foi possível validar a integridade da atualização (SHA-256 incorreto).")
                    }
                } else {
                    _downloadStatus.value = DownloadStatus.Failed("Erro ao salvar o arquivo baixado.")
                }
            } else {
                _downloadStatus.value = DownloadStatus.Failed("Arquivo temporário não encontrado.")
            }

        } catch (e: Exception) {
            Timber.e(e, "Erro durante download do APK")
            if (tempFile.exists()) tempFile.delete()
            _downloadStatus.value = DownloadStatus.Failed("Falha no download: ${e.message}")
        }
    }

    fun cancelDownload() {
        _downloadStatus.value = DownloadStatus.Idle
        cleanOldUpdates()
    }

    fun resetState() {
        _downloadStatus.value = DownloadStatus.Idle
    }

    private fun cleanOldUpdates(keepFile: File? = null) {
        try {
            updatesDir.listFiles()?.forEach { file ->
                if (file != keepFile && (file.name.endsWith(".apk") || file.name.endsWith(".tmp"))) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    private fun verifySha256(file: File, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) {
            return file.exists() && file.length() > 0
        }
        val cleanExpected = expectedSha256.trim()
            .removePrefix("sha256:")
            .removePrefix("SHA256:")
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }
            calculatedHash.equals(cleanExpected, ignoreCase = true)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao validar SHA-256")
            false
        }
    }
}
