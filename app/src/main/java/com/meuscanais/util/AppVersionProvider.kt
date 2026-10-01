package com.meuscanais.util

import android.content.Context
import android.os.Build
import com.meuscanais.BuildConfig

/**
 * Provedor centralizado para informações de versão do aplicativo.
 * Obtém dinamicamente os metadados da aplicação instalada via [android.content.pm.PackageManager]
 * com fallback seguro para os valores de [BuildConfig].
 */
object AppVersionProvider {

    /**
     * Retorna o versionName bruto definido na build (ex: "1.2" ou "2.0.1").
     */
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    /**
     * Retorna o versionName formatado para apresentação visual na UI (ex: "v1.2" ou "v2.0.1").
     * Adiciona o prefixo 'v' se ele ainda não estiver presente no versionName.
     */
    fun getFormattedVersionName(context: Context): String {
        val rawVersion = getVersionName(context)
        return if (rawVersion.startsWith("v", ignoreCase = true)) {
            rawVersion
        } else {
            "v$rawVersion"
        }
    }

    /**
     * Retorna o versionCode numérico do aplicativo instalado.
     */
    fun getVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            BuildConfig.VERSION_CODE.toLong()
        }
    }
}
