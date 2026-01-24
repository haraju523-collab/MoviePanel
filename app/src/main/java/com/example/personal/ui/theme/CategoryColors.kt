package com.example.personal.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.personal.models.Categories

fun getCategoryColor(category: String): Color {
    return when (category) {
        Categories.ALL -> PrimaryBlueStatic
        Categories.TERABOX -> CategoryHollywood // Use cyan color for TeraBox
        Categories.HOLLYWOOD -> CategoryHollywood
        Categories.BOLLYWOOD -> CategoryBollywood
        Categories.ANIME -> CategoryAnime
        Categories.WEB_SERIES -> CategoryWebSeries
        Categories.K_DRAMA -> CategoryKDrama
        Categories.SPORTS -> CategorySports
        Categories.TELEGRAM -> CategoryTelegram
        else -> PrimaryBlueStatic
    }
}
