package com.example.personal.utils

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.personal.api.TeraPlayApi
import java.io.ByteArrayInputStream

/**
 * Enhanced WebViewClient with strict ad blocking and popup prevention
 */
open class MyWebViewClient : WebViewClient() {
    
    private val TAG = "MyWebViewClient"
    private var currentUrl: String? = null
    private var lastNavigationTime = 0L
    private val navigationCooldown = 500L // ms between navigations
    private var blockedCount = 0
    
    // Track recent blocked URLs to avoid spam logs
    private val recentlyBlocked = mutableSetOf<String>()
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        currentUrl = url
        
        // Inject ad blocking script early
        view?.evaluateJavascript(AdBlocker.getAdBlockScript(), null)
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        currentUrl = url
        
        // Inject ad blocking script again after page loads
        view?.evaluateJavascript(AdBlocker.getAdBlockScript(), null)
        
        // Additional cleanup script for common overlay patterns
        val cleanupScript = """
            (function() {
                // Hide fixed overlays
                document.querySelectorAll('[style*="position: fixed"]').forEach(function(el) {
                    if (el.offsetWidth > window.innerWidth * 0.5 || 
                        el.offsetHeight > window.innerHeight * 0.5) {
                        el.style.display = 'none';
                    }
                });
                
                // Remove body scroll locks
                document.body.style.overflow = 'auto';
                document.body.style.position = 'static';
                document.documentElement.style.overflow = 'auto';
            })();
        """.trimIndent()
        view?.evaluateJavascript(cleanupScript, null)
    }
    
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()
        
        // Check if this should be blocked
        if (AdBlocker.isAd(url)) {
            if (!recentlyBlocked.contains(url)) {
                recentlyBlocked.add(url)
                blockedCount++
                Log.d(TAG, "Blocked ad request: ${url.take(80)}...")
                onBlockedRequest()
                
                // Clean up old entries periodically
                if (recentlyBlocked.size > 100) {
                    recentlyBlocked.clear()
                }
            }
            
            // Return empty response for blocked requests
            return WebResourceResponse(
                "text/plain",
                "utf-8",
                ByteArrayInputStream("".toByteArray())
            )
        }
        
        return super.shouldInterceptRequest(view, request)
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return true
        
        // Rapid navigation detection (popup/redirect spam)
        val now = System.currentTimeMillis()
        if (now - lastNavigationTime < navigationCooldown) {
            Log.d(TAG, "Blocked rapid navigation: ${url.take(50)}...")
            onBlockedRequest()
            return true
        }
        lastNavigationTime = now
        
        // Check for TeraBox URLs first - these we want to handle specially
        if (TeraPlayApi.isTeraBoxUrl(url)) {
            onTeraBoxUrlDetected(url)
            return true
        }
        
        // Check if the URL is a direct video file - allow these
        val lowerUrl = url.lowercase()
        val isVideoFile = lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || 
                          lowerUrl.endsWith(".webm") || lowerUrl.endsWith(".avi") ||
                          lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".flv") ||
                          lowerUrl.endsWith(".m3u8") || lowerUrl.contains("/video/")
                          
        if (isVideoFile) {
            return false // Let WebView handle video files
        }
        
        // Handle Telegram links - allow for video streaming
        if (lowerUrl.contains("telegram.org") || lowerUrl.contains("t.me")) {
            return false
        }
        
        // STRICT AD/REDIRECT BLOCKING
        if (AdBlocker.shouldBlockNavigation(url, currentUrl)) {
            Log.d(TAG, "Blocked navigation: ${url.take(60)}...")
            onBlockedRequest()
            return true
        }
        
        // Block suspicious URLs
        if (AdBlocker.isSuspicious(url)) {
            Log.d(TAG, "Blocked suspicious URL: ${url.take(60)}...")
            onBlockedRequest()
            return true
        }
        
        // Allow normal navigation
        return false
    }
    
    /**
     * Get count of blocked requests during this session
     */
    fun getBlockedCount(): Int = blockedCount
    
    /**
     * Reset blocked count
     */
    fun resetBlockedCount() {
        blockedCount = 0
    }
    
    // Callback for blocked requests - override in subclass
    open fun onBlockedRequest() {}
    
    // Callback for TeraBox URL detection - override in subclass
    open fun onTeraBoxUrlDetected(url: String) {}
}

/**
 * JavaScript to completely disable popups - can be injected into any WebView
 */
fun getAntiPopupScript(): String = """
    (function() {
        // Completely block popup functions
        var noop = function() { return null; };
        window.open = noop;
        
        // Block alert dialogs that are often used as part of popup chains
        window.alert = function(msg) { console.log('Blocked alert:', msg); };
        window.confirm = function() { return false; };
        window.prompt = function() { return null; };
        
        // Block creating new windows via createElement
        var origCreateElement = document.createElement;
        document.createElement = function(tagName) {
            if (tagName.toLowerCase() === 'a') {
                var el = origCreateElement.call(document, tagName);
                el.target = '_self'; // Force same window
                return el;
            }
            return origCreateElement.call(document, tagName);
        };
        
        // Block beforeunload which can trigger popups
        window.onbeforeunload = null;
        
        // Prevent scripts from changing window.location externally 
        // (but allow user-initiated navigation)
        var userClicked = false;
        document.addEventListener('click', function() { 
            userClicked = true; 
            setTimeout(function() { userClicked = false; }, 100);
        }, true);
        
        console.log('Anti-popup script loaded');
    })();
""".trimIndent()