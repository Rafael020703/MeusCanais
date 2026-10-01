package com.meuscanais.core.ui.components.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.meuscanais.core.ui.components.common.adaptiveFocus
import com.meuscanais.core.ui.theme.AppDesignSystem
import com.meuscanais.core.ui.theme.ThemeLive

@Composable
fun CinematicActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundImage: Any? = null,
    themeColor: Color = ThemeLive,
    isLarge: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val tokens = AppDesignSystem
    val responsive = tokens.responsive
    
    val contentAlpha by animateFloatAsState(if (isFocused) 1f else 0.8f, label = "contentAlpha")
    
    // Adaptive Dimensions
    val cardPadding = responsive.dp(tokens.spacing.large)
    val iconBoxSize = responsive.dp(if (isLarge) 64.dp else 48.dp)
    val iconSize = responsive.dp(if (isLarge) 36.dp else 28.dp)
    val actionIndicatorSize = responsive.dp(if (isLarge) 48.dp else 40.dp)
    val actionIconSize = responsive.dp(if (isLarge) 32.dp else 24.dp)
    
    val titleSize = responsive.sp(if (isLarge) 32.sp else 20.sp)
    val descSize = responsive.sp(tokens.typography.body.fontSize)

    Surface(
        modifier = modifier
            .adaptiveFocus(
                shape = tokens.shapes.large,
                glowColor = themeColor,
                focusedScale = if (isLarge) 1.05f else 1.08f,
                onFocus = { isFocused = it }
            )
            .clickable { onClick() },
        shape = tokens.shapes.large,
        color = tokens.colors.surfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Procedural Atmosphere / Fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                themeColor.copy(alpha = 0.2f),
                                tokens.colors.background.copy(alpha = 0.8f),
                                tokens.colors.background
                            )
                        )
                    )
            )

            // Background Image
            if (backgroundImage != null) {
                AsyncImage(
                    model = backgroundImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Cinematic Overlay (Triple gradient for depth and legibility)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                tokens.colors.background.copy(alpha = 0.5f),
                                tokens.colors.background.copy(alpha = 0.95f)
                            ),
                            startY = 0f
                        )
                    )
            )
            
            // Deep Vignette for focus
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                            center = Offset(0.5f, 0.5f),
                            radius = 2000f
                        )
                    )
            )
            
            // Focussed Glow Overlay (Internal)
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(themeColor.copy(alpha = 0.15f), Color.Transparent),
                                center = Offset(0.5f, 0.5f),
                                radius = 1000f
                            )
                        )
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cardPadding),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsive.dp(tokens.spacing.medium))
                ) {
                    // Category Icon with glow
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .clip(tokens.shapes.medium)
                            .background(themeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.uppercase(),
                            style = (if (isLarge) tokens.typography.display else tokens.typography.headline).copy(fontSize = titleSize),
                            color = tokens.colors.textPrimary.copy(alpha = contentAlpha),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        if (description.isNotEmpty()) {
                            Text(
                                text = description,
                                style = tokens.typography.body.copy(fontSize = descSize),
                                color = tokens.colors.textSecondary.copy(alpha = contentAlpha),
                                maxLines = 1
                            )
                        }
                    }

                    // Action Indicator (Premium ▶ Style)
                    if (isFocused) {
                        Surface(
                            shape = tokens.shapes.medium,
                            color = themeColor,
                            modifier = Modifier.size(actionIndicatorSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(actionIconSize)
                                )
                            }
                        }
                    } else {
                        // Subtle indicator when not focused
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = themeColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(actionIndicatorSize * 0.6f)
                        )
                    }
                }
            }
        }
    }
}
