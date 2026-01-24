package com.example.personal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personal.api.TeraPlayApi
import com.example.personal.api.TeraPlayFile
import com.example.personal.api.TeraPlayResponse
import com.example.personal.download.DownloadManager
import com.example.personal.ui.components.TeraBoxVideoDialog
import com.example.personal.ui.components.TeraBoxLoadingDialog
import com.example.personal.ui.components.TeraBoxErrorDialog
import com.example.personal.ui.components.TeraBoxFileListDialog
import com.example.personal.ui.theme.*
import kotlinx.coroutines.launch

class TeraBoxPlayerActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize DownloadManager
        DownloadManager.init(this)
        
        // Get URL from intent - support both explicit URL extra and deep link
        val teraBoxUrl = when {
            // Check for explicit URL extra (from app navigation)
            intent?.hasExtra("url") == true -> intent.getStringExtra("url")
            
            // Check for deep link (clicked TeraBox link from WhatsApp, browser, etc.)
            intent?.action == Intent.ACTION_VIEW && intent.data != null -> {
                intent.data.toString()
            }
            
            else -> null
        }
        
        val autoFetch = teraBoxUrl != null && TeraPlayApi.isTeraBoxUrl(teraBoxUrl)
        
        setContent {
            MoviePanelTheme {
                TeraBoxPlayerScreen(
                    initialUrl = teraBoxUrl ?: "",
                    autoFetch = autoFetch,
                    onBack = { finish() }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle new deep link while activity is running
        intent?.data?.let { uri ->
            // Restart activity with new URL
            val newIntent = Intent(this, TeraBoxPlayerActivity::class.java).apply {
                data = uri
                action = Intent.ACTION_VIEW
            }
            finish()
            startActivity(newIntent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeraBoxPlayerScreen(
    initialUrl: String = "",
    autoFetch: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    var urlInput by remember { mutableStateOf(initialUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var teraBoxResponse by remember { mutableStateOf<TeraPlayResponse?>(null) }
    var selectedFile by remember { mutableStateOf<TeraPlayFile?>(null) }
    
    // Auto-fetch if URL was passed
    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank() && TeraPlayApi.isTeraBoxUrl(initialUrl)) {
            isLoading = true
            val result = TeraPlayApi.fetchTeraBoxInfo(initialUrl)
            isLoading = false
            result.fold(
                onSuccess = { response ->
                    teraBoxResponse = response
                    val fileList = response.getFileList()
                    if (fileList.size == 1) {
                        selectedFile = fileList.first()
                    }
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Failed to fetch video info"
                }
            )
        }
    }
    
    fun fetchVideo() {
        if (urlInput.isBlank()) {
            Toast.makeText(context, "Please enter a TeraBox URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!TeraPlayApi.isTeraBoxUrl(urlInput)) {
            Toast.makeText(context, "Invalid TeraBox URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoading = true
        errorMessage = null
        
        scope.launch {
            val result = TeraPlayApi.fetchTeraBoxInfo(urlInput)
            isLoading = false
            result.fold(
                onSuccess = { response ->
                    teraBoxResponse = response
                    val fileList = response.getFileList()
                    if (fileList.size == 1) {
                        selectedFile = fileList.first()
                    }
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Failed to fetch video info"
                }
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A1A),
                        Color(0xFF0D1F3C),
                        Color(0xFF0A0A1A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "📦",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "TeraBox Player",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    CategoryHollywood.copy(alpha = 0.3f),
                                    AccentPurple.copy(alpha = 0.3f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = CategoryHollywood,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Stream TeraBox Videos",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    "Paste a TeraBox share link to stream or download",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // URL Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "https://teraboxapp.com/s/...",
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = CategoryHollywood
                                )
                            },
                            trailingIcon = {
                                if (urlInput.isNotBlank()) {
                                    IconButton(onClick = { urlInput = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CategoryHollywood,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = CategoryHollywood
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { fetchVideo() }
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Paste from clipboard button
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.getText()?.text?.let { clipText ->
                                        urlInput = clipText
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Paste")
                            }
                            
                            // Stream button
                            Button(
                                onClick = { fetchVideo() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CategoryHollywood,
                                    contentColor = Color.Black
                                ),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isLoading) "Loading..." else "Stream",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Supported domains info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = CategoryHollywood.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Supported Domains",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "teraboxapp.com • terabox.com • 1024terabox.com\nterasharefile.com • terasharelink.com • freeterabox.com\ndubox.com • 4funbox.com • and more...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Tips section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentPurple.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "💡 Tips",
                            color = AccentPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Copy a TeraBox share link from any app\n" +
                            "• Paste it here and tap Stream\n" +
                            "• Choose to stream directly or download\n" +
                            "• Links are temporary - fetch fresh if expired",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
        
        // Loading Dialog
        if (isLoading) {
            TeraBoxLoadingDialog(
                onDismiss = { isLoading = false }
            )
        }
        
        // File List Dialog (when multiple files)
        teraBoxResponse?.let { response ->
            val fileList = response.getFileList()
            if (fileList.size > 1 && selectedFile == null) {
                TeraBoxFileListDialog(
                    files = fileList,
                    onDismiss = {
                        teraBoxResponse = null
                    },
                    onFileSelected = { file ->
                        selectedFile = file
                    }
                )
            }
        }
        
        // Video Dialog
        selectedFile?.let { file ->
            TeraBoxVideoDialog(
                file = file,
                onDismiss = {
                    selectedFile = null
                    teraBoxResponse = null
                },
                onStream = { streamFile ->
                    val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                        putExtra("stream_url", streamFile.getStreamableUrl())
                        putExtra("file_name", streamFile.name)
                    }
                    context.startActivity(intent)
                    selectedFile = null
                    teraBoxResponse = null
                },
                onDownload = { downloadFile ->
                    val downloadUrl = downloadFile.getDownloadUrl()
                    if (downloadUrl.isBlank()) {
                        Toast.makeText(context, "Download URL not available", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            // Ensure DownloadManager is initialized
                            if (!DownloadManager.isInitialized) {
                                DownloadManager.init(context)
                            }
                            DownloadManager.startDownload(
                                url = downloadUrl,
                                fileName = downloadFile.name,
                                mimeType = "video/mp4"
                            )
                            Toast.makeText(context, "Download started: ${downloadFile.name}", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    selectedFile = null
                    teraBoxResponse = null
                }
            )
        }
        
        // Error Dialog
        errorMessage?.let { error ->
            TeraBoxErrorDialog(
                errorMessage = error,
                onDismiss = { errorMessage = null },
                onRetry = {
                    errorMessage = null
                    fetchVideo()
                },
                onOpenInBrowser = {
                    errorMessage = null
                }
            )
        }
    }
}
