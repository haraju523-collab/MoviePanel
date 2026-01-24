package com.example.personal.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.sp
import com.example.personal.data.SiteRepository
import com.example.personal.ui.theme.*
import com.example.personal.utils.ShareUtils
import com.example.personal.utils.UpdateManager
import androidx.core.content.FileProvider
import java.io.File
import com.example.personal.ui.components.UpdateDialog
import com.example.personal.download.DownloadManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    
    
    val allSites = remember(refreshKey) { SiteRepository.getAllSites() }
    val favoritesCount = remember(refreshKey) { SiteRepository.getFavoriteSites().size }
    val customSitesCount = remember(refreshKey) { SiteRepository.getCustomSites().size }
    
    // Load preferences
    var currentTheme by remember { mutableStateOf(com.example.personal.data.PreferencesManager.getTheme()) }
    var downloadLocation by remember { mutableStateOf(com.example.personal.data.PreferencesManager.getDownloadLocation()) }
    var defaultViewMode by remember { mutableStateOf(com.example.personal.data.PreferencesManager.getDefaultViewMode()) }
    var selectedFilters by remember { mutableStateOf(com.example.personal.data.PreferencesManager.getSmartFilters()) }
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDownloadLocationDialog by remember { mutableStateOf(false) }
    var showViewModeDialog by remember { mutableStateOf(false) }
    
    // Update Checker State
    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isUpdateDownloading by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf(0f) }
    
    // Calculate stats (mock data for ads blocked and time saved)
    val adsBlocked = remember(allSites.size) { (allSites.size * 150).toString() }
    val timeSaved = remember(allSites.size) { 
        val hours = allSites.size * 2
        if (hours >= 1) "${hours}h" else "${hours * 60}m"
    }
    
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MoviePanel",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ShieldActive.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = ShieldActive,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Adblock ON",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShieldActive,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Stats Row (Director's Cut style)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MinimalStatCard(
                        icon = Icons.Default.Shield,
                        value = adsBlocked,
                        label = "Ads Blocked",
                        color = ShieldActive,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalStatCard(
                        icon = Icons.Default.Timer,
                        value = timeSaved,
                        label = "Time Saved",
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Smart Filters
            item {
                Text(
                    text = "Smart Filters",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("HD", "Fast", "Mobile Friendly", "Less Popups").forEach { filter ->
                        val isSelected = selectedFilters.contains(filter)
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                selectedFilters = com.example.personal.data.PreferencesManager.toggleSmartFilter(filter)
                                showToast(if (isSelected) "$filter filter removed" else "$filter filter enabled")
                            },
                            label = { Text(filter, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryBlue
                            )
                        )
                    }
                }
            }
            
            // Preferences Section (Minimal)
            item {
                MinimalSectionCard(
                    title = "Preferences",
                    items = listOf(
                        MinimalSettingItem(
                            icon = Icons.Default.Palette,
                            title = "Theme",
                            subtitle = currentTheme.name.replaceFirstChar { it.uppercaseChar() },
                            onClick = { showThemeDialog = true }
                        ),
                        MinimalSettingItem(
                            icon = Icons.Default.ViewList,
                            title = "Default View",
                            subtitle = defaultViewMode.name.replaceFirstChar { it.uppercaseChar() },
                            onClick = { showViewModeDialog = true }
                        ),
                        MinimalSettingItem(
                            icon = Icons.Default.Folder,
                            title = "Download Location",
                            subtitle = downloadLocation,
                            onClick = { showDownloadLocationDialog = true }
                        )
                    )
                )
            }
            
            // Data Management (Minimal)
            item {
                MinimalSectionCard(
                    title = "Data",
                    items = listOf(
                        MinimalSettingItem(
                            icon = Icons.Default.Backup,
                            title = "Backup",
                            subtitle = "Export data",
                            onClick = { 
                                try {
                                    ShareUtils.shareFavorites(context, SiteRepository.getFavoriteSites())
                                    showToast("Backup shared!")
                                } catch (e: Exception) {
                                    showToast("Backup failed")
                                }
                            }
                        ),
                        MinimalSettingItem(
                            icon = Icons.Default.Delete,
                            title = "Clear Cache",
                            subtitle = "Free storage",
                            onClick = { 
                                try {
                                    context.cacheDir.deleteRecursively()
                                    showToast("Cache cleared!")
                                } catch (e: Exception) {
                                    showToast("Clear failed")
                                }
                            }
                        )
                    )
                )
            }
            
            // About (Minimal)
            item {
                MinimalSectionCard(
                    title = "About",
                    items = listOf(
                        MinimalSettingItem(
                            icon = Icons.Default.Info,
                            title = "Version",
                            subtitle = "1.0",
                            onClick = { 
                                if (!isCheckingUpdate) {
                                    isCheckingUpdate = true
                                    showToast("Checking for updates...")
                                    scope.launch {
                                        val info = UpdateManager.checkForUpdates()
                                        isCheckingUpdate = false
                                        if (info != null) {
                                            updateInfo = info
                                        } else {
                                            showToast("You are up to date! ✅")
                                        }
                                    }
                                }
                            }
                        ),
                        MinimalSettingItem(
                            icon = Icons.Default.SystemUpdate,
                            title = "Check for Updates",
                            subtitle = if (isCheckingUpdate) "Checking..." else "Tap to check",
                            onClick = {
                                if (!isCheckingUpdate) {
                                    isCheckingUpdate = true
                                    showToast("Checking for updates...")
                                    scope.launch {
                                        val info = UpdateManager.checkForUpdates()
                                        isCheckingUpdate = false
                                        if (info != null) {
                                            updateInfo = info
                                        } else {
                                            showToast("You are up to date! ✅")
                                        }
                                    }
                                }
                            }
                        ),
                        MinimalSettingItem(
                            icon = Icons.Default.Star,
                            title = "Rate App",
                            subtitle = "Feedback",
                            onClick = { 
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    showToast("Thank you! ⭐")
                                }
                            }
                        )
                    )
                )
            }
        }
        
        // Theme Selection Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = currentTheme,
                onDismiss = { showThemeDialog = false },
                onThemeSelected = { theme ->
                    currentTheme = theme
                    com.example.personal.data.PreferencesManager.setTheme(theme)
                    showThemeDialog = false
                    showToast("Theme changed to ${theme.name.replaceFirstChar { it.uppercaseChar() }}")
                    // Trigger activity recreation to apply theme immediately
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        (context as? android.app.Activity)?.recreate()
                    }, 300)
                }
            )
        }
        
        // Download Location Dialog
        if (showDownloadLocationDialog) {
            DownloadLocationDialog(
                currentLocation = downloadLocation,
                onDismiss = { showDownloadLocationDialog = false },
                onLocationSelected = { location ->
                    downloadLocation = location
                    com.example.personal.data.PreferencesManager.setDownloadLocation(location)
                    showDownloadLocationDialog = false
                    showToast("Download location: $location")
                }
            )
        }
        
        // View Mode Dialog
        if (showViewModeDialog) {
            ViewModeDialog(
                currentMode = defaultViewMode,
                onDismiss = { showViewModeDialog = false },
                onModeSelected = { mode ->
                    defaultViewMode = mode
                    com.example.personal.data.PreferencesManager.setDefaultViewMode(mode)
                    showViewModeDialog = false
                    showToast("Default view: ${mode.name.replaceFirstChar { it.uppercaseChar() }}")
                }
            )
        }
        
        // Update Dialog
        updateInfo?.let { info ->
            UpdateDialog(
                updateInfo = info,
                isDownloading = isUpdateDownloading,
                downloadProgress = updateProgress,
                onUpdate = {
                     try {
                        isUpdateDownloading = true
                        updateProgress = 0f
                        
                        // Set up callbacks
                        DownloadManager.onDownloadProgress = { item ->
                            if (item.url == info.downloadUrl) {
                                val progress = if (item.fileSize > 0) item.downloadedSize.toFloat() / item.fileSize.toFloat() else 0f
                                updateProgress = progress
                            }
                        }
                        
                        DownloadManager.onDownloadComplete = { item ->
                            if (item.url == info.downloadUrl) {
                                isUpdateDownloading = false
                                updateInfo = null
                                val file = File(item.filePath)
                                if (file.exists()) {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/vnd.android.package-archive")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        showToast("Error launching installer: ${e.message}")
                                    }
                                } else {
                                    showToast("Update file not found")
                                }
                            }
                        }

                        DownloadManager.onDownloadFailed = { item, error ->
                            if (item.url == info.downloadUrl) {
                                isUpdateDownloading = false
                                showToast("Update failed: $error")
                            }
                        }

                        DownloadManager.startDownload(
                            url = info.downloadUrl,
                            fileName = "MoviePanel_v${info.versionName}.apk",
                            mimeType = "application/vnd.android.package-archive"
                        )
                    } catch (e: Exception) {
                        isUpdateDownloading = false
                        showToast("Failed to start download: ${e.message}")
                    }
                },
                onDismiss = { 
                    if (!isUpdateDownloading) {
                        updateInfo = null 
                    }
                }
            )
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: com.example.personal.data.PreferencesManager.Theme,
    onDismiss: () -> Unit,
    onThemeSelected: (com.example.personal.data.PreferencesManager.Theme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.personal.data.PreferencesManager.Theme.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = theme.name.replaceFirstChar { it.uppercaseChar() },
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
private fun DownloadLocationDialog(
    currentLocation: String,
    onDismiss: () -> Unit,
    onLocationSelected: (String) -> Unit
) {
    val locations = listOf("App Downloads", "Movies", "Videos", "Downloads", "Custom")
    var selectedLocation by remember { mutableStateOf(currentLocation) }
    var customLocation by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Location", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                locations.forEach { location ->
                    if (location == "Custom") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomInput = !showCustomInput }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLocation == location && !showCustomInput,
                                onClick = { showCustomInput = !showCustomInput }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = location,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (showCustomInput) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customLocation,
                                onValueChange = { customLocation = it },
                                label = { Text("Folder name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLocation = location }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLocation == location && !showCustomInput,
                                onClick = { 
                                    selectedLocation = location
                                    showCustomInput = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = location,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalLocation = if (showCustomInput && customLocation.isNotBlank()) {
                        customLocation
                    } else {
                        selectedLocation
                    }
                    onLocationSelected(finalLocation)
                }
            ) {
                Text("Save", color = PrimaryBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
private fun ViewModeDialog(
    currentMode: com.example.personal.data.PreferencesManager.ViewMode,
    onDismiss: () -> Unit,
    onModeSelected: (com.example.personal.data.PreferencesManager.ViewMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default View Mode", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.personal.data.PreferencesManager.ViewMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mode.name.replaceFirstChar { it.uppercaseChar() },
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryBlue)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    items: List<SettingItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingRow(item = item)
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = GlassBorder.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
fun SettingRow(item: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { item.onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MinimalStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun MinimalSectionCard(
    title: String,
    items: List<MinimalSettingItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    MinimalSettingRow(item = item)
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = GlassBorder.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

data class MinimalSettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
private fun MinimalSettingRow(item: MinimalSettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { item.onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
