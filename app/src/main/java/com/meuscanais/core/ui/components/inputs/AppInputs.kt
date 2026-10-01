package com.meuscanais.core.ui.components.inputs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.meuscanais.core.ui.theme.*

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val tokens = AppDesignSystem
    val responsive = tokens.responsive
    
    val height = responsive.dp(tokens.dimensions.minTouchTarget + tokens.spacing.small)
    val fontSize = responsive.sp(tokens.typography.body.fontSize)
    val labelSize = responsive.sp(tokens.typography.label.fontSize)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(height),
        placeholder = { 
            Text(
                text = placeholder.uppercase(), 
                style = tokens.typography.label.copy(fontSize = labelSize),
                color = tokens.colors.textSecondary,
                fontWeight = FontWeight.Bold
            ) 
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        shape = tokens.shapes.large,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tokens.colors.primary,
            unfocusedBorderColor = tokens.colors.textPrimary.copy(alpha = 0.1f),
            focusedContainerColor = tokens.colors.textPrimary.copy(alpha = 0.1f),
            unfocusedContainerColor = tokens.colors.textPrimary.copy(alpha = 0.05f),
            cursorColor = tokens.colors.primary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        textStyle = tokens.typography.body.copy(fontWeight = FontWeight.Bold, fontSize = fontSize)
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar...",
    leadingIcon: @Composable (() -> Unit)? = { Icon(Icons.Rounded.Search, null, tint = PrimaryCyan) },
    trailingIcon: @Composable (() -> Unit)? = null,
    onSearch: () -> Unit = {}
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        imeAction = ImeAction.Search,
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}
