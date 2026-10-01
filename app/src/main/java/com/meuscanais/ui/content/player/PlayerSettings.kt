package com.meuscanais.ui.content.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Tracks
import com.meuscanais.R
import com.meuscanais.core.ui.components.buttons.AppIconButton
import com.meuscanais.core.ui.theme.*
import com.meuscanais.core.ui.components.common.adaptiveFocus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsDialog(
    tracks: Tracks?,
    sleepTimer: Int?,
    subtitleSize: Float,
    onDismiss: () -> Unit,
    onSelectTrack: (Int, Int, Int) -> Unit,
    onClearOverride: (Int) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onSetSubtitleSize: (Float) -> Unit
) {
    val tokens = AppDesignSystem
    var selectedTab by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.colors.backgroundSecondary.copy(alpha = 0.98f),
        tonalElevation = 8.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .heightIn(min = 400.dp)
        ) {
            // Sidebar Navigation
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.05f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraLarge)
            ) {
                Spacer(Modifier.height(tokens.spacing.extraLarge))
                SettingsTabItem(Icons.Rounded.InterpreterMode, selectedTab == 0) { selectedTab = 0 }
                SettingsTabItem(Icons.Rounded.ClosedCaption, selectedTab == 1) { selectedTab = 1 }
                SettingsTabItem(Icons.Rounded.VideoSettings, selectedTab == 2) { selectedTab = 2 }
                SettingsTabItem(Icons.Rounded.Settings, selectedTab == 3) { selectedTab = 3 }
            }

            // Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(tokens.spacing.extraLarge)
                    .verticalScroll(rememberScrollState())
            ) {
                val title = when(selectedTab) {
                    0 -> stringResource(R.string.audio_label)
                    1 -> stringResource(R.string.subtitles_label)
                    2 -> stringResource(R.string.video_label)
                    else -> stringResource(R.string.system_label)
                }
                
                Text(
                    text = title.uppercase(), 
                    style = tokens.typography.headline, 
                    fontWeight = FontWeight.Black, 
                    color = tokens.colors.textPrimary,
                    letterSpacing = 2.sp
                )
                
                Spacer(Modifier.height(tokens.spacing.extraLarge))
                
                when (selectedTab) {
                    0 -> { // Audio
                        if (tracks == null) NoOptionsPlaceholder()
                        else TrackTypeSection(stringResource(R.string.languages_section), tracks, C.TRACK_TYPE_AUDIO, onSelectTrack, onClearOverride)
                    }
                    1 -> { // Subtitles
                        if (tracks == null) NoOptionsPlaceholder()
                        else TrackTypeSection(stringResource(R.string.available_subtitles), tracks, C.TRACK_TYPE_TEXT, onSelectTrack, onClearOverride)

                        Spacer(Modifier.height(tokens.spacing.huge))
                        Text(
                            text = stringResource(R.string.font_size_label).uppercase(), 
                            style = tokens.typography.title, 
                            color = tokens.colors.primary, 
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier.padding(top = tokens.spacing.medium), 
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium)
                        ) {
                            listOf(0.8f to stringResource(R.string.subtitle_small), 1.0f to stringResource(R.string.subtitle_medium), 1.3f to stringResource(R.string.subtitle_large)).forEach { (size, label) ->
                                SubtitleSizeChip(selected = subtitleSize == size, label = label) { onSetSubtitleSize(size) }
                            }
                        }
                    }
                    2 -> { // Video
                        if (tracks == null) NoOptionsPlaceholder()
                        else TrackTypeSection(stringResource(R.string.video_quality_section), tracks, C.TRACK_TYPE_VIDEO, onSelectTrack, onClearOverride)
                    }
                    3 -> { // System
                        Text(
                            text = stringResource(R.string.sleep_timer_title).uppercase(), 
                            style = tokens.typography.title, 
                            color = tokens.colors.primary, 
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier
                                .padding(top = tokens.spacing.medium)
                                .horizontalScroll(rememberScrollState()), 
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium)
                        ) {
                            listOf(null to stringResource(R.string.off_label), 15 to "15m", 30 to "30m", 60 to "60m", 120 to "120m").forEach { (time, label) ->
                                SubtitleSizeChip(selected = sleepTimer == time, label = label) { onSetSleepTimer(time) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val tokens = AppDesignSystem
    Box(
        modifier = Modifier
            .size(56.dp)
            .adaptiveFocus(shape = tokens.shapes.medium)
            .background(if (isSelected) tokens.colors.primary else Color.Transparent, tokens.shapes.medium)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = if (isSelected) Color.Black else tokens.colors.textSecondary.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun NoOptionsPlaceholder() {
    val tokens = AppDesignSystem
    Text(
        text = stringResource(R.string.no_options_label), 
        color = tokens.colors.textSecondary.copy(alpha = 0.4f), 
        style = tokens.typography.body
    )
}

@Composable
fun TrackTypeSection(title: String, tracks: Tracks, trackType: Int, onSelectTrack: (Int, Int, Int) -> Unit, onClearOverride: (Int) -> Unit) {
    val tokens = AppDesignSystem
    val groups = tracks.groups.filter { it.type == trackType }
    if (groups.isNotEmpty()) {
        Text(
            text = title.uppercase(), 
            style = tokens.typography.title, 
            color = tokens.colors.primary, 
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(tokens.spacing.large))
        
        val isAnySelected = groups.any { it.isSelected }

        TrackItemRow(
            label = stringResource(R.string.auto_label).uppercase(),
            isSelected = !isAnySelected,
            onClick = { onClearOverride(trackType) }
        )

        groups.forEachIndexed { groupIndex, group ->
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val isSelected = group.isTrackSelected(i)
                val label = buildString {
                    if (trackType == C.TRACK_TYPE_VIDEO) { append("${format.height}p") }
                    else {
                        append(format.language?.uppercase() ?: stringResource(R.string.unknown).uppercase())
                        if (format.label != null) append(" (${format.label})")
                    }
                }

                TrackItemRow(
                    label = label,
                    isSelected = isSelected,
                    onClick = { onSelectTrack(groupIndex, i, trackType) }
                )
            }
        }
    }
}

@Composable
fun TrackItemRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val tokens = AppDesignSystem
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (isFocused) tokens.colors.primary else if (isSelected) tokens.colors.primary.copy(alpha = 0.15f) else Color.Transparent)
    val contentColor by animateColorAsState(if (isFocused) Color.Black else tokens.colors.textPrimary)

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = tokens.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = tokens.spacing.tiny)
            .adaptiveFocus(shape = tokens.shapes.medium, onFocus = { isFocused = it })
    ) {
        Row(modifier = Modifier.padding(tokens.spacing.large), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, 
                contentDescription = null, 
                tint = if (isFocused) Color.Black else if (isSelected) tokens.colors.primary else tokens.colors.textSecondary.copy(alpha = 0.3f)
            )
            Spacer(Modifier.width(tokens.spacing.extraLarge))
            Text(
                text = label, 
                color = contentColor, 
                style = tokens.typography.title, 
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
            )
        }
    }
}

@Composable
fun SubtitleSizeChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val tokens = AppDesignSystem
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (isFocused) tokens.colors.primary else if (selected) tokens.colors.primary.copy(alpha = 0.2f) else tokens.colors.surface.copy(alpha = 0.1f))
    val contentColor by animateColorAsState(if (isFocused) Color.Black else tokens.colors.textPrimary)

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = tokens.shapes.medium,
        modifier = Modifier.adaptiveFocus(shape = tokens.shapes.medium, onFocus = { isFocused = it })
    ) {
        Text(
            text = label.uppercase(), 
            modifier = Modifier.padding(horizontal = tokens.spacing.large, vertical = tokens.spacing.medium),
            style = tokens.typography.label,
            fontWeight = FontWeight.Black,
            color = contentColor
        )
    }
}
