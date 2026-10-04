package rsv.squitv.core.ui.responsive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun ResponsiveLayout(
    content: @Composable () -> Unit
) {
    BoxWithConstraints {
        val metrics = calculateResponsiveMetrics(
            availableWidth = maxWidth,
            availableHeight = maxHeight
        )
        
        CompositionLocalProvider(
            LocalResponsiveMetrics provides metrics
        ) {
            content()
        }
    }
}
