package com.example.personal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SwipeableCard(
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onUndo: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val snackbarHostState = remember { SnackbarHostState() }
    var swipedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(300),
        label = "swipeOffset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    offsetX = (offsetX + dragAmount).coerceIn(-300f, 300f)
                }
            }
    ) {
        // Background actions
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
        ) {
            // Right swipe action (favorite) - green background
            if (animatedOffset > 50f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(Color(0xFF4CAF50))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Left swipe action (delete) - red background
            if (animatedOffset < -50f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(Color(0xFFEF4444))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        // Main content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
        ) {
            content()
        }
        
        // Handle swipe completion
        LaunchedEffect(animatedOffset) {
            when {
                animatedOffset > 200f -> {
                    swipedAction = onSwipeRight
                    offsetX = 0f
                    onSwipeRight()
                }
                animatedOffset < -200f -> {
                    swipedAction = onSwipeLeft
                    val result = snackbarHostState.showSnackbar(
                        message = "Site removed",
                        actionLabel = "Undo"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onUndo()
                    } else {
                        onSwipeLeft()
                    }
                    offsetX = 0f
                }
                animatedOffset in -50f..50f -> {
                    offsetX = 0f
                }
            }
        }
    }
    
    SnackbarHost(hostState = snackbarHostState)
}
