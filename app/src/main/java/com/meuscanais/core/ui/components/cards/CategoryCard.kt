package com.meuscanais.core.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meuscanais.core.ui.theme.*

@Composable
fun CategoryCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val tokens = AppDesignSystem
    
    AppCard(
        modifier = modifier.height(100.dp),
        onClick = onClick,
        onFocus = { isFocused = it },
        containerColor = if (isFocused) tokens.colors.primary 
                        else if (isSelected) tokens.colors.primary.copy(alpha = 0.2f)
                        else tokens.colors.surfaceVariant.copy(alpha = 0.1f),
        contentColor = if (isFocused) tokens.colors.background else tokens.colors.textPrimary
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(tokens.spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(tokens.spacing.medium))
            Text(
                text = title.uppercase(),
                style = tokens.typography.title,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}
