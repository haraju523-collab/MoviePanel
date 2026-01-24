package com.example.personal.utils

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class HapticType {
    LIGHT,      // For favorite toggle, small actions
    MEDIUM,     // For delete, important actions
    HEAVY,      // For success, major actions
    CLICK       // For button clicks
}

@Composable
fun rememberHapticFeedback(): (HapticType) -> Unit {
    val context = LocalContext.current
    
    return remember {
        { type: HapticType ->
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                }
                
                if (vibrator == null) {
                    return@remember
                }
                
                // Check if device has vibrator (API 11+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    if (!vibrator.hasVibrator()) {
                        return@remember
                    }
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = when (type) {
                        HapticType.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        HapticType.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        HapticType.HEAVY -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                        HapticType.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    val duration = when (type) {
                        HapticType.LIGHT -> 10L
                        HapticType.MEDIUM -> 20L
                        HapticType.HEAVY -> 50L
                        HapticType.CLICK -> 15L
                    }
                    vibrator.vibrate(duration)
                }
            } catch (e: SecurityException) {
                // Permission denied or vibrator not available - silently fail
                // Haptic feedback is optional, app should continue working
            } catch (e: Exception) {
                // Any other error - silently fail
            }
        }
    }
}
