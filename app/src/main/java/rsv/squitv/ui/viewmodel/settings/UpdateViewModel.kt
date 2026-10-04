package rsv.squitv.ui.viewmodel.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import rsv.squitv.data.repository.SettingsRepository
import rsv.squitv.data.repository.UpdateRepository
import rsv.squitv.data.update.ApkDownloadManager
import rsv.squitv.data.update.DownloadStatus
import rsv.squitv.data.update.PackageInstallerHelper
import rsv.squitv.data.update.VersionComparator
import rsv.squitv.domain.model.AppUpdateInfo
import rsv.squitv.domain.model.UpdateCheckResult
import rsv.squitv.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateUiState {
    data object Checking : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class UpdateAvailable(
        val currentVersion: String,
        val updateInfo: AppUpdateInfo,
        val isIgnored: Boolean = false
    ) : UpdateUiState
    data class Downloading(
        val updateInfo: AppUpdateInfo,
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateUiState
    data class ReadyToInstall(
        val updateInfo: AppUpdateInfo,
        val apkFile: File
    ) : UpdateUiState
    data class PermissionRequired(
        val updateInfo: AppUpdateInfo,
        val apkFile: File
    ) : UpdateUiState
    data class Error(val message: String, val updateInfo: AppUpdateInfo? = null) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateRepository: UpdateRepository,
    private val apkDownloadManager: ApkDownloadManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Checking)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    val currentVersionName: String
        get() = updateRepository.currentVersionName

    private var activeUpdateInfo: AppUpdateInfo? = null
    private var downloadedApkFile: File? = null

    init {
        observeDownloadStatus()
        checkForUpdates(force = false)
    }

    private fun observeDownloadStatus() {
        viewModelScope.launch {
            apkDownloadManager.downloadStatus.collect { status ->
                val currentInfo = activeUpdateInfo
                when (status) {
                    is DownloadStatus.Idle -> {}
                    is DownloadStatus.Downloading -> {
                        if (currentInfo != null) {
                            _uiState.value = UpdateUiState.Downloading(
                                updateInfo = currentInfo,
                                progressPercent = status.progressPercent,
                                bytesDownloaded = status.bytesDownloaded,
                                totalBytes = status.totalBytes
                            )
                        }
                    }
                    is DownloadStatus.VerifyingIntegrity -> {
                        if (currentInfo != null) {
                            _uiState.value = UpdateUiState.Downloading(
                                updateInfo = currentInfo,
                                progressPercent = 100,
                                bytesDownloaded = currentInfo.apkSize,
                                totalBytes = currentInfo.apkSize
                            )
                        }
                    }
                    is DownloadStatus.Completed -> {
                        if (VersionComparator.compareVersions(status.updateInfo.versionName, currentVersionName) <= 0) {
                            apkDownloadManager.resetState()
                            _uiState.value = UpdateUiState.UpToDate(currentVersionName)
                        } else {
                            downloadedApkFile = status.apkFile
                            activeUpdateInfo = status.updateInfo
                            if (PackageInstallerHelper.canRequestPackageInstalls(context)) {
                                _uiState.value = UpdateUiState.ReadyToInstall(status.updateInfo, status.apkFile)
                            } else {
                                _uiState.value = UpdateUiState.PermissionRequired(status.updateInfo, status.apkFile)
                            }
                        }
                    }
                    is DownloadStatus.Failed -> {
                        _uiState.value = UpdateUiState.Error(status.errorMessage, activeUpdateInfo)
                    }
                }
            }
        }
    }

    fun checkForUpdates(force: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            apkDownloadManager.resetState()

            when (val result = updateRepository.checkForUpdates(force = force)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    activeUpdateInfo = result.updateInfo
                    val settings = settingsRepository.settingsFlow.first()
                    val isIgnored = settings.ignoredVersion == result.updateInfo.versionName

                    _uiState.value = UpdateUiState.UpdateAvailable(
                        currentVersion = currentVersionName,
                        updateInfo = result.updateInfo,
                        isIgnored = isIgnored
                    )

                    if (settings.lastNotifiedVersion != result.updateInfo.versionName && !isIgnored) {
                        NotificationHelper.showUpdateNotification(context, result.updateInfo)
                        settingsRepository.updateLastNotifiedVersion(result.updateInfo.versionName)
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _uiState.value = UpdateUiState.UpToDate(currentVersionName)
                }
                is UpdateCheckResult.NoCompatibleApk -> {
                    _uiState.value = UpdateUiState.Error("Esta atualização não possui um APK compatível com este dispositivo.")
                }
                is UpdateCheckResult.Error -> {
                    _uiState.value = UpdateUiState.Error(result.message)
                }
            }
        }
    }

    fun startDownload() {
        val info = activeUpdateInfo ?: return
        viewModelScope.launch {
            apkDownloadManager.downloadApk(info)
        }
    }

    fun cancelDownload() {
        apkDownloadManager.cancelDownload()
        val info = activeUpdateInfo
        if (info != null) {
            _uiState.value = UpdateUiState.UpdateAvailable(
                currentVersion = currentVersionName,
                updateInfo = info
            )
        } else {
            checkForUpdates(force = false)
        }
    }

    fun installUpdate() {
        val file = downloadedApkFile ?: return
        if (!PackageInstallerHelper.canRequestPackageInstalls(context)) {
            val info = activeUpdateInfo
            if (info != null) {
                _uiState.value = UpdateUiState.PermissionRequired(info, file)
            }
            return
        }
        PackageInstallerHelper.installApk(context, file)
    }

    fun openSettingsForPermission() {
        PackageInstallerHelper.openUnknownAppSourcesSettings(context)
    }

    fun checkPermissionAndInstall() {
        val file = downloadedApkFile ?: return
        val info = activeUpdateInfo ?: return
        if (PackageInstallerHelper.canRequestPackageInstalls(context)) {
            _uiState.value = UpdateUiState.ReadyToInstall(info, file)
            PackageInstallerHelper.installApk(context, file)
        } else {
            _uiState.value = UpdateUiState.PermissionRequired(info, file)
        }
    }

    fun ignoreVersion() {
        val info = activeUpdateInfo ?: return
        viewModelScope.launch {
            settingsRepository.updateIgnoredVersion(info.versionName)
            _uiState.value = UpdateUiState.UpToDate(currentVersionName)
        }
    }
}
