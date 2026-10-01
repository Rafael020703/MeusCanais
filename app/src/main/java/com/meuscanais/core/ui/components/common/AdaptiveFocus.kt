package com.meuscanais.core.ui.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meuscanais.core.ui.theme.AppDimensions
import com.meuscanais.core.ui.theme.AppShapes
import com.meuscanais.core.ui.theme.FocusGlowCyan
import com.meuscanais.util.DeviceType
import com.meuscanais.util.rememberWindowInfo

/**
 * Adaptive focus modifier that handles scale and border highlights for TV and Mobile.
 */
fun Modifier.adaptiveFocus(
    shape: Shape = AppShapes.medium,
    focusedBorderWidth: Dp = AppDimensions.focusBorderWidth,
    unfocusedBorderWidth: Dp = AppDimensions.standardBorderWidth,
    focusedScale: Float = 1.1f,
    focusedColor: Color = FocusGlowCyan,
    unfocusedColor: Color = Color.White.copy(alpha = 0.1f),
    glowColor: Color? = null,
    onFocus: (Boolean) -> Unit = {}
) = composed {
    val windowInfo = rememberWindowInfo()
    val isTv = windowInfo.deviceType == DeviceType.TV
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused && isTv) focusedScale else 1.0f,
        label = "focusScale"
    )
    
    val finalFocusedColor = glowColor ?: focusedColor
    
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) finalFocusedColor else unfocusedColor,
        label = "focusBorderColor"
    )

    this
        .onFocusChanged { 
            isFocused = it.isFocused
            onFocus(it.isFocused)
        }
        .scale(scale)
        .then(
            if (isTv || isFocused) {
                Modifier.border(
                    BorderStroke(if (isFocused) focusedBorderWidth else unfocusedBorderWidth, borderColor),
                    shape
                )
            } else Modifier
        )
        .then(
            if (isFocused && isTv) {
                Modifier.shadow(
                    elevation = if (glowColor != null) 24.dp else 16.dp, 
                    shape = shape, 
                    ambientColor = finalFocusedColor.copy(alpha = 0.5f), 
                    spotColor = finalFocusedColor
                )
            } else Modifier
        )
}
