package com.example.personal

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.personal.download.DownloadManager
import com.example.personal.models.DownloadItem
import com.example.personal.models.DownloadStatus
import com.example.personal.ui.theme.*

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.activity.enableEdgeToEdge

class DownloadsActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // Permission granted
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        DownloadManager.init(this)
        requestStoragePermissions()
        
        setContent {
            MoviePanelTheme {
                DownloadsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
    
    private fun requestStoragePermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var downloads by remember { mutableStateOf(DownloadManager.getDownloads()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    
    // Set up callbacks
    LaunchedEffect(Unit) {
        DownloadManager.onDownloadProgress = { downloads = DownloadManager.getDownloads() }
        DownloadManager.onDownloadComplete = { downloads = DownloadManager.getDownloads() }
        DownloadManager.onDownloadFailed = { _, _ -> downloads = DownloadManager.getDownloads() }
    }
    
    // Refresh downloads list
    LaunchedEffect(refreshKey) {
        downloads = DownloadManager.getDownloads()
    }
    
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "My Downloads",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DarkBackground, DarkBackgroundEnd)
                    )
                )
                .navigationBarsPadding()
        ) {
            if (downloads.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No downloads yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Downloaded files will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(downloads, key = { it.id }) { download ->
                        DownloadCard(
                            download = download,
                            onOpen = {
                                openFile(context, download)
                            },
                            onRetry = {
                                DownloadManager.retryDownload(download.id)
                            },
                            onDelete = {
                                DownloadManager.deleteDownload(download.id)
                            },
                            onPause = {
                                DownloadManager.cancelDownload(download.id)
                            },
                            onResume = {
                                DownloadManager.retryDownload(download.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadCard(
    download: DownloadItem,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val statusColor = when (download.status) {
        DownloadStatus.COMPLETED -> ShieldActive
        DownloadStatus.DOWNLOADING -> PrimaryBlue
        DownloadStatus.FAILED -> SecondaryCoral
        DownloadStatus.PAUSED -> AccentGold
        DownloadStatus.PENDING -> TextTertiary
    }
    
    val rawProgress = if (download.fileSize > 0) {
        (download.downloadedSize.toFloat() / download.fileSize.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 400),
        label = "downloadProgress"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = download.status == DownloadStatus.COMPLETED) { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // File icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getFileIcon(download.mimeType, download.fileName),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // Decode URL-encoded filename for display
                        val displayName = remember(download.fileName) {
                            try {
                                if (download.fileName.contains('%') || download.fileName.contains('+')) {
                                    java.net.URLDecoder.decode(download.fileName, "UTF-8")
                                } else {
                                    download.fileName
                                }
                            } catch (e: Exception) {
                                download.fileName
                            }
                        }
                        
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = getStatusText(download),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor
                                )
                                
                                if (download.fileSize > 0) {
                                    Text(
                                        text = " • ${DownloadManager.formatFileSize(download.fileSize)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextTertiary
                                    )
                                }
                            }

                            if (download.status == DownloadStatus.DOWNLOADING && download.speedBytesPerSec > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatSpeedAndEta(download),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                )
                            }
                        }
                    }
                    
                    // Action buttons
                    Row {
                        when (download.status) {
                            DownloadStatus.DOWNLOADING -> {
                                IconButton(onClick = onPause) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pause",
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                            DownloadStatus.PAUSED -> {
                                IconButton(onClick = onResume) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                            DownloadStatus.COMPLETED -> {
                                IconButton(onClick = onOpen) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Open",
                                        tint = ShieldActive
                                    )
                                }
                            }
                            DownloadStatus.FAILED -> {
                                IconButton(onClick = onRetry) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                            DownloadStatus.PENDING -> {
                                // No extra action
                            }
                        }
                        
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = TextTertiary
                            )
                        }
                    }
                }
                
                // Progress bar for active downloads
                if (download.status == DownloadStatus.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PrimaryBlue,
                        trackColor = DarkSurface
                    )
                }
            }
        }
    }
}

private fun getFileIcon(mimeType: String, fileName: String): ImageVector {
    return when {
        mimeType.startsWith("video") || fileName.endsWith(".mp4") || fileName.endsWith(".mkv") -> 
            Icons.Outlined.Movie
        mimeType.startsWith("audio") || fileName.endsWith(".mp3") || fileName.endsWith(".m4a") -> 
            Icons.Outlined.MusicNote
        mimeType.startsWith("image") || fileName.endsWith(".jpg") || fileName.endsWith(".png") -> 
            Icons.Outlined.Image
        else -> Icons.Outlined.InsertDriveFile
    }
}

private fun getStatusText(download: DownloadItem): String {
    return when (download.status) {
        DownloadStatus.PENDING -> "Waiting..."
        DownloadStatus.DOWNLOADING -> {
            val downloaded = DownloadManager.formatFileSize(download.downloadedSize)
            if (download.fileSize > 0) {
                val percent = (download.downloadedSize * 100 / download.fileSize).toInt()
                "$downloaded ($percent%)"
            } else {
                downloaded
            }
        }
        DownloadStatus.COMPLETED -> "Completed"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.PAUSED -> {
            val downloaded = DownloadManager.formatFileSize(download.downloadedSize)
            if (download.fileSize > 0 && download.downloadedSize > 0) {
                val percent = (download.downloadedSize * 100 / download.fileSize).toInt()
                "Paused • $downloaded ($percent%)"
            } else {
                "Paused"
            }
        }
    }
}

private fun formatSpeedAndEta(download: DownloadItem): String {
    val speed = download.speedBytesPerSec
    if (speed <= 0) return ""

    val speedText = "${DownloadManager.formatFileSize(speed)}/s"

    val eta = download.etaSeconds
    val etaText = if (eta > 0) {
        val minutes = eta / 60
        val seconds = eta % 60
        when {
            minutes > 0 -> String.format(" • %d min %02d s left", minutes, seconds)
            else -> String.format(" • %d s left", seconds)
        }
    } else ""

    return "Speed $speedText$etaText"
}

private fun openFile(context: android.content.Context, download: DownloadItem) {
    try {
        val file = File(download.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val mimeType = when {
            download.mimeType.isNotBlank() -> download.mimeType
            download.fileName.endsWith(".mp4") -> "video/mp4"
            download.fileName.endsWith(".mkv") -> "video/x-matroska"
            download.fileName.endsWith(".mp3") -> "audio/mpeg"
            download.fileName.endsWith(".jpg") || download.fileName.endsWith(".jpeg") -> "image/jpeg"
            download.fileName.endsWith(".png") -> "image/png"
            else -> "*/*"
        }
        
        // If it's a video, use our internal advanced player
        if (mimeType.startsWith("video")) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra("download_id", download.id)
                putExtra("file_name", download.fileName)
            }
            context.startActivity(intent)
            return
        }
        
        // For other files, use external apps
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app to open this file", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
