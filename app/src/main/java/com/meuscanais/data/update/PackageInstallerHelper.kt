package com.meuscanais.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.meuscanais.util.AppVersionProvider
import timber.log.Timber
import java.io.File

object PackageInstallerHelper {

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownAppSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e, "Erro ao abrir configurações de fontes desconhecidas")
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun isApkValid(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0) {
            Timber.e("Arquivo APK não existe ou está vazio")
            return false
        }
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (packageInfo == null) {
                Timber.e("Não foi possível analisar as informações do pacote APK")
                return false
            }
            if (packageInfo.packageName != context.packageName) {
                Timber.e("O packageName do APK (%s) não coincide com o aplicativo instalado (%s)", packageInfo.packageName, context.packageName)
                return false
            }

            val apkVersionName = packageInfo.versionName ?: ""
            val currentVersionName = AppVersionProvider.getVersionName(context)

            if (VersionComparator.compareVersions(apkVersionName, currentVersionName) <= 0) {
                Timber.i("O APK baixado (%s) não é mais recente que a versão instalada (%s). Limpando arquivo.", apkVersionName, currentVersionName)
                try { apkFile.delete() } catch (_: Exception) {}
                return false
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "Erro ao validar estrutura do arquivo APK antes da instalação")
            false
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        if (!isApkValid(context, apkFile)) {
            Timber.e("Arquivo APK inválido ou incompatível para instalação")
            return false
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.e(e, "Erro ao iniciar instalação do APK")
            false
        }
    }
}
