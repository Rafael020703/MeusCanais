package com.meuscanais.data.repository

import android.content.Context
import com.meuscanais.data.update.*
import com.meuscanais.domain.model.AppUpdateInfo
import com.meuscanais.domain.model.UpdateCheckResult
import com.meuscanais.util.AppVersionProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubApiService: GitHubApiService,
    private val settingsRepository: SettingsRepository
) {

    val currentVersionName: String
        get() = AppVersionProvider.getFormattedVersionName(context)

    val currentVersionCode: Long
        get() = AppVersionProvider.getVersionCode(context)

    suspend fun checkForUpdates(force: Boolean = false): UpdateCheckResult {
        try {
            val settings = settingsRepository.settingsFlow.first()
            val now = System.currentTimeMillis()
            val lastCheck = settings.lastUpdateCheckTimestamp

            // Throttle automatic background checks (12h interval) unless user manually forced check
            if (!force && lastCheck > 0 && (now - lastCheck < TimeUnit.HOURS.toMillis(12))) {
                val lastKnownVersion = settings.lastAvailableVersion
                if (lastKnownVersion != null && VersionComparator.isUpdateAvailable(currentVersionName, lastKnownVersion)) {
                    // Update is available from last check
                }
            }

            val release = gitHubApiService.getLatestRelease()

            if (release.isDraft || release.isPrerelease) {
                return UpdateCheckResult.UpToDate(currentVersionName)
            }

            val versionTag = release.tagName.ifBlank { release.name ?: "" }
            if (versionTag.isBlank()) {
                return UpdateCheckResult.Error("Versão da release não identificada.")
            }

            val cleanRemoteVersion = versionTag.trim().removePrefix("v").removePrefix("V")

            if (!VersionComparator.isUpdateAvailable(currentVersionName, cleanRemoteVersion)) {
                settingsRepository.updateLastUpdateCheck(now, null)
                try {
                    File(context.cacheDir, "updates").deleteRecursively()
                } catch (_: Exception) {}
                return UpdateCheckResult.UpToDate(currentVersionName)
            }

            // Find valid APK asset, preferring EXPECTED_APK_PREFIX
            val apkAsset = release.assets.find { asset ->
                asset.name.startsWith(GitHubUpdateConfig.EXPECTED_APK_PREFIX, ignoreCase = true) &&
                (asset.name.endsWith(".apk", ignoreCase = true) || asset.contentType == "application/vnd.android.package-archive")
            } ?: release.assets.find { asset ->
                asset.name.endsWith(".apk", ignoreCase = true) ||
                asset.contentType == "application/vnd.android.package-archive"
            } ?: return UpdateCheckResult.NoCompatibleApk

            // Extract SHA256 if provided in asset or in body description
            val sha256 = apkAsset.digest
                ?.removePrefix("sha256:")
                ?.removePrefix("SHA256:")
                ?.takeIf { it.isNotBlank() }
                ?: extractSha256FromBody(release.body)

            val updateInfo = AppUpdateInfo(
                versionName = cleanRemoteVersion,
                releaseName = release.name ?: "Meus Canais $cleanRemoteVersion",
                tagName = release.tagName,
                publishedAt = release.publishedAt ?: "",
                changelog = release.body ?: "",
                releaseUrl = release.htmlUrl,
                apkUrl = apkAsset.downloadUrl,
                apkName = apkAsset.name,
                apkSize = apkAsset.size,
                sha256Digest = sha256
            )

            settingsRepository.updateLastUpdateCheck(now, cleanRemoteVersion)
            return UpdateCheckResult.UpdateAvailable(updateInfo)

        } catch (e: Exception) {
            Timber.e(e, "Erro ao consultar API de releases do GitHub")
            return UpdateCheckResult.Error(e.message ?: "Não foi possível verificar atualizações agora.")
        }
    }

    private fun extractSha256FromBody(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val regex = Regex("[a-fA-F0-9]{64}")
        return regex.find(body)?.value
    }
}
