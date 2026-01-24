package com.example.personal.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.personal.DownloadsActivity
import com.example.personal.ui.screens.*
import com.example.personal.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Browser : Screen("browser", "Browser", Icons.Default.Public)
    object Downloads : Screen("downloads", "Downloads", Icons.Default.Download)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val context = LocalContext.current
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkCard,
                contentColor = TextPrimary,
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    spotColor = PrimaryBlue.copy(alpha = 0.1f)
                )
            ) {
                listOf(Screen.Home, Screen.Browser, Screen.Downloads, Screen.Favorites).forEach { screen ->
                    val isSelected = selectedScreen == screen
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "iconScale"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryBlue else TextTertiary.copy(alpha = 0.6f),
                        animationSpec = tween(300),
                        label = "iconColor"
                    )
                    
                    NavigationBarItem(
                        icon = {
                            Box(
                                modifier = Modifier.scale(iconScale),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = androidx.compose.foundation.shape.CircleShape,
                                                spotColor = PrimaryBlue.copy(alpha = 0.4f)
                                            )
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        PrimaryBlue.copy(alpha = 0.2f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = isSelected,
                        onClick = { selectedScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.15f),
                            unselectedIconColor = TextTertiary.copy(alpha = 0.6f),
                            unselectedTextColor = TextTertiary.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedScreen) {
                is Screen.Home -> HomeScreen(
                    onNavigateToDownloads = {
                        selectedScreen = Screen.Downloads
                    },
                    onNavigateToProfile = {
                        selectedScreen = Screen.Profile
                    },
                    onNavigateToExplore = {
                        // Removed explore
                    }
                )
                is Screen.Browser -> {
                    // Reset to Home so we don't get stuck on this tab when coming back
                    SideEffect { selectedScreen = Screen.Home }
                    LaunchedEffect(Unit) {
                        context.startActivity(android.content.Intent(context, com.example.personal.WebViewActivity::class.java))
                    }
                }
                is Screen.Downloads -> {
                    // Reset to Home
                    SideEffect { selectedScreen = Screen.Home }
                    LaunchedEffect(Unit) {
                        context.startActivity(android.content.Intent(context, DownloadsActivity::class.java))
                    }
                }
                is Screen.Favorites -> FavoritesScreen()
                is Screen.Profile -> ProfileScreen()
            }
        }
    }
}
