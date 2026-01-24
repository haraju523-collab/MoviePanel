package com.example.personal.models

data class MovieSite(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val description: String,
    val iconEmoji: String,
    val isFavorite: Boolean = false,
    val lastAccessed: Long = 0L,
    val accessCount: Int = 0
)

object Categories {
    const val ALL = "All"
    const val TERABOX = "TeraBox"
    const val HOLLYWOOD = "Hollywood"
    const val BOLLYWOOD = "Bollywood"
    const val ANIME = "Anime"
    const val WEB_SERIES = "Web Series"
    const val K_DRAMA = "K-Drama"
    const val SPORTS = "Sports"
    const val TELEGRAM = "Telegram"
    
    val list = listOf(ALL, TERABOX, HOLLYWOOD, BOLLYWOOD, ANIME, WEB_SERIES, K_DRAMA, SPORTS, TELEGRAM)
}
