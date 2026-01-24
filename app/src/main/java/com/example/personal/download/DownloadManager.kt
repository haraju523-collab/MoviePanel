package com.example.personal.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.personal.DownloadsActivity
import com.example.personal.models.DownloadItem
import com.example.personal.models.DownloadStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object DownloadManager {
    
    private const val PREFS_NAME = "download_prefs"
    private const val KEY_DOWNLOADS = "downloads"
    private const val CHANNEL_ID = "download_channel"
    private const val BUFFER_SIZE = 8192
    
    private var context: Context? = null
    private val gson = Gson()
    private val downloads = ConcurrentHashMap<String, DownloadItem>()
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Callbacks for UI updates
    var onDownloadProgress: ((DownloadItem) -> Unit)? = null
    var onDownloadComplete: ((DownloadItem) -> Unit)? = null
    var onDownloadFailed: ((DownloadItem, String) -> Unit)? = null
    
    val isInitialized: Boolean
        get() = context != null
    
    fun init(ctx: Context) {
        if (context == null) {
            context = ctx.applicationContext
            createNotificationChannel()
            loadDownloads()
        }
    }
    
    private fun requireContext(): Context {
        return context ?: throw IllegalStateException("DownloadManager not initialized. Call init(context) first.")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Downloads"
            val descriptionText = "Download notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun loadDownloads() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_DOWNLOADS, null)
        if (saved != null) {
            try {
                val type = object : TypeToken<List<DownloadItem>>() {}.type
                val list: List<DownloadItem> = gson.fromJson(saved, type)
                list.forEach { downloads[it.id] = it }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun saveDownloads() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(downloads.values.toList())
        prefs.edit().putString(KEY_DOWNLOADS, json).apply()
    }
    
    fun getDownloads(): List<DownloadItem> {
        return downloads.values.sortedByDescending { it.timestamp }
    }
    
    fun getDownloadById(id: String): DownloadItem? = downloads[id]
    
    fun startDownload(
        url: String,
        fileName: String? = null,
        mimeType: String = "",
        subdirectory: String? = null
    ): DownloadItem {
        // Validate URL before starting download
        require(url.isNotBlank()) { "Download URL cannot be empty" }
        
        val id = UUID.randomUUID().toString()
        val actualFileName = sanitizeFileName(fileName ?: getFileNameFromUrl(url), mimeType, url)
        val downloadDir = getDownloadDirectory(subdirectory)
        val filePath = File(downloadDir, actualFileName).absolutePath
        
        val item = DownloadItem(
            id = id,
            fileName = actualFileName,
            url = url,
            filePath = filePath,
            mimeType = mimeType,
            status = DownloadStatus.PENDING
        )
        
        downloads[id] = item
        saveDownloads()
        
        val job = scope.launch {
            downloadFile(item)
        }
        downloadJobs[id] = job
        
        return item
    }
    
    /**
     * Start a streaming download that saves to cache folder and allows playback while downloading
     */
    fun startStreamingDownload(
        url: String,
        fileName: String? = null,
        mimeType: String = ""
    ): DownloadItem {
        val id = UUID.randomUUID().toString()
        val actualFileName = sanitizeFileName(fileName ?: getFileNameFromUrl(url), mimeType, url)
        
        // Save to cache directory for streaming
        val cacheDir = requireContext().cacheDir
        val streamDir = File(cacheDir, "streaming_downloads")
        if (!streamDir.exists()) {
            streamDir.mkdirs()
        }
        val filePath = File(streamDir, actualFileName).absolutePath
        
        val item = DownloadItem(
            id = id,
            fileName = actualFileName,
            url = url,
            filePath = filePath,
            mimeType = mimeType,
            status = DownloadStatus.PENDING
        )
        
        downloads[id] = item
        saveDownloads()
        
        val job = scope.launch {
            downloadFileForStreaming(item)
        }
        downloadJobs[id] = job
        
        return item
    }
    
    /**
     * Download file for streaming - uses same logic but saves to cache
     * and allows progressive playback
     */
    private suspend fun downloadFileForStreaming(item: DownloadItem) {
        downloadFile(item) // Reuse same download logic
    }
    
    private suspend fun downloadFile(item: DownloadItem) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        
        try {
            // Update status to downloading
            val startingItem = item.copy(status = DownloadStatus.DOWNLOADING)
            updateDownload(startingItem)
            showProgressNotification(startingItem, 0)
            
            val url = URL(item.url)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.connect()
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP $responseCode")
            }
            
            val fileSize = connection.contentLengthLong.takeIf { it > 0 } ?: 0L
            var updatedItem = startingItem.copy(fileSize = fileSize)
            updateDownload(updatedItem)
            
            inputStream = connection.inputStream
            val file = File(item.filePath)
            file.parentFile?.mkdirs()
            outputStream = FileOutputStream(file)
            
            val buffer = ByteArray(BUFFER_SIZE)
            var downloadedBytes = 0L
            var bytesRead: Int
            var lastProgressUpdate = 0L
            var lastBytesReported = 0L
            var lastTimeReported = System.currentTimeMillis()
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!downloadJobs.containsKey(item.id)) {
                    // Download was cancelled
                    throw CancellationException("Download cancelled")
                }
                
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                
                // Update progress every 500ms to avoid too many updates
                val now = System.currentTimeMillis()
                if (now - lastProgressUpdate > 500 || (fileSize > 0 && downloadedBytes == fileSize)) {
                    val elapsedMs = (now - lastTimeReported).coerceAtLeast(1L)
                    val deltaBytes = downloadedBytes - lastBytesReported
                    val speedBps = if (deltaBytes > 0) (deltaBytes * 1000L) / elapsedMs else 0L
                    val etaSeconds = if (fileSize > 0 && speedBps > 0) {
                        ((fileSize - downloadedBytes) / speedBps).coerceAtLeast(0L)
                    } else 0L

                    lastProgressUpdate = now
                    lastTimeReported = now
                    lastBytesReported = downloadedBytes

                    val progressItem = updatedItem.copy(
                        downloadedSize = downloadedBytes,
                        speedBytesPerSec = speedBps,
                        etaSeconds = etaSeconds,
                        // bump timestamp so active downloads stay fresh
                        timestamp = System.currentTimeMillis()
                    )
                    updatedItem = progressItem
                    updateDownload(progressItem)
                    
                    val progress = if (fileSize > 0) ((downloadedBytes * 100) / fileSize).toInt() else 0
                    showProgressNotification(progressItem, progress)
                    
                    withContext(Dispatchers.Main) {
                        onDownloadProgress?.invoke(progressItem)
                    }
                }
            }
            
            outputStream.flush()
            
            // Download complete
            val completedItem = updatedItem.copy(
                status = DownloadStatus.COMPLETED,
                downloadedSize = downloadedBytes,
                fileSize = if (fileSize <= 0) downloadedBytes else fileSize,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
                timestamp = System.currentTimeMillis()
            )
            updateDownload(completedItem)
            showCompletedNotification(completedItem)
            
            withContext(Dispatchers.Main) {
                onDownloadComplete?.invoke(completedItem)
            }
            
        } catch (e: CancellationException) {
            val pausedItem = item.copy(status = DownloadStatus.PAUSED)
            updateDownload(pausedItem)
        } catch (e: Exception) {
            e.printStackTrace()
            val failedItem = item.copy(status = DownloadStatus.FAILED)
            updateDownload(failedItem)
            showFailedNotification(failedItem)
            
            withContext(Dispatchers.Main) {
                onDownloadFailed?.invoke(failedItem, e.message ?: "Unknown error")
            }
        } finally {
            inputStream?.close()
            outputStream?.close()
            connection?.disconnect()
            downloadJobs.remove(item.id)
        }
    }
    
    private fun updateDownload(item: DownloadItem) {
        downloads[item.id] = item
        saveDownloads()
    }
    
    fun cancelDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        
        // Optimistically mark as paused in storage so UI updates immediately
        downloads[id]?.let { current ->
            val paused = current.copy(status = DownloadStatus.PAUSED)
            updateDownload(paused)
        }
    }
    
    fun deleteDownload(id: String) {
        cancelDownload(id)
        downloads[id]?.let { item ->
            File(item.filePath).delete()
        }
        downloads.remove(id)
        saveDownloads()
        
        // Clear any existing notification for this download
        try {
            NotificationManagerCompat.from(requireContext()).cancel(id.hashCode())
        } catch (_: SecurityException) {
        }
    }
    
    fun retryDownload(id: String) {
        downloads[id]?.let { item ->
            File(item.filePath).delete()
            val newItem = item.copy(
                status = DownloadStatus.PENDING,
                downloadedSize = 0L,
                timestamp = System.currentTimeMillis()
            )
            downloads[id] = newItem
            saveDownloads()
            
            val job = scope.launch {
                downloadFile(newItem)
            }
            downloadJobs[id] = job
        }
    }
    
    private fun getDownloadDirectory(subdirectory: String? = null): File {
        // On modern Android (scoped storage, targetSdk >= 30) direct writes to
        // Environment.getExternalStoragePublicDirectory(...) are no longer allowed.
        // Use the app-specific external "Downloads" directory instead, which works
        // without storage permissions for API 29+ and is still accessible to the app.
        val baseDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: requireContext().filesDir
        
        val moviePanelRoot = File(baseDir, "MoviePanel")
        if (!moviePanelRoot.exists()) {
            moviePanelRoot.mkdirs()
        }

        val safeSubdirName = subdirectory
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val dir = if (safeSubdirName != null) {
            File(moviePanelRoot, safeSubdirName)
        } else {
            moviePanelRoot
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    private fun getFileNameFromUrl(url: String): String {
        try {
            val path = URL(url).path
            val fileName = path.substringAfterLast('/')
            if (fileName.isNotBlank() && fileName.contains('.')) {
                return fileName.take(100)  // Limit filename length
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Generate a default name
        val timestamp = System.currentTimeMillis()
        val extension = MimeTypeMap.getFileExtensionFromUrl(url) ?: "mp4"
        return "download_$timestamp.$extension"
    }

    private fun sanitizeFileName(
        rawName: String,
        mimeType: String,
        url: String
    ): String {
        var name = rawName.trim()

        // Try to decode URL-encoded names like "The%20Pitt%20S01E01"
        try {
            if (name.contains('%') || name.contains('+')) {
                name = java.net.URLDecoder.decode(name, "UTF-8")
            }
        } catch (_: Exception) {
        }
        if (name.isBlank()) {
            name = getFileNameFromUrl(url)
        }

        // Remove illegal filename characters on most filesystems
        name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        // Ensure we have an extension
        val hasExtension = name.contains('.') &&
            !name.endsWith(".") &&
            name.substringAfterLast('.', "").length in 1..10

        if (!hasExtension) {
            val extFromMime = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
            val extFromUrl = MimeTypeMap.getFileExtensionFromUrl(url)
            val ext = extFromMime ?: extFromUrl ?: "mp4"
            name = "${name.ifBlank { "download_${System.currentTimeMillis()}" }}.$ext"
        }

        // Limit length to something reasonable
        if (name.length > 120) {
            val dotIndex = name.lastIndexOf('.')
            val base = if (dotIndex > 0) name.substring(0, dotIndex) else name
            val ext = if (dotIndex > 0) name.substring(dotIndex) else ""
            name = base.take(100) + ext.take(10)
        }

        return name
    }
    
    private fun showProgressNotification(item: DownloadItem, progress: Int) {
        try {
            val intent = Intent(requireContext(), DownloadsActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(item.fileName)
                .setContentText("Downloading... $progress%")
                .setProgress(100, progress, progress == 0)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()
            
            NotificationManagerCompat.from(requireContext()).notify(item.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }
    
    private fun showCompletedNotification(item: DownloadItem) {
        try {
            val intent = Intent(requireContext(), DownloadsActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText(item.fileName)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            
            NotificationManagerCompat.from(requireContext()).notify(item.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }
    
    private fun showFailedNotification(item: DownloadItem) {
        try {
            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download Failed")
                .setContentText(item.fileName)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(requireContext()).notify(item.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }
    
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
