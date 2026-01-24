package com.example.personal

import android.app.Application
import com.example.personal.data.SiteRepository
import com.example.personal.download.DownloadManager

class MoviePanelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize singletons
        SiteRepository.init(this)
        DownloadManager.init(this)
    }
}
