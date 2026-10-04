package rsv.squitv.core.ui.components.states

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import rsv.squitv.core.ui.theme.*
import rsv.squitv.core.ui.components.buttons.AppButton

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val tokens = AppDesignSystem
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = tokens.colors.primary,
                strokeWidth = 3.dp
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(tokens.spacing.large))
                Text(
                    text = message.uppercase(),
                    style = tokens.typography.label,
                    color = tokens.colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector = Icons.Rounded.Info,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val tokens = AppDesignSystem
    Box(
        modifier = modifier.fillMaxSize().padding(tokens.spacing.huge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = tokens.colors.textPrimary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(tokens.spacing.extraLarge))
            Text(
                text = title.uppercase(),
                style = tokens.typography.headline,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = tokens.colors.textPrimary
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(tokens.spacing.medium))
                Text(
                    text = description,
                    style = tokens.typography.body,
                    color = tokens.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(tokens.spacing.huge))
                AppButton(text = actionText, onClick = onActionClick)
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    EmptyState(
        title = "Ops! Ocorreu um erro",
        description = message,
        icon = Icons.Rounded.Error,
        actionText = if (onRetry != null) "Tentar Novamente" else null,
        onActionClick = onRetry,
        modifier = modifier
    )
}
