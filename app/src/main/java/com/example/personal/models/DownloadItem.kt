package com.example.personal.models

data class DownloadItem(
    val id: String,
    val fileName: String,
    val url: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val mimeType: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    // Transient runtime info (not critical if stale)
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}
