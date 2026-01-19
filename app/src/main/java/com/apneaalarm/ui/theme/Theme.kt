package com.apneaalarm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Zen-inspired dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4A574),          // Warm gold/amber (temple bells)
    onPrimary = Color(0xFF3D2914),        // Dark brown
    primaryContainer = Color(0xFF5C3D1E), // Deep bronze
    onPrimaryContainer = Color(0xFFF5E6D3), // Cream
    secondary = Color(0xFF9CB99C),        // Sage green (nature)
    onSecondary = Color(0xFF1A3A1A),      // Deep forest
    secondaryContainer = Color(0xFF2D4D2D), // Forest green
    onSecondaryContainer = Color(0xFFD4E8D4), // Pale sage
    tertiary = Color(0xFFB8A9C4),         // Soft lavender (meditation)
    onTertiary = Color(0xFF362D3D),       // Deep purple
    tertiaryContainer = Color(0xFF4D4255), // Muted purple
    onTertiaryContainer = Color(0xFFE8E0ED), // Pale lavender
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1A2E),       // Deep twilight indigo
    onBackground = Color(0xFFF5F0E8),     // Warm cream
    surface = Color(0xFF252540),          // Slightly lighter indigo
    onSurface = Color(0xFFF5F0E8),        // Warm cream
    surfaceVariant = Color(0xFF3D3D5C),   // Muted indigo
    onSurfaceVariant = Color(0xFFD4CFC7), // Warm grey
    outline = Color(0xFF8A8A9A),          // Soft grey
    outlineVariant = Color(0xFF3D3D5C)    // Muted indigo
)

// Zen-inspired light color scheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5A2B),          // Warm brown (wooden temple)
    onPrimary = Color(0xFFFFFBF8),        // Warm white
    primaryContainer = Color(0xFFF5E6D3), // Cream
    onPrimaryContainer = Color(0xFF3D2914), // Dark brown
    secondary = Color(0xFF4A6B4A),        // Deep sage green
    onSecondary = Color(0xFFFFFBF8),      // Warm white
    secondaryContainer = Color(0xFFD4E8D4), // Pale sage
    onSecondaryContainer = Color(0xFF1A3A1A), // Deep forest
    tertiary = Color(0xFF6B5B7A),         // Muted purple
    onTertiary = Color(0xFFFFFBF8),       // Warm white
    tertiaryContainer = Color(0xFFE8E0ED), // Pale lavender
    onTertiaryContainer = Color(0xFF362D3D), // Deep purple
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF6F0),       // Warm parchment
    onBackground = Color(0xFF2D2A26),     // Warm charcoal
    surface = Color(0xFFF5F0E8),          // Cream
    onSurface = Color(0xFF2D2A26),        // Warm charcoal
    surfaceVariant = Color(0xFFEBE5DB),   // Slightly darker cream
    onSurfaceVariant = Color(0xFF4A4640), // Muted brown
    outline = Color(0xFF7A7570),          // Warm grey
    outlineVariant = Color(0xFFD4CFC7)    // Light warm grey
)

@Composable
fun ApneaAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use our custom zen color scheme (no dynamic colors)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use background color for status bar for a more immersive feel
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
