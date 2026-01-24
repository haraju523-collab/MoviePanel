package com.example.personal.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personal.WebViewActivity
import com.example.personal.data.SiteRepository
import com.example.personal.ui.components.EmptyStateView
import com.example.personal.ui.components.WebsiteCard
import com.example.personal.ui.components.AddSiteDialog
import com.example.personal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showAddSiteDialog by remember { mutableStateOf(false) }
    
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
    
    val favorites = remember(refreshKey) { SiteRepository.getFavoriteSites() }
    
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
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
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    isSearch = false,
                    searchQuery = "",
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(favorites, key = { it.id }) { site ->
                    WebsiteCard(
                        site = site,
                        isFavorite = true,
                        onClick = {
                            SiteRepository.addToRecent(site.id)
                            val intent = Intent(context, WebViewActivity::class.java).apply {
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
            }
        }
    }
}
