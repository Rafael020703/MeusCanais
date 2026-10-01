package com.meuscanais.core.ui.components.common

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.meuscanais.core.ui.theme.AppDesignSystem
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DigitalClock(
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    format: String = "HH:mm:ss a"
) {
    val tokens = AppDesignSystem
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    val timeFormatter = remember(format) { SimpleDateFormat(format, Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(1000)
        }
    }

    Text(
        text = timeFormatter.format(currentTime).uppercase(),
        style = textStyle ?: tokens.typography.title,
        fontWeight = FontWeight.Black,
        color = tokens.colors.textPrimary,
        modifier = modifier
    )
}

@Composable
fun DigitalDate(
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    format: String = "EEEE, d MMM yyyy"
) {
    val tokens = AppDesignSystem
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    val dateFormatter = remember(format) { SimpleDateFormat(format, Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(60000)
        }
    }

    Text(
        text = dateFormatter.format(currentTime).uppercase(),
        style = textStyle ?: tokens.typography.body,
        color = tokens.colors.textSecondary,
        modifier = modifier,
        fontWeight = FontWeight.Bold
    )
}
