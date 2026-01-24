package com.example.personal.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * TeraPlay API Client
 * Fetches video information from TeraBox links via multiple API endpoints
 * 
 * Primary API: https://teraplay.in/api/fetch?url={terabox_url}
 * Fallback API: https://1024teradownloader.com/api/stream (POST multipart/form-data)
 * 
 * Note: These are third-party APIs and may change or become unavailable.
 * Video/download links are time-limited tokens.
 */
object TeraPlayApi {
    private const val TAG = "TeraPlayApi"
    private const val TERAPLAY_URL = "https://teraplay.in/api/fetch"
    private const val TERADOWNLOADER_URL = "https://1024teradownloader.com/api/stream"
    private const val TIMEOUT_MS = 20000
    
    private val gson = Gson()
    
    /**
     * TeraBox domain patterns to detect - supports wide variety of TeraBox domains
     */
    val TERABOX_DOMAINS = listOf(
        // Official TeraBox domains
        "teraboxapp.com",
        "terabox.com",
        "1024terabox.com",
        // TeraBox link variants
        "teraboxlink.com",
        "terasharelink.com",
        "terasharefile.com",
        // Free TeraBox variants
        "freeterabox.com",
        "terabox.fun",
        "terabox.club",
        // Dubox domains (TeraBox's predecessor)
        "dubox.com",
        "1024dubox.com",
        // TeraBox mirror domains
        "terabox.tech",
        "terabox.co",
        "terabox.me",
        "mirrobox.com",
        "nephobox.com",
        // Additional variants
        "momerybox.com",
        "teraboxshare.com",
        "4funbox.com",
        "4funbox.co"
    )
    
    /**
     * Check if a URL is a TeraBox share link
     */
    fun isTeraBoxUrl(url: String): Boolean {
        return try {
            val lowerUrl = url.lowercase()
            TERABOX_DOMAINS.any { domain ->
                lowerUrl.contains(domain) && 
                (lowerUrl.contains("/s/") || lowerUrl.contains("/sharing/link"))
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Fetch video information from a TeraBox URL
     * Tries primary API first, falls back to secondary if it fails
     * 
     * @param teraboxUrl The TeraBox share URL (e.g., https://teraboxapp.com/s/xxx)
     * @return TeraPlayResponse on success, null on failure
     */
    suspend fun fetchTeraBoxInfo(teraboxUrl: String): Result<TeraPlayResponse> {
        return withContext(Dispatchers.IO) {
            // Try primary API (teraplay.in)
            Log.d(TAG, "Trying primary API (teraplay.in)...")
            val primaryResult = fetchFromTeraPlay(teraboxUrl)
            
            if (primaryResult.isSuccess) {
                Log.d(TAG, "Primary API succeeded")
                return@withContext primaryResult
            }
            
            Log.d(TAG, "Primary API failed, trying fallback (1024teradownloader.com)...")
            // Try fallback API (1024teradownloader.com)
            val fallbackResult = fetchFromTeraDownloader(teraboxUrl)
            
            if (fallbackResult.isSuccess) {
                Log.d(TAG, "Fallback API succeeded")
                return@withContext fallbackResult
            }
            
            // Both failed, return the primary error
            Log.e(TAG, "Both APIs failed")
            primaryResult
        }
    }
    
    /**
     * Fetch from teraplay.in (GET request)
     */
    private fun fetchFromTeraPlay(teraboxUrl: String): Result<TeraPlayResponse> {
        return try {
            val encodedUrl = URLEncoder.encode(teraboxUrl, "UTF-8")
            val apiUrl = "$TERAPLAY_URL?url=$encodedUrl"
            
            Log.d(TAG, "TeraPlay API: $apiUrl")
            
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
                setRequestProperty("Referer", "https://teraplay.in/")
                setRequestProperty("Origin", "https://teraplay.in")
                setRequestProperty("Sec-Fetch-Dest", "empty")
                setRequestProperty("Sec-Fetch-Mode", "cors")
                setRequestProperty("Sec-Fetch-Site", "same-origin")
                setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"142\", \"Not A Brand\";v=\"99\"")
                setRequestProperty("sec-ch-ua-mobile", "?0")
                setRequestProperty("sec-ch-ua-platform", "\"Windows\"")
            }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "TeraPlay response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "TeraPlay response: ${responseText.take(200)}...")
                
                val response = gson.fromJson(responseText, TeraPlayResponse::class.java)
                
                if (response.isSuccess()) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("TeraPlay API returned status: ${response.status}"))
                }
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "TeraPlay API error: $responseCode - $errorText")
                Result.failure(Exception("TeraPlay API error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "TeraPlay API failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch from 1024teradownloader.com (POST multipart/form-data)
     */
    private fun fetchFromTeraDownloader(teraboxUrl: String): Result<TeraPlayResponse> {
        return try {
            val boundary = "----WebKitFormBoundary${UUID.randomUUID().toString().replace("-", "").take(16)}"
            
            Log.d(TAG, "TeraDownloader API: $TERADOWNLOADER_URL")
            
            val url = URL(TERADOWNLOADER_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                
                // Headers mimicking browser
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
                setRequestProperty("Origin", "https://1024teradownloader.com")
                setRequestProperty("Referer", "https://1024teradownloader.com/")
                setRequestProperty("Sec-Fetch-Dest", "empty")
                setRequestProperty("Sec-Fetch-Mode", "cors")
                setRequestProperty("Sec-Fetch-Site", "same-origin")
                setRequestProperty("sec-ch-ua", "\"Not_A Brand\";v=\"99\", \"Chromium\";v=\"142\"")
                setRequestProperty("sec-ch-ua-mobile", "?0")
                setRequestProperty("sec-ch-ua-platform", "\"Windows\"")
            }
            
            // Build multipart body
            val body = buildString {
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"url\"\r\n")
                append("\r\n")
                append(teraboxUrl)
                append("\r\n")
                append("--$boundary--")
            }
            
            // Write body
            DataOutputStream(connection.outputStream).use { out ->
                out.writeBytes(body)
                out.flush()
            }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "TeraDownloader response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "TeraDownloader response: ${responseText.take(200)}...")
                
                val response = gson.fromJson(responseText, TeraPlayResponse::class.java)
                
                if (response.isSuccess()) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("TeraDownloader API returned status: ${response.status}"))
                }
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "TeraDownloader API error: $responseCode - $errorText")
                Result.failure(Exception("TeraDownloader API error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "TeraDownloader API failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Convenience method with callbacks
     */
    suspend fun fetchTeraBoxInfo(
        teraBoxUrl: String,
        onSuccess: (TeraPlayResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        fetchTeraBoxInfo(teraBoxUrl).fold(
            onSuccess = onSuccess,
            onFailure = onError
        )
    }
}
