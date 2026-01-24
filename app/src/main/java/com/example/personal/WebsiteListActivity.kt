package com.example.personal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personal.data.SiteRepository
import com.example.personal.models.Categories
import com.example.personal.ui.components.AddSiteDialog
import com.example.personal.ui.components.CategoryChips
import com.example.personal.ui.components.EmptyStateView
import com.example.personal.ui.components.SectionHeader
import com.example.personal.ui.components.StatBadge
import com.example.personal.ui.components.WebsiteCard
import com.example.personal.ui.navigation.MainNavigation
import com.example.personal.ui.theme.*

import androidx.activity.enableEdgeToEdge

enum class ViewMode { GRID, LIST }

class WebsiteListActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SiteRepository.init(this)
        
        setContent {
            MoviePanelTheme {
                MainNavigation()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviePanelScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(Categories.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Trigger recomposition when favorites/sites change
    var refreshKey by remember { mutableIntStateOf(0) }
    
    val allSites = remember(refreshKey) { SiteRepository.getAllSites() }
    val favoritesCount = remember(refreshKey) { SiteRepository.getFavoriteSites().size }
    val customSitesCount = remember(refreshKey) { SiteRepository.getCustomSites().size }
    
    val sites = remember(selectedCategory, searchQuery, refreshKey) {
        if (searchQuery.isNotBlank()) {
            SiteRepository.searchSites(searchQuery)
        } else {
            SiteRepository.getSitesByCategory(selectedCategory)
        }
    }
    
    val customSites = remember(refreshKey) { SiteRepository.getCustomSites() }
    
    // Add Site Dialog
    if (showAddSiteDialog) {
        AddSiteDialog(
            onDismiss = { showAddSiteDialog = false },
            onAddSite = { site ->
                SiteRepository.addCustomSite(site)
                refreshKey++
            }
        )
    }
    
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DarkBackground,
                                DarkSurface
                            )
                        )
                    )
            ) {
                // App Header with Stats
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "Protected",
                                    tint = ShieldActive,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "MoviePanel",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ad-Free Streaming Hub",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ShieldActive,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                        
                    Row {
                        // View mode toggle (Grid/List)
                        IconButton(
                            onClick = { 
                                viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.List,
                                contentDescription = "Toggle view",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        
                        // Downloads button
                        IconButton(
                            onClick = { 
                                context.startActivity(Intent(context, DownloadsActivity::class.java))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Downloads",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        
                        // TeraBox Player button
                        IconButton(
                            onClick = { 
                                context.startActivity(Intent(context, TeraBoxPlayerActivity::class.java))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "TeraBox Player",
                                tint = CategoryHollywood,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        
                        // Search button
                        IconButton(
                            onClick = { showSearch = !showSearch }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (showSearch) PrimaryBlue else TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    }
                    
                    // Stats Row
                    if (!showSearch && searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatBadge(
                                label = "Sites",
                                value = "${allSites.size}",
                                color = PrimaryBlue,
                                modifier = Modifier.weight(1f)
                            )
                            StatBadge(
                                label = "Favorites",
                                value = "$favoritesCount",
                                color = SecondaryCoral,
                                modifier = Modifier.weight(1f)
                            )
                            StatBadge(
                                label = "Custom",
                                value = "$customSitesCount",
                                color = AccentGold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Search bar
                AnimatedVisibility(
                    visible = showSearch,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextPrimary
                                ),
                                cursorBrush = SolidColor(PrimaryBlue),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search sites...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextTertiary
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }
                
                // Category chips
                if (!showSearch) {
                    CategoryChips(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSiteDialog = true },
                containerColor = PrimaryBlue,
                contentColor = DarkBackground,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Site",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (viewMode == ViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    renderSiteList(
                        context = context,
                        sites = sites,
                        customSites = customSites,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        refreshKey = refreshKey,
                        onRefresh = { refreshKey++ }
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    renderSiteGrid(
                        context = context,
                        sites = sites,
                        customSites = customSites,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        refreshKey = refreshKey,
                        onRefresh = { refreshKey++ }
                    )
                }
            }
            
            // Pull-to-refresh indicator
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    color = PrimaryBlue
                )
            }
        }
    }
}

private fun LazyListScope.renderSiteList(
    context: android.content.Context,
    sites: List<com.example.personal.models.MovieSite>,
    customSites: List<com.example.personal.models.MovieSite>,
    selectedCategory: String,
    searchQuery: String,
    refreshKey: Int,
    onRefresh: () -> Unit
) {
    // Custom sites section
    if (customSites.isNotEmpty() && selectedCategory == com.example.personal.models.Categories.ALL && searchQuery.isEmpty()) {
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                SectionHeader(title = "📌 My Sites")
            }
        }
        items(
            items = customSites,
            key = { "custom_${it.id}" }
        ) { site ->
            renderSiteCard(context, site, onRefresh)
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
    
    // Favorites section
    if (selectedCategory == com.example.personal.models.Categories.ALL && searchQuery.isEmpty()) {
        val favorites = com.example.personal.data.SiteRepository.getFavoriteSites()
        if (favorites.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    SectionHeader(title = "⭐ Favorites")
                }
            }
            items(favorites.take(3), key = { "fav_${it.id}" }) { site ->
                renderSiteCard(context, site, onRefresh)
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
    
    // All sites section
    item {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            SectionHeader(
                title = if (searchQuery.isNotBlank()) "🔍 Results" 
                       else if (selectedCategory == com.example.personal.models.Categories.ALL) "📚 All Sites" 
                       else "🎬 $selectedCategory"
            )
        }
    }
    
    items(sites, key = { it.id }) { site ->
        renderSiteCard(context, site, onRefresh)
    }
    
    // Empty state
    if (sites.isEmpty()) {
        item {
            EmptyStateView(
                isSearch = searchQuery.isNotBlank(),
                searchQuery = searchQuery,
                modifier = Modifier.padding(vertical = 60.dp)
            )
        }
    }
    
    // Extra space for FAB
    item { Spacer(modifier = Modifier.height(80.dp)) }
}

private fun LazyGridScope.renderSiteGrid(
    context: android.content.Context,
    sites: List<com.example.personal.models.MovieSite>,
    customSites: List<com.example.personal.models.MovieSite>,
    selectedCategory: String,
    searchQuery: String,
    refreshKey: Int,
    onRefresh: () -> Unit
) {
    // Similar structure but with grid layout
    if (customSites.isNotEmpty() && selectedCategory == com.example.personal.models.Categories.ALL && searchQuery.isEmpty()) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            SectionHeader(title = "📌 My Sites")
        }
        items(
            items = customSites,
            key = { "custom_${it.id}" }
        ) { site ->
            renderSiteCard(context, site, onRefresh, isGrid = true)
        }
    }
    
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
        SectionHeader(
            title = if (searchQuery.isNotBlank()) "🔍 Results" 
                   else if (selectedCategory == com.example.personal.models.Categories.ALL) "📚 All Sites" 
                   else "🎬 $selectedCategory"
        )
    }
    
    items(sites, key = { it.id }) { site ->
        renderSiteCard(context, site, onRefresh, isGrid = true)
    }
    
    if (sites.isEmpty()) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            EmptyStateView(
                isSearch = searchQuery.isNotBlank(),
                searchQuery = searchQuery,
                modifier = Modifier.padding(vertical = 60.dp)
            )
        }
    }
    
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun renderSiteCard(
    context: android.content.Context,
    site: com.example.personal.models.MovieSite,
    onRefresh: () -> Unit,
    isGrid: Boolean = false
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInHorizontally(),
        exit = fadeOut() + slideOutHorizontally()
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
            onRemoveClick = {
                com.example.personal.data.SiteRepository.removeSite(site.id)
                onRefresh()
            }
        )
    }
}

// EmptyStateView, SectionHeader, and StatBadge are now in ui.components package