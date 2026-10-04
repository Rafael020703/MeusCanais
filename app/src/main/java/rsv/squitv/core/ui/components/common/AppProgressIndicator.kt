package rsv.squitv.core.ui.components.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rsv.squitv.core.ui.theme.AppDesignSystem

@Composable
fun AppProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 3.dp
) {
    val tokens = AppDesignSystem
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = tokens.colors.primary,
        strokeWidth = strokeWidth
    )
}
