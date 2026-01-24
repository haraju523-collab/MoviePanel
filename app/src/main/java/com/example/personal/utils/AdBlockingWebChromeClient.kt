package com.example.personal.utils

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest

/**
 * Enhanced WebChromeClient that blocks popups and unwanted features
 */
open class AdBlockingWebChromeClient : WebChromeClient() {
    
    private val TAG = "AdBlockChromeClient"
    private var blockedPopups = 0
    
    // Callback for file chooser (if needed for uploads)
    var onFileChooser: ((ValueCallback<Array<Uri>>?) -> Boolean)? = null
    
    /**
     * Block popup windows completely
     */
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        // Even if it's supposedly user gesture, we block all popup windows
        // The user can manually tap links to navigate
        blockedPopups++
        Log.d(TAG, "Blocked popup window (dialog=$isDialog, userGesture=$isUserGesture)")
        
        // Return false to prevent the popup
        return false
    }
    
    /**
     * Block JavaScript alerts (often used in popup chains)
     */
    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        Log.d(TAG, "Blocked JS alert from $url: ${message?.take(50)}")
        result?.cancel()
        return true // Return true to indicate we handled it
    }
    
    /**
     * Block JavaScript confirms
     */
    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        Log.d(TAG, "Blocked JS confirm from $url: ${message?.take(50)}")
        result?.cancel()
        return true
    }
    
    /**
     * Block JavaScript prompts
     */
    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        Log.d(TAG, "Blocked JS prompt from $url: ${message?.take(50)}")
        result?.cancel()
        return true
    }
    
    /**
     * Block beforeunload dialogs (often used to keep users on page)
     */
    override fun onJsBeforeUnload(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        // Always allow leaving the page without confirmation
        result?.confirm()
        return true
    }
    
    /**
     * Block geolocation requests
     */
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        // Deny geolocation for privacy
        callback?.invoke(origin, false, false)
        Log.d(TAG, "Blocked geolocation request from $origin")
    }
    
    /**
     * Block permission requests (camera, mic, etc.)
     */
    override fun onPermissionRequest(request: PermissionRequest?) {
        // Deny all permissions by default
        request?.deny()
        Log.d(TAG, "Blocked permission request: ${request?.resources?.joinToString()}")
    }
    
    /**
     * Log console messages for debugging
     */
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        // Log only errors and warnings
        when (consoleMessage?.messageLevel()) {
            ConsoleMessage.MessageLevel.ERROR -> {
                Log.e(TAG, "Console error: ${consoleMessage.message()}")
            }
            ConsoleMessage.MessageLevel.WARNING -> {
                Log.w(TAG, "Console warning: ${consoleMessage.message()}")
            }
            else -> {} // Ignore other messages
        }
        return true
    }
    
    /**
     * Handle file chooser for uploads
     */
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onFileChooser?.invoke(filePathCallback) ?: run {
            filePathCallback?.onReceiveValue(null)
            true
        }
    }
    
    /**
     * Block custom view (fullscreen ads)
     */
    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        // For video fullscreen, we want to allow it
        // But for ads, we'd block it - hard to distinguish so we allow
        super.onShowCustomView(view, callback)
    }
    
    /**
     * Get count of blocked popups
     */
    fun getBlockedPopupCount(): Int = blockedPopups
    
    /**
     * Reset blocked popup count
     */
    fun resetBlockedPopupCount() {
        blockedPopups = 0
    }
}
