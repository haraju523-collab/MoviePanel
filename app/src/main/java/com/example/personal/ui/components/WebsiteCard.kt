package com.example.personal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personal.models.MovieSite
import com.example.personal.ui.theme.DarkCard
import com.example.personal.ui.theme.DarkCardHover
import com.example.personal.ui.theme.SecondaryCoral
import com.example.personal.ui.theme.TextPrimary
import com.example.personal.ui.theme.TextSecondary
import com.example.personal.ui.theme.TextTertiary
import com.example.personal.ui.theme.getCategoryColor
import com.example.personal.utils.rememberHapticFeedback
import com.example.personal.utils.HapticType

@Composable
fun WebsiteCard(
    site: MovieSite,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val categoryColor = getCategoryColor(site.category)
    
    // Smooth scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )
    
    // Elevation animation
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4f else 8f,
        animationSpec = tween(200),
        label = "cardElevation"
    )
    
    // Favorite icon scale animation
    val favoriteScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favoriteScale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = categoryColor.copy(alpha = 0.3f)
            )
            .scale(scale)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPressed) DarkCardHover else DarkCard
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Subtle gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    categoryColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Premium Icon with gradient
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = categoryColor.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        categoryColor.copy(alpha = 0.3f),
                                        categoryColor.copy(alpha = 0.15f),
                                        categoryColor.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = site.iconEmoji,
                            fontSize = 32.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(18.dp))
                    
                    // Content with better typography
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = site.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = 0.3.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = site.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Enhanced Favorite button with animation
                    IconButton(
                        onClick = {
                            try {
                                haptic(HapticType.MEDIUM)
                            } catch (e: Exception) {
                                // Ignore haptic errors
                            }
                            onFavoriteClick()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        val favoriteColor by animateColorAsState(
                            targetValue = if (isFavorite) SecondaryCoral else TextTertiary,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "favoriteColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .scale(favoriteScale),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = favoriteColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

