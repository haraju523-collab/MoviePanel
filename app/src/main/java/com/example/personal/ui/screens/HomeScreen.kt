package com.example.personal.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personal.WebViewActivity
import com.example.personal.TeraBoxPlayerActivity
import com.example.personal.data.SiteRepository
import com.example.personal.ui.components.WebsiteCard
import com.example.personal.ui.theme.*
import com.example.personal.ui.navigation.Screen

enum class ViewMode { GRID, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDownloads: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToExplore: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedQuickAction by remember { mutableStateOf("Favorites") }
    var refreshKey by remember { mutableIntStateOf(0) }
    
    // Get data for Cinematic Hub
    val allSites = remember(refreshKey) { SiteRepository.getAllSites() }
    val favoriteSites = remember(refreshKey) { SiteRepository.getFavoriteSites() }
    val recentSites = remember(refreshKey) { SiteRepository.getRecentSites().take(5) }
    val trendingSites = remember(refreshKey) { 
        // Sites sorted by access count (most used)
        allSites.sortedByDescending { it.accessCount }.take(6)
    }
    
    // Filter based on quick action
    val displayedSites = remember(selectedQuickAction, refreshKey) {
        when (selectedQuickAction) {
            "Movies" -> allSites.filter { it.category == "Hollywood" || it.category == "Bollywood" }
            "Series" -> allSites.filter { it.category == "Web Series" || it.category == "KDrama" }
            "Websites" -> allSites
            "Favorites" -> favoriteSites
            else -> favoriteSites
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // Cinematic Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DarkBackground,
                                DarkBackgroundEnd,
                                DarkBackgroundEnd.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                // Profile Button overlay
                IconButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(DarkCard.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = TextPrimary
                    )
                }

                // Hero content overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "Tonight's Picks",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Premium streaming, ad-free experience",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkBackgroundEnd,
                            DarkBackground
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Quick Action Pills
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("🎥 Movies", "📺 Series", "🌐 Websites", "❤️ Favorites").forEach { action ->
                            val isSelected = when (action) {
                                "🎥 Movies" -> selectedQuickAction == "Movies"
                                "📺 Series" -> selectedQuickAction == "Series"
                                "🌐 Websites" -> selectedQuickAction == "Websites"
                                "❤️ Favorites" -> selectedQuickAction == "Favorites"
                                else -> false
                            }
                            
                            val actionKey = action.substringAfter(" ")
                            QuickActionPill(
                                text = action,
                                isSelected = isSelected,
                                onClick = { selectedQuickAction = actionKey }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Trending Sites Carousel
                
                // Trending Sites Carousel
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clickable {
                                context.startActivity(Intent(context, TeraBoxPlayerActivity::class.java))
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            CategoryHollywood.copy(alpha = 0.2f),
                                            AccentPurple.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            CategoryHollywood.copy(alpha = 0.5f),
                                            AccentPurple.copy(alpha = 0.3f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CategoryHollywood.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            tint = CategoryHollywood,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "📦 TeraBox Player",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Paste link & stream instantly",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = CategoryHollywood,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                // Recently Visited
                if (recentSites.isNotEmpty() && selectedQuickAction == "Favorites") {
                    item {
                        SectionHeaderWithAction(
                            title = "⏱ Recently Visited",
                            onSeeAll = null,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            items(recentSites, key = { it.id }) { site ->
                                RecentSiteCard(
                                    site = site,
                                    onClick = {
                                        SiteRepository.addToRecent(site.id)
                                        val intent = Intent(context, WebViewActivity::class.java).apply {
                                            putExtra("url", site.url)
                                            putExtra("title", site.name)
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                
                // Main Content Section
                item {
                    SectionHeaderWithAction(
                        title = when (selectedQuickAction) {
                            "Movies" -> "🎬 Movies"
                            "Series" -> "📺 Series"
                            "Websites" -> "🌐 All Sites"
                            else -> "❤️ Your Favorites"
                        },
                        onSeeAll = null,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (displayedSites.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No sites found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try a different category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextTertiary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(displayedSites, key = { it.id }) { site ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(400)) + slideInVertically(
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialOffsetY = { it / 2 }
                            ),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                renderSiteCard(context, site, { refreshKey++ })
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue else DarkCard,
        animationSpec = tween(300),
        label = "pillBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextSecondary,
        animationSpec = tween(300),
        label = "pillText"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .shadow(
                elevation = if (isSelected) 8.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isSelected) PrimaryBlue.copy(alpha = 0.4f) else Color.Transparent
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SectionHeaderWithAction(
    title: String,
    onSeeAll: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = "See All →",
                    color = PrimaryBlue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TrendingSiteCard(
    site: com.example.personal.models.MovieSite,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val categoryColor = getCategoryColor(site.category)
    val isFavorite = SiteRepository.isFavorite(site.id)
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = categoryColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                categoryColor.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon and favorite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(categoryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = site.iconEmoji,
                            fontSize = 24.sp
                        )
                    }
                    
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) SecondaryCoral else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Site info
                Column {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(categoryColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "HD",
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (site.accessCount > 10) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚡ Fast",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSiteCard(
    site: com.example.personal.models.MovieSite,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(site.category)
    
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(140.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = categoryColor.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = site.iconEmoji,
                    fontSize = 28.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = site.name,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun renderSiteCard(
    context: android.content.Context,
    site: com.example.personal.models.MovieSite,
    onRefresh: () -> Unit,
    isGrid: Boolean = false
) {
    WebsiteCard(
        site = site,
        isFavorite = com.example.personal.data.SiteRepository.isFavorite(site.id),
        onClick = {
            com.example.personal.data.SiteRepository.addToRecent(site.id)
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra("url", site.url)
                putExtra("title", site.name)
            }
            context.startActivity(intent)
        },
        onFavoriteClick = {
            com.example.personal.data.SiteRepository.toggleFavorite(site.id)
            onRefresh()
        },
        onRemoveClick = null, // Hide delete button for simplicity
        onShareClick = null // Hide share button for simplicity
    )
}
