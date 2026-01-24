package com.example.personal

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.activity.enableEdgeToEdge
import com.example.personal.api.TeraPlayApi
import com.example.personal.api.TeraPlayFile
import com.example.personal.api.TeraPlayResponse
import com.example.personal.download.DownloadManager
import com.example.personal.ui.components.TeraBoxVideoDialog
import com.example.personal.ui.components.TeraBoxLoadingDialog
import com.example.personal.ui.components.TeraBoxErrorDialog
import com.example.personal.ui.components.TeraBoxFileListDialog
import com.example.personal.ui.theme.*
import com.example.personal.utils.AdBlocker
import com.example.personal.utils.AdBlockingWebChromeClient
import com.example.personal.utils.MyWebViewClient
import com.example.personal.ui.components.BrowserStartPage
import kotlinx.coroutines.launch


class WebViewActivity : ComponentActivity() {
    
    private var webView: WebView? = null
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize download manager
        DownloadManager.init(this)
        
        // Initialize AdBlocker with comprehensive rules
        AdBlocker.loadAdBlockRules(this)
        
        // Get URL - empty string means start with blank search page
        val url = intent.getStringExtra("url") ?: ""
        val title = intent.getStringExtra("title") ?: "Private Browser"
        val incognitoMode = intent.getBooleanExtra("incognito", false)
        
        setContent {
            MoviePanelTheme {
                WebViewScreen(
                    initialUrl = url,
                    initialTitle = title,
                    incognitoMode = incognitoMode,
                    onWebViewCreated = { webView = it },
                    onClose = { finish() },
                    onOpenDownloads = {
                        startActivity(Intent(this, DownloadsActivity::class.java))
                    }
                )
            }
        }
    }
    
    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        // Clear data on exit if in incognito mode
        if (intent.getBooleanExtra("incognito", false)) {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.WebStorage.getInstance().deleteAllData()
        }
        webView?.destroy()
        super.onDestroy()
    }
}

data class PendingDownload(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val contentLength: Long
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    initialUrl: String,
    initialTitle: String,
    incognitoMode: Boolean = false,
    onWebViewCreated: (WebView) -> Unit,
    onClose: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Start with blank page if no URL provided
    val hasInitialUrl = initialUrl.isNotBlank()
    var currentUrl by remember { mutableStateOf(if (hasInitialUrl) initialUrl else "about:blank") }
    var pageTitle by remember { mutableStateOf(if (hasInitialUrl) initialTitle else if (incognitoMode) "Private Browser" else "New Tab") }
    var isLoading by remember { mutableStateOf(hasInitialUrl) }
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var blockedCount by remember { mutableIntStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var downloadStarted by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    
    // TeraBox integration states
    var teraBoxLoading by remember { mutableStateOf(false) }
    var teraBoxError by remember { mutableStateOf<String?>(null) }
    var teraBoxPendingUrl by remember { mutableStateOf<String?>(null) }
    var teraBoxResponse by remember { mutableStateOf<TeraPlayResponse?>(null) }
    var selectedTeraBoxFile by remember { mutableStateOf<TeraPlayFile?>(null) }
    
    // Show toast when download starts
    LaunchedEffect(downloadStarted) {
        if (downloadStarted) {
            Toast.makeText(context, "Download started!", Toast.LENGTH_SHORT).show()
            downloadStarted = false
        }
    }
    
    // Handle back press
    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }
    
    // Brave-style browser state
    var showUrlBar by remember { mutableStateOf(!hasInitialUrl) }
    var urlFieldText by remember { mutableStateOf(TextFieldValue(if (hasInitialUrl) initialUrl else "")) }
    var showMenu by remember { mutableStateOf(false) }
    var desktopMode by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    
    // Update URL field when page changes
    LaunchedEffect(currentUrl) {
        if (!showUrlBar) {
            urlFieldText = TextFieldValue(currentUrl)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Minimal Top Bar - just SSL lock, title, and shield
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (incognitoMode) Color(0xFF1A1A2E) else DarkSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SSL Lock or Incognito indicator
                Icon(
                    imageVector = if (incognitoMode) Icons.Default.PrivacyTip else Icons.Default.Lock,
                    contentDescription = if (incognitoMode) "Incognito" else "Secure",
                    tint = if (incognitoMode) Color(0xFFBB86FC) else ShieldActive,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Page title (clickable to show URL bar)
                Text(
                    text = pageTitle.ifEmpty { if (incognitoMode) "Private Browser" else "New Tab" },
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showUrlBar = true }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Shield stats badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ShieldActive.copy(alpha = 0.15f))
                        .clickable { 
                            Toast.makeText(context, "Blocked $blockedCount ads & trackers", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Protected",
                        tint = ShieldActive,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$blockedCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShieldActive,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Loading progress bar (thin line under top bar)
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = PrimaryBlue,
                    trackColor = DarkCard
                )
            }
            
            // WebView Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            webViewClient = object : MyWebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let { currentUrl = it }
                                }
                                
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                    view?.title?.let { if (it.isNotBlank()) pageTitle = it }
                                    
                                    // Inject popup killer JS (omitted for brevity, same as before)
                                    val jsCode = """
                                        (function() {
                                            setInterval(() => {
                                                document.querySelectorAll('iframe[src*="ad"], .ads, [id*="ad"], [class*="popup"], [class*="overlay"]').forEach(e => e.remove());
                                            }, 1000);
                                            window.open = () => null;
                                            window.alert = () => {};
                                            window.confirm = () => true;
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(jsCode, null)
                                }
                                
                                override fun onBlockedRequest() {
                                    blockedCount++
                                }
                                
                                override fun onTeraBoxUrlDetected(url: String) {
                                    teraBoxPendingUrl = url
                                    teraBoxLoading = true
                                    teraBoxError = null
                                    scope.launch {
                                        val result = TeraPlayApi.fetchTeraBoxInfo(url)
                                        teraBoxLoading = false
                                        result.fold(
                                            onSuccess = { response ->
                                                teraBoxResponse = response
                                                val fileList = response.getFileList()
                                                if (fileList.size == 1) selectedTeraBoxFile = fileList.first()
                                            },
                                            onFailure = { error ->
                                                teraBoxError = error.message ?: "Failed to fetch video info"
                                            }
                                        )
                                    }
                                }
                            }
                            
                            webChromeClient = object : AdBlockingWebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress / 100f
                                }
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    title?.let { if (it.isNotBlank()) pageTitle = it }
                                }
                            }
                            
                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                val fileName = extractFileName(contentDisposition, url)
                                pendingDownload = PendingDownload(url, fileName, mimetype ?: "", contentLength)
                            }
                            
                            settings.apply {
                                javaScriptEnabled = true
                                javaScriptCanOpenWindowsAutomatically = false
                                setSupportMultipleWindows(false)
                                allowFileAccess = false
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                
                                if (incognitoMode) {
                                    cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                                    setSaveFormData(false)
                                } else {
                                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                }
                            }
                            
                            if (incognitoMode) {
                                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                                android.webkit.WebStorage.getInstance().deleteAllData()
                                clearCache(true)
                                clearHistory()
                                clearFormData()
                            }
                            
                            if (hasInitialUrl) {
                                loadUrl(initialUrl)
                            }
                            webViewInstance = this
                            onWebViewCreated(this)
                        }
                    },
                    update = { webView ->
                        webView.settings.userAgentString = if (desktopMode) {
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        } else {
                            null
                        }
                    }
                )
                
                // Overlay Start Page if URL is blank
                if (currentUrl == "about:blank" || currentUrl.isBlank()) {
                    BrowserStartPage(
                        onNavigate = { url ->
                            webViewInstance?.loadUrl(url)
                            currentUrl = url
                            showUrlBar = false
                        }
                    )
                }
            }
        }
        
        // Floating Bottom Bar (Brave-style)
        BraveStyleBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            currentUrl = currentUrl,
            blockedCount = blockedCount,
            canGoBack = canGoBack,
            isLoading = isLoading,
            showUrlBar = showUrlBar,
            urlFieldText = urlFieldText,
            onUrlFieldChange = { urlFieldText = it },
            onUrlBarToggle = { showUrlBar = it },
            onNavigate = { url ->
                val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else if (url.contains(".") && !url.contains(" ")) {
                    "https://$url"
                } else {
                    "https://www.google.com/search?q=${java.net.URLEncoder.encode(url, "UTF-8")}"
                }
                webViewInstance?.loadUrl(finalUrl)
                showUrlBar = false
                focusManager.clearFocus()
            },
            onBack = { webViewInstance?.goBack() },
            onRefresh = { 
                if (isLoading) webViewInstance?.stopLoading()
                else webViewInstance?.reload()
            },
            onHome = onClose,
            onDownloads = onOpenDownloads,
            desktopMode = desktopMode,
            onDesktopModeToggle = { 
                desktopMode = it
                webViewInstance?.reload()
            },
            incognitoMode = incognitoMode,
            onIncognitoToggle = { shouldBeIncognito ->
                // Restart activity with new mode
                val intent = Intent(context, WebViewActivity::class.java).apply {
                    putExtra("url", "") // Start blank
                    putExtra("title", if (shouldBeIncognito) "Private Browser" else "New Tab")
                    putExtra("incognito", shouldBeIncognito)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                onClose() // Close current activity
            },
            focusRequester = focusRequester
        )
    }

    // Download confirmation dialog
    pendingDownload?.let { pending ->
        DownloadConfirmDialog(
            pending = pending,
            onDismiss = { pendingDownload = null },
            onDownloadOnly = { customName, subdirectory ->
                val finalName = customName.ifBlank { pending.fileName }
                DownloadManager.startDownload(
                    url = pending.url,
                    fileName = finalName,
                    mimeType = pending.mimeType,
                    subdirectory = subdirectory.ifBlank { null }
                )
                downloadStarted = true
                pendingDownload = null
            },
            onStreamAndDownload = { customName ->
                val finalName = customName.ifBlank { pending.fileName }
                val streamItem = DownloadManager.startStreamingDownload(
                    url = pending.url,
                    fileName = finalName,
                    mimeType = pending.mimeType
                )
                // Open video player to stream while downloading
                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtra("download_id", streamItem.id)
                    putExtra("file_name", streamItem.fileName)
                }
                context.startActivity(intent)
                downloadStarted = true
                pendingDownload = null
            }
        )
    }
    
    // TeraBox Loading Dialog
    if (teraBoxLoading) {
        TeraBoxLoadingDialog(
            onDismiss = {
                teraBoxLoading = false
                teraBoxPendingUrl = null
            }
        )
    }
    
    // TeraBox File List Dialog (when multiple files)
    teraBoxResponse?.let { response ->
        val fileList = response.getFileList()
        if (fileList.size > 1 && selectedTeraBoxFile == null) {
            TeraBoxFileListDialog(
                files = fileList,
                onDismiss = {
                    teraBoxResponse = null
                    teraBoxPendingUrl = null
                },
                onFileSelected = { file ->
                    selectedTeraBoxFile = file
                }
            )
        }
    }
    
    // TeraBox Video Dialog (show file details)
    selectedTeraBoxFile?.let { file ->
        TeraBoxVideoDialog(
            file = file,
            onDismiss = {
                selectedTeraBoxFile = null
                teraBoxResponse = null
                teraBoxPendingUrl = null
            },
            onStream = { selectedFile ->
                // Launch video player with stream URL
                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtra("stream_url", selectedFile.getStreamableUrl())
                    putExtra("file_name", selectedFile.name)
                }
                context.startActivity(intent)
                selectedTeraBoxFile = null
                teraBoxResponse = null
                teraBoxPendingUrl = null
            },
            onDownload = { selectedFile ->
                // Start download via DownloadManager
                val downloadUrl = selectedFile.getDownloadUrl()
                if (downloadUrl.isBlank()) {
                    Toast.makeText(context, "Download URL not available", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        // DownloadManager should already be initialized in onCreate
                        DownloadManager.startDownload(
                            url = downloadUrl,
                            fileName = selectedFile.name,
                            mimeType = "video/mp4"
                        )
                        Toast.makeText(context, "Download started: ${selectedFile.name}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                selectedTeraBoxFile = null
                teraBoxResponse = null
                teraBoxPendingUrl = null
            }
        )
    }
    
    // TeraBox Error Dialog
    teraBoxError?.let { error ->
        TeraBoxErrorDialog(
            errorMessage = error,
            onDismiss = {
                teraBoxError = null
                teraBoxPendingUrl = null
            },
            onRetry = {
                teraBoxError = null
                teraBoxPendingUrl?.let { url ->
                    teraBoxLoading = true
                    scope.launch {
                        val result = TeraPlayApi.fetchTeraBoxInfo(url)
                        teraBoxLoading = false
                        result.fold(
                            onSuccess = { response ->
                                teraBoxResponse = response
                                val fileList = response.getFileList()
                                if (fileList.size == 1) {
                                    selectedTeraBoxFile = fileList.first()
                                }
                            },
                            onFailure = { err ->
                                teraBoxError = err.message ?: "Failed to fetch video info"
                            }
                        )
                    }
                }
            },
            onOpenInBrowser = {
                // Allow WebView to navigate to the URL
                teraBoxPendingUrl?.let { url ->
                    webViewInstance?.loadUrl(url)
                }
                teraBoxError = null
                teraBoxPendingUrl = null
            }
        )
    }
}

private fun extractFileName(contentDisposition: String?, url: String): String {
    var fileName: String? = null
    
    // Try to get filename from content disposition
    if (!contentDisposition.isNullOrBlank()) {
        val pattern = Regex("filename[*]?=['\"]?(?:UTF-\\d['\"]*)?([^'\"\\s;]+)")
        pattern.find(contentDisposition)?.let { match ->
            fileName = match.groupValues[1]
        }
        
        // Alternative pattern
        if (fileName == null) {
            val altPattern = Regex("filename=\"([^\"]+)\"")
            altPattern.find(contentDisposition)?.let { match ->
                fileName = match.groupValues[1]
            }
        }
    }
    
    // Extract from URL if not found in content disposition
    if (fileName.isNullOrBlank()) {
        try {
            val path = java.net.URL(url).path
            fileName = path.substringAfterLast('/')
            if (fileName.isBlank() || !fileName.contains('.')) {
                fileName = null
            } else {
                fileName = fileName.take(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Decode URL-encoded characters
    if (!fileName.isNullOrBlank()) {
        try {
            // Decode URL encoding (%20, %28, %29, etc.)
            fileName = java.net.URLDecoder.decode(fileName, "UTF-8")
        } catch (e: Exception) {
            // If decoding fails, use original
            e.printStackTrace()
        }
    }
    
    return fileName?.take(200) ?: "download_${System.currentTimeMillis()}.mp4"
}

@Composable
private fun DownloadConfirmDialog(
    pending: PendingDownload,
    onDismiss: () -> Unit,
    onDownloadOnly: (fileName: String, subdirectory: String) -> Unit,
    onStreamAndDownload: (fileName: String) -> Unit
) {
    // Decode filename if it's URL-encoded
    val decodedFileName = remember(pending.fileName) {
        try {
            if (pending.fileName.contains('%') || pending.fileName.contains('+')) {
                java.net.URLDecoder.decode(pending.fileName, "UTF-8")
            } else {
                pending.fileName
            }
        } catch (e: Exception) {
            pending.fileName
        }
    }
    
    var nameState by remember { mutableStateOf(TextFieldValue(decodedFileName)) }
    var folderState by remember { mutableStateOf(TextFieldValue("")) }

    val prettySize = remember(pending.contentLength) {
        if (pending.contentLength > 0) {
            DownloadManager.formatFileSize(pending.contentLength)
        } else {
            "Unknown size"
        }
    }

    val isVideo = remember(pending.mimeType, nameState.text) {
        pending.mimeType.startsWith("video/") || 
        nameState.text.contains(Regex("\\.(mp4|mkv|avi|mov|webm|flv|3gp|m4v)$", RegexOption.IGNORE_CASE))
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isVideo) {
                    Button(
                        onClick = { onStreamAndDownload(nameState.text.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        )
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stream & Download")
                    }
                }
                OutlinedButton(
                    onClick = { onDownloadOnly(nameState.text.trim(), folderState.text.trim()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Only")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { 
            Text(
                text = if (isVideo) "Stream or Download?" else "Download file?", 
                color = TextPrimary
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "File: ${nameState.text}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "Size: $prettySize",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                
                if (isVideo) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Stream & Download: Watch while downloading to cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isVideo) {
                    OutlinedTextField(
                        value = folderState,
                        onValueChange = { folderState = it },
                        label = { Text("Save in folder (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                text = "Inside MoviePanel downloads",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun BottomToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onDownloads: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurface)
                )
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            enabled = canGoBack,
            onClick = onBack
        )
        
        ToolbarButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Forward",
            enabled = canGoForward,
            onClick = onForward
        )
        
        ToolbarButton(
            icon = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
            contentDescription = if (isLoading) "Stop" else "Refresh",
            enabled = true,
            onClick = onRefresh
        )
        
        ToolbarButton(
            icon = Icons.Outlined.Download,
            contentDescription = "Downloads",
            enabled = true,
            onClick = onDownloads,
            tint = PrimaryBlue
        )
        
        ToolbarButton(
            icon = Icons.Default.Home,
            contentDescription = "Home",
            enabled = true,
            onClick = onHome
        )
    }
}

@Composable
fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color? = null
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) DarkCard else DarkCard.copy(alpha = 0.5f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: (if (enabled) TextPrimary else TextTertiary),
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Brave-style floating bottom bar with URL field and controls
 */
@Composable
fun BraveStyleBottomBar(
    modifier: Modifier = Modifier,
    currentUrl: String,
    blockedCount: Int,
    canGoBack: Boolean,
    isLoading: Boolean,
    showUrlBar: Boolean,
    urlFieldText: TextFieldValue,
    onUrlFieldChange: (TextFieldValue) -> Unit,
    onUrlBarToggle: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onDownloads: () -> Unit,
    desktopMode: Boolean,
    onDesktopModeToggle: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoToggle: (Boolean) -> Unit,
    focusRequester: FocusRequester
) {
    var showMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    
    // Request focus when URL bar opens
    LaunchedEffect(showUrlBar) {
        if (showUrlBar) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground.copy(alpha = 0f),
                        DarkBackground.copy(alpha = 0.9f),
                        DarkBackground
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Main bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkCard)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) TextPrimary else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // URL/Search field (pill shape)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .clickable { onUrlBarToggle(true) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (showUrlBar) {
                    // Editable URL field
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = urlFieldText,
                            onValueChange = onUrlFieldChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(PrimaryBlue),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    onNavigate(urlFieldText.text)
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (urlFieldText.text.isEmpty()) {
                                        Text(
                                            text = "Search or type URL",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextTertiary
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (urlFieldText.text.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    onUrlFieldChange(TextFieldValue(""))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Display current URL (simplified)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ShieldActive,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = try {
                                java.net.URL(currentUrl).host.removePrefix("www.")
                            } catch (_: Exception) {
                                currentUrl.take(30)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Refresh/Stop button
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = if (isLoading) "Stop" else "Refresh",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Menu button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Dropdown menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkCard)
                ) {
                    // Downloads
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Downloads", color = TextPrimary)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDownloads()
                        }
                    )
                    
                    // Desktop Site toggle
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DesktopWindows,
                                    contentDescription = null,
                                    tint = if (desktopMode) ShieldActive else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (desktopMode) "Mobile Site" else "Desktop Site",
                                    color = TextPrimary
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDesktopModeToggle(!desktopMode)
                        }
                    )
                    
                    // Incognito Mode toggle
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PrivacyTip,
                                    contentDescription = null,
                                    tint = if (incognitoMode) Color(0xFFBB86FC) else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (incognitoMode) "Close Incognito" else "New Incognito Tab",
                                    color = TextPrimary
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onIncognitoToggle(!incognitoMode)
                        }
                    )
                    
                    HorizontalDivider(color = DarkSurface)
                    
                    // Home
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Exit Browser", color = TextPrimary)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onHome()
                        }
                    )
                }
            }
        }
        
        // Cancel button when URL bar is open
        AnimatedVisibility(visible = showUrlBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        onUrlBarToggle(false)
                        focusManager.clearFocus()
                    }
                ) {
                    Text("Cancel", color = PrimaryBlue)
                }
            }
        }
    }
}