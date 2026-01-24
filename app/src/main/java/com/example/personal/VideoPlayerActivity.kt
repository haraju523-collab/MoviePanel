package com.example.personal

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import android.util.TypedValue
import androidx.media3.ui.CaptionStyleCompat
import com.example.personal.download.DownloadManager
import com.example.personal.models.DownloadStatus
import com.example.personal.ui.components.PlayerControls
import com.example.personal.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

class VideoPlayerActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val downloadId = intent.getStringExtra("download_id")
        val streamUrl = intent.getStringExtra("stream_url")
        val fileName = intent.getStringExtra("file_name") ?: "Video"
        
        // Must have either download_id or stream_url
        if (downloadId == null && streamUrl == null) {
            finish()
            return
        }
        
        setContent {
            MoviePanelTheme {
                VideoPlayerScreen(
                    downloadId = downloadId,
                    streamUrl = streamUrl,
                    fileName = fileName,
                    onClose = { finish() },
                    onPiP = { 
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            enterPictureInPictureMode(params)
                        }
                    },
                    onToggleRotation = {
                        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    downloadId: String? = null,
    streamUrl: String? = null,
    fileName: String,
    onClose: () -> Unit,
    onPiP: () -> Unit,
    onToggleRotation: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAutoRotated by remember { mutableStateOf(false) }
    
    // State
    var downloadItem by remember { mutableStateOf(downloadId?.let { DownloadManager.getDownloadById(it) }) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentTime by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferPercentage by remember { mutableIntStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var currentSpeed by remember { mutableFloatStateOf(1f) }
    var showTrackSelection by remember { mutableStateOf(false) }
    var subtitleSize by remember { mutableFloatStateOf(16f) } // Default 16sp
    var playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
    
    // Player
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }
    
    // Setup Player with stream URL or download file
    LaunchedEffect(streamUrl, downloadItem) {
        // Priority 1: Stream URL (direct URL from TeraBox or other sources)
        if (streamUrl != null) {
            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            return@LaunchedEffect
        }
        
        // Priority 2: Downloaded/Downloading file
        val item = downloadItem
        if (item?.status == DownloadStatus.COMPLETED) {
            val file = File(item.filePath)
            if (file.exists()) {
                val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }
        } else if (item?.status == DownloadStatus.DOWNLOADING) {
             // For streaming while downloading, we would need a proper proxy server or file input stream 
             // support in ExoPlayer which is complex. For now basic support for completed files.
             // If file path exists partially we could try to play it
             val file = File(item.filePath)
            if (file.exists()) {
                 val mediaItemBuilder = MediaItem.Builder()
                     .setUri(Uri.fromFile(file))
                 
                 // Scan for subtitles in the same directory
                 val parentDir = file.parentFile
                 if (parentDir != null && parentDir.exists()) {
                     val baseName = file.nameWithoutExtension
                     parentDir.listFiles { _, name -> 
                         name.startsWith(baseName) && (name.endsWith(".srt") || name.endsWith(".vtt"))
                     }?.forEach { subtitleFile ->
                         val subtitleUri = Uri.fromFile(subtitleFile)
                         val mimeType = if (subtitleFile.extension == "srt") MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_VTT
                         val language = subtitleFile.nameWithoutExtension.substringAfterLast(".", "en") // approximate lang
                         
                         val subtitle = SubtitleConfiguration.Builder(subtitleUri)
                             .setMimeType(mimeType)
                             .setLanguage(language)
                             .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                             .build()
                         mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
                     }
                 }
                 
                 exoPlayer.setMediaItem(mediaItemBuilder.build())
                 exoPlayer.prepare()
            }
        }
    }
    
    // Progress Loop
    LaunchedEffect(Unit) {
        while(true) {
            delay(500)
            currentTime = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            bufferPercentage = exoPlayer.bufferedPercentage
            isPlaying = exoPlayer.isPlaying
            
            // Auto hide controls after 3 seconds
            if (isPlaying && isControlsVisible) {
                // Logic handled by separate effect or manual toggle usually better for custom UI
            }
            
            // Auto-rotate based on video dimensions (only once)
            if (!hasAutoRotated && exoPlayer.videoFormat != null) {
                 val format = exoPlayer.videoFormat
                 if (format != null && format.width > format.height) {
                     // Horizontal video -> Force Landscape
                     onToggleRotation()
                     hasAutoRotated = true
                 } else if (format != null) {
                     // Vertical or square -> mark as handled
                     hasAutoRotated = true
                 }
            }
        }
    }

    // Lifecycle Management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isControlsVisible = !isControlsVisible },
                    onDoubleTap = { offset ->
                         if (offset.x < size.width / 2) {
                             // Rewind
                             exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L))
                         } else {
                             // Forward
                             exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                         }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Use our custom controls
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    playerViewRef.value = this
                    subtitleView?.setStyle(
                        CaptionStyleCompat(
                            Color.White.toArgb(),
                            Color.Black.copy(alpha = 0.5f).toArgb(),
                            Color.Transparent.toArgb(),
                            CaptionStyleCompat.EDGE_TYPE_NONE,
                            Color.Black.toArgb(),
                            null
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        PlayerControls(
            modifier = Modifier.fillMaxSize(),
            isVisible = isControlsVisible,
            isPlaying = isPlaying,
            title = fileName,
            duration = duration,
            currentTime = currentTime,
            bufferPercentage = bufferPercentage,
            onReplayClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
            onForwardClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration)) },
            onPauseToggle = {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            onSeekChanged = { timeMs -> exoPlayer.seekTo(timeMs.toLong()) },
            onSeekFinished = { exoPlayer.play() },
            onClose = onClose,
            onPiP = onPiP,
            onRotate = onToggleRotation,
            onSpeedClick = { speed ->
                currentSpeed = speed
                exoPlayer.setPlaybackSpeed(speed)
            },
            onTracksClick = {
                showTrackSelection = true
            },
            onSubtitleSizeChange = { sizeSp ->
                subtitleSize = sizeSp
                playerViewRef.value?.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            },
            currentSpeed = currentSpeed
        )
        
        if (showTrackSelection) {
            TrackSelectionDialog(
                tracks = exoPlayer.currentTracks,
                onDismiss = { showTrackSelection = false },
                onTrackSelected = { trackType, groupIndex, trackIndex ->
                    val builder = exoPlayer.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(trackType, false)
                        .setOverrideForType(
                            androidx.media3.common.TrackSelectionOverride(
                                exoPlayer.currentTracks.groups[groupIndex].mediaTrackGroup,
                                trackIndex
                            )
                        )
                    exoPlayer.trackSelectionParameters = builder.build()
                    showTrackSelection = false
                }
            )
        }
    }
}

@Composable
fun TrackSelectionDialog(
    tracks: Tracks,
    onDismiss: () -> Unit,
    onTrackSelected: (trackType: Int, groupIndex: Int, trackIndex: Int) -> Unit
) {
    if (tracks.groups.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
            title = { Text("No Tracks Available") },
            text = { Text("This video does not have alternate audio or subtitle tracks.") }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Select Audio/Subtitles") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                tracks.groups.forEachIndexed { groupIndex, group ->
                    val trackGroup = group.mediaTrackGroup
                    val trackType = trackGroup.type
                    
                    if (trackType == C.TRACK_TYPE_AUDIO || trackType == C.TRACK_TYPE_TEXT) {
                        Text(
                            text = if (trackType == C.TRACK_TYPE_AUDIO) "Audio" else "Subtitles",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        for (i in 0 until trackGroup.length) {
                             val isSelected = group.isSelected
                             // Note: Exact selection check is complex, simplistic check here
                             val format = trackGroup.getFormat(i)
                             val languageCode = format.language
                             val languageName = if (languageCode != null) {
                                 java.util.Locale(languageCode).displayLanguage.replaceFirstChar { 
                                     if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
                                 }
                             } else "Unknown"
                             
                             val label = format.label ?: languageName
                             
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .clickable { onTrackSelected(trackType, groupIndex, i) }
                                     .padding(vertical = 8.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 RadioButton(
                                     selected = isSelected && group.isTrackSelected(i),
                                     onClick = { onTrackSelected(trackType, groupIndex, i) }
                                 )
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text(text = label)
                             }
                        }
                    }
                }
            }
        }
    )
}
