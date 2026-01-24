package com.example.personal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personal.ui.theme.*
import com.example.personal.utils.AdBlocker
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.personal.utils.UpdateManager
import com.example.personal.ui.components.UpdateDialog
import com.example.personal.download.DownloadManager
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize repositories and managers
        com.example.personal.data.SiteRepository.init(this)
        com.example.personal.data.PreferencesManager.init(this)
        
        // Load ad blocker rules
        AdBlocker.loadAdBlockRules(this)
        
        setContent {
            MoviePanelTheme {
                var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
                
                // Check for updates on launch
                LaunchedEffect(Unit) {
                    launch {
                        val info = UpdateManager.checkForUpdates()
                        if (info != null) {
                            updateInfo = info
                        }
                    }
                }
                
                SplashScreen(
                    onComplete = {
                        val intent = Intent(this, WebsiteListActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
                
                // Show Update Dialog if available
                updateInfo?.let { info ->
                    UpdateDialog(
                        updateInfo = info,
                        onUpdate = {
                            startUpdateDownload(info.downloadUrl, info.versionName)
                            updateInfo = null
                        },
                        onDismiss = { updateInfo = null }
                    )
                }
            }
        }
    }
    
    private fun startUpdateDownload(url: String, versionName: String) {
        try {
            DownloadManager.startDownload(
                url = url,
                fileName = "MoviePanel_v${versionName}.apk",
                mimeType = "application/vnd.android.package-archive"
            )
            Toast.makeText(this, "Downloading update...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val transition = updateTransition(targetState = startAnimation, label = "splashTransition")
    
    val alphaAnim by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = "alpha"
    ) { if (it) 1f else 0f }
    
    val scaleAnim by transition.animateFloat(
        transitionSpec = { 
            if (false isTransitioningTo true) {
                 spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            } else {
                tween(0)
            }
        },
        label = "scale"
    ) { if (it) 1f else 0.5f }
    
    val offsetAnim by transition.animateDp(
        transitionSpec = { tween(1000, easing = EaseOutExpo) },
        label = "offset"
    ) { if (it) 0.dp else 50.dp }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // Longer delay to enjoy animation
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Animated Background Gradient
        val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
        val rotateAnim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing)
            ),
            label = "rotation"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                             DarkBackground,
                             PrimaryBlueDark.copy(alpha = 0.2f),
                             DarkBackground
                        )
                    )
                )
                .graphicsLayer { rotationZ = rotateAnim }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
                .offset(y = offsetAnim)
        ) {
            // Composed Logo (Shield + Play)
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(120.dp).alpha(0.2f)
                )
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = ShieldActive,
                    modifier = Modifier.size(100.dp)
                )
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(50.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "MoviePanel",
                style = MaterialTheme.typography.displayMedium.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PrimaryBlue, Color.Cyan)
                    )
                ),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ad-Free Streaming Hub",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
        }
    }
}