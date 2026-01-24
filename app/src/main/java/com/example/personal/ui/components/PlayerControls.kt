package com.example.personal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personal.ui.theme.PrimaryBlue

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isPlaying: Boolean,
    title: String,
    duration: Long,
    currentTime: Long,
    bufferPercentage: Int,
    onReplayClick: () -> Unit,
    onForwardClick: () -> Unit,
    onPauseToggle: () -> Unit,
    onSeekChanged: (timeMs: Float) -> Unit,
    onSeekFinished: () -> Unit,
    onClose: () -> Unit,
    onPiP: () -> Unit,
    onRotate: () -> Unit,
    onSpeedClick: (Float) -> Unit,
    onTracksClick: () -> Unit,
    onSubtitleSizeChange: (Float) -> Unit,
    currentSpeed: Float = 1f
) {
    val durationFormatted = remember(duration) { formatTime(duration) }
    val currentTimeFormatted = remember(currentTime) { formatTime(currentTime) }

    var showMainMenu by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleSizeDialog by remember { mutableStateOf(false) }

    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    maxLines = 1
                )
                
                IconButton(onClick = onRotate) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Rotate Screen",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(onClick = { showMainMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMainMenu,
                        onDismissRequest = { showMainMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Playback Speed: ${currentSpeed}x") },
                            onClick = { 
                                showMainMenu = false
                                showSpeedDialog = true
                            },
                            leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Audio & Subtitles") },
                            onClick = { 
                                showMainMenu = false
                                onTracksClick()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Subtitles, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Subtitle Size") },
                            onClick = { 
                                showMainMenu = false
                                showSubtitleSizeDialog = true 
                            },
                            leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Picture-in-Picture") },
                            onClick = { 
                                showMainMenu = false
                                onPiP()
                            },
                            leadingIcon = { Icon(Icons.Default.PictureInPictureAlt, contentDescription = null) }
                        )
                    }
                }
            }
            
            if (showSpeedDialog) {
                AlertDialog(
                    onDismissRequest = { showSpeedDialog = false },
                    confirmButton = { TextButton(onClick = { showSpeedDialog = false }) { Text("Close") } },
                    title = { Text("Playback Speed") },
                    text = {
                        Column {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onSpeedClick(speed)
                                            showSpeedDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = speed == currentSpeed,
                                        onClick = { 
                                            onSpeedClick(speed)
                                            showSpeedDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${speed}x", fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                )
            }

            if (showSubtitleSizeDialog) {
                AlertDialog(
                    onDismissRequest = { showSubtitleSizeDialog = false },
                    confirmButton = { TextButton(onClick = { showSubtitleSizeDialog = false }) { Text("Close") } },
                    title = { Text("Subtitle Size") },
                    text = {
                        Column {
                             listOf(
                                 "Small" to 16f,
                                 "Normal" to 24f,
                                 "Large" to 32f,
                                 "Huge" to 48f
                             ).forEach { (label, size) ->
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .clickable { 
                                             onSubtitleSizeChange(size)
                                             showSubtitleSizeDialog = false 
                                         }
                                         .padding(vertical = 12.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text(label)
                                 }
                             }
                        }
                    }
                )
            }

            // Center Controls
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onReplayClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Replay 10s",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    onClick = onForwardClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = currentTimeFormatted, color = Color.White)
                    Text(text = durationFormatted, color = Color.White)
                }
                
                Slider(
                    value = currentTime.toFloat(),
                    onValueChange = onSeekChanged,
                    onValueChangeFinished = onSeekFinished,
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue,
                        inactiveTrackColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).toInt()
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
