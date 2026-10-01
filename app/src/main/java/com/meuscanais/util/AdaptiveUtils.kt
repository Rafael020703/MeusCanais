package com.meuscanais.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSize {
    COMPACT,   // Phone portrait
    MEDIUM,    // Phone landscape / Small tablet
    EXPANDED   // Large tablet / TV
}

enum class DeviceType {
    PHONE,
    TABLET,
    TV
}

data class WindowInfo(
    val widthSize: WindowSize,
    val heightSize: WindowSize,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val deviceType: DeviceType
) {
    val isTablet: Boolean get() = deviceType == DeviceType.TABLET
    val isTv: Boolean get() = deviceType == DeviceType.TV
    val isPhone: Boolean get() = deviceType == DeviceType.PHONE
    val isExpanded: Boolean get() = widthSize == WindowSize.EXPANDED
}

@Composable
fun rememberWindowInfo(): WindowInfo {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val widthSize = when {
        screenWidth < 600.dp -> WindowSize.COMPACT
        screenWidth < 840.dp -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }

    val heightSize = when {
        screenHeight < 480.dp -> WindowSize.COMPACT
        screenHeight < 900.dp -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }

    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val deviceType = when {
        uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION -> DeviceType.TV
        widthSize == WindowSize.EXPANDED || (widthSize == WindowSize.MEDIUM && heightSize == WindowSize.EXPANDED) -> DeviceType.TABLET
        else -> DeviceType.PHONE
    }

    return WindowInfo(
        widthSize = widthSize,
        heightSize = heightSize,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        deviceType = deviceType
    )
}
