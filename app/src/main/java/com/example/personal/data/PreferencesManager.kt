package com.example.personal.data

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    
    private const val PREFS_NAME = "app_preferences"
    private const val KEY_THEME = "theme"
    private const val KEY_DOWNLOAD_LOCATION = "download_location"
    private const val KEY_DEFAULT_VIEW = "default_view"
    private const val KEY_SMART_FILTERS = "smart_filters"
    
    private lateinit var prefs: SharedPreferences
    
    // Theme change listener for reactive updates
    private var themeChangeListeners = mutableListOf<() -> Unit>()
    
    fun addThemeChangeListener(listener: () -> Unit) {
        themeChangeListeners.add(listener)
    }
    
    fun removeThemeChangeListener(listener: () -> Unit) {
        themeChangeListeners.remove(listener)
    }
    
    private fun notifyThemeChanged() {
        themeChangeListeners.forEach { it.invoke() }
    }
    
    enum class Theme {
        DARK, LIGHT, SYSTEM
    }
    
    enum class ViewMode {
        GRID, LIST
    }
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Theme
    fun getTheme(): Theme {
        val themeName = prefs.getString(KEY_THEME, Theme.DARK.name) ?: Theme.DARK.name
        return try {
            Theme.valueOf(themeName)
        } catch (e: Exception) {
            Theme.DARK
        }
    }
    
    fun setTheme(theme: Theme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        notifyThemeChanged()
    }
    
    // Download Location
    fun getDownloadLocation(): String {
        return prefs.getString(KEY_DOWNLOAD_LOCATION, "App Downloads") ?: "App Downloads"
    }
    
    fun setDownloadLocation(location: String) {
        prefs.edit().putString(KEY_DOWNLOAD_LOCATION, location).apply()
    }
    
    // Default View Mode
    fun getDefaultViewMode(): ViewMode {
        val modeName = prefs.getString(KEY_DEFAULT_VIEW, ViewMode.LIST.name) ?: ViewMode.LIST.name
        return try {
            ViewMode.valueOf(modeName)
        } catch (e: Exception) {
            ViewMode.LIST
        }
    }
    
    fun setDefaultViewMode(mode: ViewMode) {
        prefs.edit().putString(KEY_DEFAULT_VIEW, mode.name).apply()
    }
    
    // Smart Filters
    fun getSmartFilters(): Set<String> {
        return prefs.getStringSet(KEY_SMART_FILTERS, emptySet()) ?: emptySet()
    }
    
    fun setSmartFilters(filters: Set<String>) {
        prefs.edit().putStringSet(KEY_SMART_FILTERS, filters).apply()
    }
    
    fun toggleSmartFilter(filter: String): Set<String> {
        val current = getSmartFilters().toMutableSet()
        if (current.contains(filter)) {
            current.remove(filter)
        } else {
            current.add(filter)
        }
        setSmartFilters(current)
        return current
    }
}
