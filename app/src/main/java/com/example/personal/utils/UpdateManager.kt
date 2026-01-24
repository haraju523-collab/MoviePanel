package com.example.personal.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.personal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object UpdateManager {
    
    // TODO: Replace with your actual GitHub repository details
    // Ensure you have a file named 'version.json' at the root of your repo
    // Format:
    // {
    //   "versionCode": 2,
    //   "versionName": "1.1.0",
    //   "changelog": "New features added...",
    //   "downloadUrl": "https://github.com/USER/REPO/releases/download/v1.1.0/app-release.apk"
    // }
    private const val GITHUB_USER = "haraju523-collab"
    private const val GITHUB_REPO = "MoviePanel"
    private const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/main/version.json"

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val changelog: String,
        val downloadUrl: String
    )

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$UPDATE_JSON_URL?t=${System.currentTimeMillis()}")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)
                
                val remoteVersionCode = json.getInt("versionCode")
                
                if (remoteVersionCode > BuildConfig.VERSION_CODE) {
                    return@withContext UpdateInfo(
                        versionCode = remoteVersionCode,
                        versionName = json.getString("versionName"),
                        changelog = json.getString("changelog"),
                        downloadUrl = json.getString("downloadUrl")
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
