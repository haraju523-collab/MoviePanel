package com.example.personal.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personal.data.SiteRepository
import com.example.personal.models.MovieSite
import com.example.personal.ui.theme.*

@Composable
fun BrowserStartPage(
    onNavigate: (String) -> Unit
) {
    val quotes = remember {
        listOf(
            "The magic you're looking for is in the work you're avoiding." to "Unknown",
            "Every movie is a miracle." to "Audrey Hepburn",
            "I'm going to make him an offer he can't refuse." to "The Godfather",
            "May the Force be with you." to "Star Wars",
            "Why so serious?" to "The Dark Knight",
            "Life is like a box of chocolates. You never know what you're going to get." to "Forrest Gump",
            "To infinity and beyond!" to "Toy Story",
            "I see dead people." to "The Sixth Sense",
            "It's alive! It's alive!" to "Frankenstein",
            "Houston, we have a problem." to "Apollo 13",
            "Here's looking at you, kid." to "Casablanca",
            "There's no place like home." to "The Wizard of Oz",
            "Carpe Diem. Seize the day, boys." to "Dead Poets Society",
            "Keep your friends close, but your enemies closer." to "The Godfather Part II",
            "Just keep swimming." to "Finding Nemo"
        )
    }
    
    val randomQuote = remember { quotes.random() }
    val trendingMovies = remember { 
        SiteRepository.getAllSites().sortedByDescending { it.accessCount }.take(10)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp), // Padding for bottom bar
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Quote Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2C2C3E),
                                Color(0xFF1F1F2C)
                            )
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = AccentGold.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = randomQuote.first,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "- ${randomQuote.second}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextTertiary,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Trending Movies Section
            if (trendingMovies.isNotEmpty()) {
                Text(
                    text = "Trending Now",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 24.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(trendingMovies) { site ->
                        StartPageMovieCard(
                            site = site,
                            onClick = { onNavigate(site.url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StartPageMovieCard(
    site: MovieSite,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(site.category)
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
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
                                categoryColor.copy(alpha = 0.15f)
                            )
                        )
                    )
            )
            
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
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = site.iconEmoji,
                        fontSize = 28.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = site.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = site.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
