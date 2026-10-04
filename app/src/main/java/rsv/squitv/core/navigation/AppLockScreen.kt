package rsv.squitv.core.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rsv.squitv.R
import rsv.squitv.core.ui.components.buttons.AppButton
import rsv.squitv.core.ui.theme.AppDesignSystem
import rsv.squitv.ui.dashboard.PortalBackground

@Composable
fun AppLockScreen(onUnlock: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val tokens = AppDesignSystem

    PortalBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(360.dp)
                    .padding(tokens.spacing.large),
                shape = tokens.shapes.extraLarge,
                color = tokens.colors.surface.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(tokens.spacing.giant)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = tokens.colors.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(tokens.spacing.large))
                    Text(
                        text = stringResource(R.string.app_locked_title).uppercase(),
                        style = tokens.typography.headline,
                        fontWeight = FontWeight.Black,
                        color = tokens.colors.textPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = stringResource(R.string.app_locked_desc),
                        style = tokens.typography.body,
                        color = tokens.colors.textSecondary,
                        modifier = Modifier.padding(top = tokens.spacing.small)
                    )
                    Spacer(modifier = Modifier.height(tokens.spacing.giant))
                    
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { 
                            if (it.length <= 4) {
                                pin = it
                                if (error) error = false
                            }
                        },
                        label = { Text(stringResource(R.string.pin_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = error,
                        shape = tokens.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.colors.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    if (error) {
                        Text(
                            text = stringResource(R.string.invalid_pin).uppercase(),
                            color = tokens.colors.error,
                            style = tokens.typography.caption,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = tokens.spacing.small)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(tokens.spacing.giant))
                    
                    AppButton(
                        text = stringResource(R.string.unlock_button).uppercase(),
                        onClick = { 
                            if (!onUnlock(pin)) {
                                error = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
