package com.meuscanais.core.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.meuscanais.core.ui.theme.AppDesignSystem

@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = "VER TUDO",
    onActionClick: (() -> Unit)? = null
) {
    val tokens = AppDesignSystem
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.extraLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            style = tokens.typography.title,
            fontWeight = FontWeight.Black,
            color = tokens.colors.primary,
            letterSpacing = 1.sp
        )
        
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = tokens.typography.label,
                color = tokens.colors.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}
