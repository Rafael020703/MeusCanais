package rsv.squitv.core.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rsv.squitv.core.ui.theme.Spacing
import rsv.squitv.core.ui.theme.Typography

/**
 * Adaptive Spacing Tokens
 */
object ResponsiveSpacing {
    val small: Dp @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.dp(Spacing.small)
    val medium: Dp @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.dp(Spacing.medium)
    val large: Dp @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.dp(Spacing.large)
    val extraLarge: Dp @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.dp(Spacing.extraLarge)
    val huge: Dp @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.dp(Spacing.huge)
    val giant: Dp @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.dp(Spacing.giant)
}

/**
 * Adaptive Typography Tokens
 */
object ResponsiveTypography {
    val display: TextUnit @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.sp(Typography.displayLarge.fontSize)
    val headline: TextUnit @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.sp(Typography.headlineLarge.fontSize)
    val title: TextUnit @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.sp(Typography.titleLarge.fontSize)
    val body: TextUnit @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.sp(Typography.bodyLarge.fontSize)
    val caption: TextUnit @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.sp(Typography.labelSmall.fontSize)
}

/**
 * Adaptive Component Sizes
 */
object ResponsiveCards {
    val mainHeight: Dp @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.dp(280.dp)
    val secondaryHeight: Dp @Composable @ReadOnlyComposable get() = LocalResponsiveMetrics.current.dp(150.dp)
}
