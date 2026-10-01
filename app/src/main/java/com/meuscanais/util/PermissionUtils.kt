package com.meuscanais.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionUtils {
    
    val REQUIRED_PERMISSIONS = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getPermissionLabel(permission: String): String {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> "Notificações"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Armazenamento"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Armazenamento"
            else -> permission.split(".").last()
        }
    }

    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> "Necessário para enviar lembretes de programas e status de downloads."
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Necessário para salvar filmes e episódios no dispositivo."
            else -> "Permissão opcional para melhor funcionamento do app."
        }
    }
}
