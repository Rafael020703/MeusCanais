package com.meuscanais.core.ui.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.meuscanais.core.ui.theme.AppDesignSystem
import com.meuscanais.core.ui.components.common.adaptiveFocus

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onFocus: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = AppDesignSystem
    val baseShape = shape ?: tokens.shapes.card
    val baseContainerColor = containerColor ?: tokens.colors.surface.copy(alpha = 0.5f)
    val baseContentColor = contentColor ?: tokens.colors.textPrimary

    Surface(
        modifier = modifier
            .adaptiveFocus(
                shape = baseShape,
                onFocus = onFocus
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = baseShape,
        color = baseContainerColor,
        contentColor = baseContentColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
