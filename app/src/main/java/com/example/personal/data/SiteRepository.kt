package com.example.personal.data

import android.content.Context
import android.content.SharedPreferences
import com.example.personal.models.Categories
import com.example.personal.models.MovieSite
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SiteRepository {
    
    private const val PREFS_NAME = "movie_panel_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENT = "recent_sites"
    private const val KEY_CUSTOM_SITES = "custom_sites"
    private const val KEY_REMOVED_SITES = "removed_sites"
    private const val KEY_SITE_STATS = "site_stats"
    
    private lateinit var prefs: SharedPreferences
    private val favorites = mutableSetOf<String>()
    private val recentSites = mutableListOf<String>()
    private val customSites = mutableListOf<MovieSite>()
    private val removedSites = mutableSetOf<String>()
    private val siteStats = mutableMapOf<String, Pair<Long, Int>>() // siteId -> (lastAccessed, accessCount)
    private val gson = Gson()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFavorites()
        loadRecent()
        loadCustomSites()
        loadRemovedSites()
        loadSiteStats()
    }
    
    private fun loadFavorites() {
        val saved = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        favorites.clear()
        favorites.addAll(saved)
    }
    
    private fun loadRecent() {
        val saved = prefs.getString(KEY_RECENT, "") ?: ""
        recentSites.clear()
        if (saved.isNotEmpty()) {
            recentSites.addAll(saved.split(",").take(10))
        }
    }
    
    private fun loadCustomSites() {
        val saved = prefs.getString(KEY_CUSTOM_SITES, null)
        customSites.clear()
        if (saved != null) {
            try {
                val type = object : TypeToken<List<MovieSite>>() {}.type
                val sites: List<MovieSite> = gson.fromJson(saved, type)
                customSites.addAll(sites)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRemovedSites() {
        val saved = prefs.getStringSet(KEY_REMOVED_SITES, emptySet()) ?: emptySet()
        removedSites.clear()
        removedSites.addAll(saved)
    }

    private fun saveRemovedSites() {
        prefs.edit().putStringSet(KEY_REMOVED_SITES, removedSites).apply()
    }

    private fun loadSiteStats() {
        val saved = prefs.getString(KEY_SITE_STATS, null)
        siteStats.clear()
        if (saved != null) {
            try {
                val type = object : TypeToken<Map<String, List<*>>>() {}.type
                val map: Map<String, List<*>> = gson.fromJson(saved, type)
                map.forEach { (id, values) ->
                    if (values.size >= 2) {
                        val lastAccessed = (values[0] as? Double)?.toLong() ?: 0L
                        val accessCount = (values[1] as? Double)?.toInt() ?: 0
                        siteStats[id] = Pair(lastAccessed, accessCount)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveSiteStats() {
        val map = siteStats.mapValues { listOf(it.value.first, it.value.second) }
        val json = gson.toJson(map)
        prefs.edit().putString(KEY_SITE_STATS, json).apply()
    }
    
    private fun saveCustomSites() {
        val json = gson.toJson(customSites)
        prefs.edit().putString(KEY_CUSTOM_SITES, json).apply()
    }
    
    fun addCustomSite(site: MovieSite) {
        customSites.add(0, site)
        saveCustomSites()
    }
    
    fun removeCustomSite(siteId: String) {
        customSites.removeAll { it.id == siteId }
        saveCustomSites()
    }
    
    fun getCustomSites(): List<MovieSite> =
        customSites.filterNot { removedSites.contains(it.id) }

    /**
     * Remove a site (built-in or custom) from the UI.
     *
     * - For custom sites, we delete the stored entry.
     * - For built-in default sites, we mark them as "removed" so they are hidden
     *   everywhere (lists, search, favorites, recent).
     */
    fun removeSite(siteId: String) {
        // Remove from custom collection if present
        customSites.removeAll { it.id == siteId }
        saveCustomSites()

        // Mark as removed globally
        removedSites.add(siteId)
        saveRemovedSites()
    }
    
    fun toggleFavorite(siteId: String) {
        if (favorites.contains(siteId)) {
            favorites.remove(siteId)
        } else {
            favorites.add(siteId)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }
    
    fun isFavorite(siteId: String): Boolean = favorites.contains(siteId)
    
    fun addToRecent(siteId: String) {
        recentSites.remove(siteId)
        recentSites.add(0, siteId)
        if (recentSites.size > 10) {
            recentSites.removeAt(recentSites.size - 1)
        }
        prefs.edit().putString(KEY_RECENT, recentSites.joinToString(",")).apply()
        
        // Track access statistics
        val now = System.currentTimeMillis()
        val currentStats = siteStats[siteId] ?: Pair(0L, 0)
        siteStats[siteId] = Pair(now, currentStats.second + 1)
        saveSiteStats()
    }
    
    private fun enrichSiteWithStats(site: MovieSite): MovieSite {
        val stats = siteStats[site.id] ?: Pair(0L, 0)
        return site.copy(
            lastAccessed = stats.first,
            accessCount = stats.second
        )
    }
    
    fun getRecentSites(): List<MovieSite> {
        val allAvailable = getAllSites()
        return recentSites.mapNotNull { id -> allAvailable.find { it.id == id } }
    }
    
    fun getFavoriteSites(): List<MovieSite> {
        val allAvailable = getAllSites()
        return allAvailable.filter { favorites.contains(it.id) }
    }
    
    fun getAllSites(): List<MovieSite> =
        (customSites + defaultSites)
            .filterNot { removedSites.contains(it.id) }
            .map { enrichSiteWithStats(it) }
    
    fun getSitesByCategory(category: String): List<MovieSite> {
        val all = getAllSites()
        return if (category == Categories.ALL) {
            all
        } else {
            all.filter { it.category == category }
        }
    }
    
    fun searchSites(query: String): List<MovieSite> {
        if (query.isBlank()) return getAllSites()
        val lowerQuery = query.lowercase()
        return getAllSites().filter { 
            it.name.lowercase().contains(lowerQuery) || 
            it.category.lowercase().contains(lowerQuery) ||
            it.url.lowercase().contains(lowerQuery)
        }
    }
    
    fun getSiteStats(siteId: String): Pair<Long, Int> {
        return siteStats[siteId] ?: Pair(0L, 0)
    }
    
    // Comprehensive list of streaming sites
    private val defaultSites = listOf(
        // Hollywood Sites
        MovieSite(
            id = "movies4u",
            name = "Movies4U",
            url = "https://movies4u.com",
            category = Categories.HOLLYWOOD,
            description = "Latest Hollywood releases in HD",
            iconEmoji = "🎬"
        ),
        MovieSite(
            id = "soap2day",
            name = "Soap2Day",
            url = "https://soap2day.to",
            category = Categories.HOLLYWOOD,
            description = "Free movies and TV shows",
            iconEmoji = "🎥"
        ),
        MovieSite(
            id = "fmovies",
            name = "FMovies",
            url = "https://fmovies.to",
            category = Categories.HOLLYWOOD,
            description = "Watch movies online for free",
            iconEmoji = "🍿"
        ),
        MovieSite(
            id = "123movies",
            name = "123Movies",
            url = "https://123movies.com",
            category = Categories.HOLLYWOOD,
            description = "Stream thousands of movies",
            iconEmoji = "🎞️"
        ),
        MovieSite(
            id = "yesmovies",
            name = "YesMovies",
            url = "https://yesmovies.to",
            category = Categories.HOLLYWOOD,
            description = "HD movies and series",
            iconEmoji = "✨"
        ),
        MovieSite(
            id = "teraplay",
            name = "TeraPlay",
            url = "https://teraplay.in",
            category = Categories.HOLLYWOOD,
            description = "TeraBox Video Player & Downloader",
            iconEmoji = "▶️"
        ),
        
        // Bollywood Sites
        MovieSite(
            id = "bollyflix",
            name = "BollyFlix",
            url = "https://bollyflix.com",
            category = Categories.BOLLYWOOD,
            description = "Latest Bollywood movies",
            iconEmoji = "🇮🇳"
        ),
        MovieSite(
            id = "filmyzilla",
            name = "Filmyzilla",
            url = "https://filmyzilla.com",
            category = Categories.BOLLYWOOD,
            description = "Hindi dubbed movies",
            iconEmoji = "🎭"
        ),
        MovieSite(
            id = "mp4moviez",
            name = "MP4Moviez",
            url = "https://mp4moviez.com",
            category = Categories.BOLLYWOOD,
            description = "Download Bollywood movies",
            iconEmoji = "💫"
        ),
        MovieSite(
            id = "vegamovies",
            name = "VegaMovies",
            url = "https://vegamovies.com",
            category = Categories.BOLLYWOOD,
            description = "Multi-language content",
            iconEmoji = "🌟"
        ),
        
        // Anime Sites
        MovieSite(
            id = "9anime",
            name = "9Anime",
            url = "https://9anime.to",
            category = Categories.ANIME,
            description = "Popular anime streaming",
            iconEmoji = "🎌"
        ),
        MovieSite(
            id = "gogoanime",
            name = "GogoAnime",
            url = "https://gogoanime.pe",
            category = Categories.ANIME,
            description = "Watch anime online",
            iconEmoji = "🐉"
        ),
        MovieSite(
            id = "crunchyroll",
            name = "Crunchyroll",
            url = "https://crunchyroll.com",
            category = Categories.ANIME,
            description = "Official anime platform",
            iconEmoji = "🍥"
        ),
        MovieSite(
            id = "zoro",
            name = "Zoro.to",
            url = "https://zoro.to",
            category = Categories.ANIME,
            description = "Ad-free anime streaming",
            iconEmoji = "⚔️"
        ),
        
        // Web Series
        MovieSite(
            id = "seriesflix",
            name = "SeriesFlix",
            url = "https://seriesflix.com",
            category = Categories.WEB_SERIES,
            description = "Binge-worthy web series",
            iconEmoji = "📺"
        ),
        MovieSite(
            id = "lookmovie",
            name = "LookMovie",
            url = "https://lookmovie2.to",
            category = Categories.WEB_SERIES,
            description = "TV series and movies",
            iconEmoji = "👀"
        ),
        MovieSite(
            id = "cmovies",
            name = "CMovies",
            url = "https://cmovies.so",
            category = Categories.WEB_SERIES,
            description = "Complete series library",
            iconEmoji = "🎪"
        ),
        
        // K-Drama
        MovieSite(
            id = "dramacool",
            name = "DramaCool",
            url = "https://dramacool.so",
            category = Categories.K_DRAMA,
            description = "Korean drama streaming",
            iconEmoji = "🇰🇷"
        ),
        MovieSite(
            id = "kissasian",
            name = "KissAsian",
            url = "https://kissasian.li",
            category = Categories.K_DRAMA,
            description = "Asian dramas & movies",
            iconEmoji = "💋"
        ),
        MovieSite(
            id = "myasiantv",
            name = "MyAsianTV",
            url = "https://myasiantv.cc",
            category = Categories.K_DRAMA,
            description = "Watch K-dramas online",
            iconEmoji = "🌸"
        ),
        
        // Sports
        MovieSite(
            id = "cricfree",
            name = "CricFree",
            url = "https://cricfree.io",
            category = Categories.SPORTS,
            description = "Live cricket streaming",
            iconEmoji = "🏏"
        ),
        MovieSite(
            id = "sportsbay",
            name = "SportsBay",
            url = "https://sportsbay.org",
            category = Categories.SPORTS,
            description = "All sports live",
            iconEmoji = "⚽"
        ),
        MovieSite(
            id = "buffstreams",
            name = "BuffStreams",
            url = "https://buffstreams.tv",
            category = Categories.SPORTS,
            description = "HD sports streaming",
            iconEmoji = "🏆"
        ),
        
        // Telegram Channels (Stream via Telegram Web - no download needed!)
        MovieSite(
            id = "telegram_web",
            name = "Telegram Web",
            url = "https://web.telegram.org",
            category = Categories.TELEGRAM,
            description = "Access Telegram channels & stream videos",
            iconEmoji = "✈️"
        ),
        MovieSite(
            id = "telegram_movies",
            name = "Movies Channel",
            url = "https://web.telegram.org/k/#@movies_channel",
            category = Categories.TELEGRAM,
            description = "Popular movie streaming channel",
            iconEmoji = "🎬"
        ),
        MovieSite(
            id = "telegram_bollywood",
            name = "Bollywood Movies",
            url = "https://web.telegram.org/k/#@bollywood_movies",
            category = Categories.TELEGRAM,
            description = "Latest Bollywood releases",
            iconEmoji = "🇮🇳"
        ),
        MovieSite(
            id = "telegram_series",
            name = "Web Series Hub",
            url = "https://web.telegram.org/k/#@webseries_hub",
            category = Categories.TELEGRAM,
            description = "TV series & web shows",
            iconEmoji = "📺"
        ),
        MovieSite(
            id = "telegram_anime",
            name = "Anime Streaming",
            url = "https://web.telegram.org/k/#@anime_streaming",
            category = Categories.TELEGRAM,
            description = "Anime & manga content",
            iconEmoji = "🎌"
        )
    )
}
