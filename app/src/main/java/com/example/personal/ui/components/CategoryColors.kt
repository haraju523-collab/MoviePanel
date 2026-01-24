package com.example.personal.ui.components

import androidx.compose.ui.graphics.Color
import com.example.personal.models.Categories
import com.example.personal.ui.theme.CategoryAnime
import com.example.personal.ui.theme.CategoryBollywood
import com.example.personal.ui.theme.CategoryHollywood
import com.example.personal.ui.theme.CategoryKDrama
import com.example.personal.ui.theme.CategorySports
import com.example.personal.ui.theme.CategoryWebSeries
import com.example.personal.ui.theme.PrimaryBlueStatic

fun getCategoryColor(category: String): Color {
    return when (category) {
        Categories.ALL -> PrimaryBlueStatic
        Categories.HOLLYWOOD -> CategoryHollywood
        Categories.BOLLYWOOD -> CategoryBollywood
        Categories.ANIME -> CategoryAnime
        Categories.WEB_SERIES -> CategoryWebSeries
        Categories.K_DRAMA -> CategoryKDrama
        Categories.SPORTS -> CategorySports
        else -> PrimaryBlueStatic
    }
}
