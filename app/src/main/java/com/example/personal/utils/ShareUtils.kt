package com.example.personal.utils

import android.content.Context
import android.content.Intent
import com.example.personal.models.MovieSite

object ShareUtils {
    fun shareSite(context: Context, site: MovieSite) {
        val shareText = """
            📺 ${site.name}
            ${site.description}
            
            🔗 ${site.url}
            
            Shared from MoviePanel - Ad-Free Streaming Hub
        """.trimIndent()
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Check out ${site.name}")
        }
        
        context.startActivity(Intent.createChooser(intent, "Share ${site.name}"))
    }
    
    fun shareFavorites(context: Context, sites: List<MovieSite>) {
        val shareText = buildString {
            appendLine("⭐ My Favorite Streaming Sites")
            appendLine()
            sites.forEachIndexed { index, site ->
                appendLine("${index + 1}. ${site.name}")
                appendLine("   ${site.url}")
                appendLine()
            }
            appendLine("Shared from MoviePanel - Ad-Free Streaming Hub")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "My Favorite Streaming Sites")
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Favorites"))
    }
    
    fun shareCustomSites(context: Context, sites: List<MovieSite>) {
        val shareText = buildString {
            appendLine("📌 My Custom Streaming Sites")
            appendLine()
            sites.forEachIndexed { index, site ->
                appendLine("${index + 1}. ${site.name}")
                appendLine("   Category: ${site.category}")
                appendLine("   ${site.url}")
                appendLine()
            }
            appendLine("Shared from MoviePanel - Ad-Free Streaming Hub")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "My Custom Streaming Sites")
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Custom Sites"))
    }
}
