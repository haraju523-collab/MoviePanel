package com.example.personal.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light theme colors
private val LightBackground = Color(0xFFFFFFFF)
private val LightSurface = Color(0xFFF5F5F5)
private val LightCard = Color(0xFFFFFFFF)
private val LightTextPrimary = Color(0xFF000000)
private val LightTextSecondary = Color(0xFF666666)
private val LightTextTertiary = Color(0xFF999999)

// Static dark theme colors (for use in Theme.kt)
private val StaticDarkBackground = Color(0xFF000000)
private val StaticDarkBackgroundEnd = Color(0xFF0E0E11)
private val StaticDarkSurface = Color(0xFF0A0A0A)
private val StaticDarkCard = Color(0xFF1A1A1A)
private val StaticTextPrimary = Color(0xFFFFFFFF)
private val StaticTextSecondary = Color(0xFFCCCCCC)
private val StaticTextTertiary = Color(0xFF999999)
private val StaticPrimaryBlue = Color(0xFFE50914)
private val StaticSecondaryCoral = Color(0xFFFF1E56)
private val StaticPrimaryBlueDark = Color(0xFF8B0000)
private val StaticSecondaryCoralVariant = Color(0xFFFF006E)
private val StaticAccentPurple = Color(0xFF9D4EDD)
private val StaticGlassBorder = Color(0x33FFFFFF)

private val MoviePanelDarkColorScheme = darkColorScheme(
    primary = StaticPrimaryBlue,
    onPrimary = StaticDarkBackground,
    primaryContainer = StaticPrimaryBlueDark,
    onPrimaryContainer = StaticTextPrimary,
    secondary = StaticSecondaryCoral,
    onSecondary = StaticDarkBackground,
    secondaryContainer = StaticSecondaryCoralVariant,
    onSecondaryContainer = StaticTextPrimary,
    tertiary = StaticAccentPurple,
    onTertiary = StaticTextPrimary,
    background = StaticDarkBackground,
    onBackground = StaticTextPrimary,
    surface = StaticDarkSurface,
    onSurface = StaticTextPrimary,
    surfaceVariant = StaticDarkCard,
    onSurfaceVariant = StaticTextSecondary,
    outline = StaticGlassBorder,
    outlineVariant = StaticTextTertiary
)

private val MoviePanelLightColorScheme = lightColorScheme(
    primary = StaticPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = StaticPrimaryBlueDark,
    onPrimaryContainer = LightTextPrimary,
    secondary = StaticSecondaryCoral,
    onSecondary = Color.White,
    secondaryContainer = StaticSecondaryCoralVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = StaticAccentPurple,
    onTertiary = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCCCCCC),
    outlineVariant = LightTextTertiary
)

// Local composition for theme colors
data class AppColors(
    val background: Color,
    val backgroundEnd: Color,
    val surface: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primaryBlue: Color,
    val secondaryCoral: Color
)

val LocalAppColors = compositionLocalOf<AppColors> {
    error("No AppColors provided")
}

private val DarkAppColors = AppColors(
    background = StaticDarkBackground,
    backgroundEnd = StaticDarkBackgroundEnd,
    surface = StaticDarkSurface,
    card = StaticDarkCard,
    textPrimary = StaticTextPrimary,
    textSecondary = StaticTextSecondary,
    textTertiary = StaticTextTertiary,
    primaryBlue = StaticPrimaryBlue,
    secondaryCoral = StaticSecondaryCoral
)

private val LightAppColors = AppColors(
    background = LightBackground,
    backgroundEnd = Color(0xFFE0E0E0),
    surface = LightSurface,
    card = LightCard,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    primaryBlue = StaticPrimaryBlue,
    secondaryCoral = StaticSecondaryCoral
)

@Composable
fun MoviePanelTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    
    // Initialize PreferencesManager if needed
    LaunchedEffect(Unit) {
        try {
            com.example.personal.data.PreferencesManager.init(context)
        } catch (e: Exception) {
            // Already initialized
        }
    }
    
    // Get theme preference - make it reactive
    var themePreference by remember { mutableStateOf(com.example.personal.data.PreferencesManager.getTheme()) }
    
    // Listen for theme changes
    DisposableEffect(Unit) {
        val listener: () -> Unit = { themePreference = com.example.personal.data.PreferencesManager.getTheme() }
        com.example.personal.data.PreferencesManager.addThemeChangeListener(listener)
        onDispose {
            com.example.personal.data.PreferencesManager.removeThemeChangeListener(listener)
        }
    }
    
    // Determine if dark theme should be used
    val useDarkTheme = when (themePreference) {
        com.example.personal.data.PreferencesManager.Theme.DARK -> true
        com.example.personal.data.PreferencesManager.Theme.LIGHT -> false
        com.example.personal.data.PreferencesManager.Theme.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (useDarkTheme) MoviePanelDarkColorScheme else MoviePanelLightColorScheme
    val appColors = if (useDarkTheme) DarkAppColors else LightAppColors
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (useDarkTheme) StaticDarkBackground else LightBackground
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Legacy theme for compatibility
@Composable
fun PersonalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MoviePanelTheme(content = content)
}