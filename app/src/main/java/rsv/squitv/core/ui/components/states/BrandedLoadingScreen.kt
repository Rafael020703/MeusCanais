package rsv.squitv.core.ui.components.states

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rsv.squitv.core.ui.theme.*

@Composable
fun BrandedLoadingScreen(
    message: String = "Carregando seu conteúdo..."
) {
    val tokens = AppDesignSystem
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Background Glow
        Box(
            modifier = Modifier
                .size(250.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tokens.colors.primary.copy(alpha = alpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // App Logo Container
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    color = tokens.colors.surfaceVariant,
                    tonalElevation = 4.dp,
                    border = BorderStroke(2.dp, tokens.colors.primary.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = tokens.colors.primary,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(tokens.spacing.giant))
            
            CircularProgressIndicator(
                color = tokens.colors.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(tokens.spacing.extraLarge))
            
            Text(
                text = message.uppercase(),
                color = tokens.colors.textSecondary,
                style = tokens.typography.label,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
    }
}
