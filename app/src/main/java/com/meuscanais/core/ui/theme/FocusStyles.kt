package com.meuscanais.core.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.meuscanais.core.ui.components.common.adaptiveFocus

fun Modifier.appFocus(
    shape: Shape? = null,
    scale: Float = 1.05f,
    borderWidth: Dp = AppDimensions.focusBorderWidth,
    borderColor: Color? = null,
    onFocus: (Boolean) -> Unit = {}
): Modifier = this.adaptiveFocus(
    shape = shape ?: AppShapes.medium,
    focusedScale = scale,
    focusedBorderWidth = borderWidth,
    focusedColor = borderColor ?: FocusGlowCyan,
    onFocus = onFocus
)
