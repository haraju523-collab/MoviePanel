package com.example.personal.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personal.WebViewActivity
import com.example.personal.data.SiteRepository
import com.example.personal.models.Categories
import com.example.personal.ui.components.EmptyStateView
import com.example.personal.ui.components.WebsiteCard
import com.example.personal.ui.theme.*
import com.example.personal.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    
    val allSites = remember(refreshKey) { SiteRepository.getAllSites() }
    val favoriteSites = remember(refreshKey) { SiteRepository.getFavoriteSites().take(3) }
    
    // Get greeting based on time
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    
    val categorySites = remember(selectedCategory) {
        if (selectedCategory == null) {
            allSites
        } else {
            allSites.filter { it.category == selectedCategory }
        }
    }
    
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground,
                            DarkBackgroundEnd
                        )
                    )
                ),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Greeting Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "$greeting 🍿",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ready for a movie?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
            
            // Big CTA Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val buttonScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isPressed) 0.97f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "ctaScale"
                    )
                    
                    Button(
                        onClick = {
                            // Start browsing
                            if (allSites.isNotEmpty()) {
                                val firstSite = allSites.first()
                                SiteRepository.addToRecent(firstSite.id)
                                val intent = Intent(context, com.example.personal.WebViewActivity::class.java).apply {
                                    putExtra("url", firstSite.url)
                                    putExtra("title", firstSite.name)
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .scale(buttonScale)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = PrimaryBlue.copy(alpha = 0.5f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        interactionSource = interactionSource
                    ) {
                        Text(
                            text = "Start Watching",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Category Cards
            item {
                Text(
                    text = "Browse by Category",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Hollywood", "Bollywood", "Anime", "Web Series", "KDrama", "Telegram").forEach { category ->
                        CategoryCard(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Favorites Preview
            if (favoriteSites.isNotEmpty() && selectedCategory == null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ Your Favorites",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { /* Navigate to favorites */ }) {
                            Text(
                                text = "View all →",
                                color = PrimaryBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(favoriteSites, key = { it.id }) { site ->
                            FavoritePreviewCard(
                                site = site,
                                onClick = {
                                    SiteRepository.addToRecent(site.id)
                                    val intent = Intent(context, com.example.personal.WebViewActivity::class.java).apply {
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
            
            // Sites List
            item {
                Text(
                    text = if (selectedCategory != null) selectedCategory!! else "All Streaming Sites",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (categorySites.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            isSearch = false,
                            searchQuery = "",
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            } else {
                items(categorySites, key = { it.id }) { site ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        WebsiteCard(
                            site = site,
                            isFavorite = SiteRepository.isFavorite(site.id),
                            onClick = {
                                SiteRepository.addToRecent(site.id)
                                val intent = Intent(context, com.example.personal.WebViewActivity::class.java).apply {
                                    putExtra("url", site.url)
                                    putExtra("title", site.name)
                                }
                                context.startActivity(intent)
                            },
                            onFavoriteClick = {
                                SiteRepository.toggleFavorite(site.id)
                                refreshKey++
                            },
                            onRemoveClick = null,
                            onShareClick = null
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(category)
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) categoryColor.copy(alpha = 0.2f) else DarkCard,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "categoryBg"
    )
    val textColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) categoryColor else TextSecondary,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "categoryText"
    )
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clickable { onClick() }
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isSelected) categoryColor.copy(alpha = 0.3f) else Color.Transparent
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (category) {
                    "Hollywood" -> "🎬"
                    "Bollywood" -> "🎭"
                    "Anime" -> "🎨"
                    "Web Series" -> "📺"
                    "KDrama" -> "🇰🇷"
                    "Telegram" -> "✈️"
                    else -> "🎥"
                },
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FavoritePreviewCard(
    site: com.example.personal.models.MovieSite,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(site.category)
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = SecondaryCoral.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SecondaryCoral.copy(alpha = 0.1f),
                                Color.Transparent
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = site.iconEmoji,
                        fontSize = 28.sp
                    )
                }
                
                Column {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = site.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = categoryColor
                    )
                }
            }
        }
    }
}

enum class SortOption {
    NAME, RECENTLY_USED, MOST_USED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    selectedCategories: Set<String>,
    sortOption: SortOption,
    onCategoriesChanged: (Set<String>) -> Unit,
    onSortChanged: (SortOption) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    val categories = Categories.list.filter { it != Categories.ALL }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onReset) {
                    Text("Reset", color = PrimaryBlue)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Category filter
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowCategories.forEach { category ->
                            FilterChip(
                                selected = selectedCategories.contains(category),
                                onClick = {
                                    val newSet = selectedCategories.toMutableSet()
                                    if (newSet.contains(category)) {
                                        newSet.remove(category)
                                    } else {
                                        newSet.add(category)
                                    }
                                    onCategoriesChanged(newSet)
                                },
                                label = { Text(category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty space if odd number
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sort options
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            SortOption.values().forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sortOption == option,
                        onClick = { onSortChanged(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (option) {
                            SortOption.NAME -> "Name A-Z"
                            SortOption.RECENTLY_USED -> "Recently Used"
                            SortOption.MOST_USED -> "Most Used"
                        },
                        color = TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Apply button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Text("Apply", color = DarkBackground)
            }
        }
    }
}
