package com.example.personal.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Theme-aware colors - use LocalAppColors for dynamic theme support
// These will automatically switch between dark and light based on theme preference
val DarkBackground: Color @Composable get() = LocalAppColors.current.background
val DarkBackgroundEnd: Color @Composable get() = LocalAppColors.current.backgroundEnd
val DarkSurface: Color @Composable get() = LocalAppColors.current.surface
val DarkCard: Color @Composable get() = LocalAppColors.current.card
val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
val TextTertiary: Color @Composable get() = LocalAppColors.current.textTertiary
val PrimaryBlue: Color @Composable get() = LocalAppColors.current.primaryBlue
val SecondaryCoral: Color @Composable get() = LocalAppColors.current.secondaryCoral

// Static colors (category colors, etc. - same for both themes)
val DarkCardElevated = Color(0xFF242424) // Elevated cards
val DarkCardHover = Color(0xFF2A2A2A)    // Hover state
val GlassBackground = Color(0x1AFFFFFF)  // Glass effect background
val GlassBorderColor = Color(0x26FFFFFF) // Glass border (15% opacity)
val GlassBorder = Color(0x33FFFFFF)

// Primary - Premium Blue (Netflix/Disney+ style) - static
// Static version for use in non-composable contexts (like CategoryColors)
val PrimaryBlueStatic = Color(0xFFE50914)
val PrimaryBlueVariant = Color(0xFFB20710)
val PrimaryBlueDark = Color(0xFF8B0000)

// Secondary - Premium Coral (for favorites) - static
val SecondaryCoralVariant = Color(0xFFFF006E)

// Accent Colors - static
val AccentPurple = Color(0xFF9D4EDD)
val AccentGold = Color(0xFFFFD93D)
val AccentGreen = Color(0xFF6BCB77)

// Category Badge Colors - static (same for both themes)
val CategoryHollywood = Color(0xFF00D4FF)
val CategoryBollywood = Color(0xFFFF6B6B)
val CategoryAnime = Color(0xFF9D4EDD)
val CategoryWebSeries = Color(0xFF6BCB77)
val CategoryKDrama = Color(0xFFFFD93D)
val CategorySports = Color(0xFFFF8C42)
val CategoryTelegram = Color(0xFF0088CC) // Telegram brand blue

// Gradients components - static
val GradientStart = Color(0xFF00D4FF)
val GradientEnd = Color(0xFF9D4EDD)

// Shield/Protection indicator - static
val ShieldGreen = Color(0xFF4CAF50)
val ShieldActive = Color(0xFF00E676)
