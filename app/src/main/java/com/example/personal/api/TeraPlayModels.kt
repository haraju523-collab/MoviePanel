package com.example.personal.api

import com.google.gson.annotations.SerializedName

/**
 * TeraPlay API Response Models
 * Used to parse the JSON response from TeraBox API endpoints
 */

data class TeraPlayResponse(
    @SerializedName("status")
    val status: String?,
    
    @SerializedName("total_files")
    val totalFiles: Int?,
    
    @SerializedName("list")
    val list: List<TeraPlayFile>?
) {
    fun isSuccess(): Boolean = status == "success" && !list.isNullOrEmpty()
    fun getFileList(): List<TeraPlayFile> = list ?: emptyList()
}

/**
 * Fast stream URLs with multiple quality options
 */
data class FastStreamUrl(
    @SerializedName("360p")
    val quality360p: String?,
    
    @SerializedName("480p")
    val quality480p: String?,
    
    @SerializedName("720p")
    val quality720p: String?,
    
    @SerializedName("1080p")
    val quality1080p: String?
) {
    /**
     * Get best available quality URL
     */
    fun getBestQualityUrl(): String? {
        return quality1080p?.takeIf { it.isNotBlank() }
            ?: quality720p?.takeIf { it.isNotBlank() }
            ?: quality480p?.takeIf { it.isNotBlank() }
            ?: quality360p?.takeIf { it.isNotBlank() }
    }
    
    /**
     * Get all available qualities as a map
     */
    fun getAvailableQualities(): Map<String, String> {
        return buildMap {
            quality360p?.takeIf { it.isNotBlank() }?.let { put("360p", it) }
            quality480p?.takeIf { it.isNotBlank() }?.let { put("480p", it) }
            quality720p?.takeIf { it.isNotBlank() }?.let { put("720p", it) }
            quality1080p?.takeIf { it.isNotBlank() }?.let { put("1080p", it) }
        }
    }
}

data class TeraPlayFile(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("size")
    val sizeBytes: Long?,
    
    @SerializedName("size_formatted")
    val sizeFormatted: String?,
    
    @SerializedName("duration")
    val duration: String?,
    
    @SerializedName("quality")
    val quality: String?,
    
    @SerializedName("download_link")
    val downloadLink: String?,
    
    @SerializedName("fast_download_link")
    val fastDownloadLink: String?,
    
    @SerializedName("stream_url")
    val streamUrl: String?,
    
    @SerializedName("fast_stream_url")
    val fastStreamUrl: FastStreamUrl?,
    
    @SerializedName("thumbnail")
    val thumbnail: String?,
    
    @SerializedName("subtitle_url")
    val subtitleUrl: String?,
    
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("fs_id")
    val fsId: Long?,
    
    @SerializedName("is_dir")
    val isDir: String?,
    
    @SerializedName("folder")
    val folder: String?
) {
    /**
     * Check if this file is a video based on type or file extension
     */
    fun isVideo(): Boolean {
        if (type?.contains("video", ignoreCase = true) == true) return true
        val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".webm", ".m4v", ".flv")
        return videoExtensions.any { name.lowercase().endsWith(it) }
    }
    
    /**
     * Get the best URL for streaming (prefer fast stream, then stream URL, fallback to download)
     */
    fun getStreamableUrl(): String {
        // Try fast stream URL first (quality options)
        fastStreamUrl?.getBestQualityUrl()?.let { return it }
        // Then regular stream URL
        streamUrl?.takeIf { it.isNotBlank() }?.let { return it }
        // Fallback to download URLs
        return fastDownloadLink?.takeIf { it.isNotBlank() }
            ?: downloadLink ?: ""
    }
    
    /**
     * Get available stream qualities
     */
    fun getStreamQualities(): Map<String, String> {
        return fastStreamUrl?.getAvailableQualities() ?: emptyMap()
    }
    
    /**
     * Get the best URL for downloading
     */
    fun getDownloadUrl(): String {
        return fastDownloadLink?.takeIf { it.isNotBlank() }
            ?: downloadLink ?: ""
    }
    
    /**
     * Format file size for display
     */
    fun getFormattedSize(): String {
        return sizeFormatted?.takeIf { it.isNotBlank() } ?: "Unknown size"
    }
    
    /**
     * Format duration for display
     */
    fun getFormattedDuration(): String {
        return duration?.takeIf { it.isNotBlank() } ?: "Unknown duration"
    }
}
