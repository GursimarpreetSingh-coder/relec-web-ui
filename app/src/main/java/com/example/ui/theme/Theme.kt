package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

private val PremiumDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = M3OnPrimaryDark,
    secondary = CosmicDeepBlue,
    tertiary = CyberPurple,
    background = Color(0xFF070B14), // Deep luxury midnight slate
    surface = Color(0xFF0F172A),    // Clean deep glass slate
    onBackground = Color.White,
    onSurface = M3OnSurfaceDark,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = ElectricBlue
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = CosmicDeepBlue,
    onPrimary = Color.White,
    secondary = ElectricBlue,
    tertiary = CyberPurple,
    background = Color(0xFFF8FAFC), // Clean, luminous, crisp slate-50 canvas
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = CosmicDeepBlue
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default as requested
    customAccentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) PremiumDarkColorScheme else PremiumLightColorScheme
    val accent = customAccentColor ?: if (darkTheme) ElectricBlue else CosmicDeepBlue
    val finalScheme = baseScheme.copy(
        primary = accent,
        secondaryContainer = accent.copy(alpha = if (darkTheme) 0.20f else 0.12f),
        onSecondaryContainer = if (darkTheme) Color.White else accent
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // When in light theme (darkTheme == false), icons (time, battery, wifi, notifications) must be dark.
                // When in dark theme (darkTheme == true), icons must be light/white.
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun PremiumGlassBackdrop(
    isDarkTheme: Boolean,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    val isRevolt = accentColor == Color(0xFFEF4444) || accentColor == Color(0xFFDC2626)
    val topColor = if (isRevolt) {
        if (isDarkTheme) Color(0xFF140306) else Color(0xFFFFF8F8)
    } else {
        if (isDarkTheme) Color(0xFF070B14) else Color(0xFFF8FAFC)
    }
    val bottomColor = if (isRevolt) {
        if (isDarkTheme) Color(0xFF1E060A) else Color(0xFFFFF1F2)
    } else {
        if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .drawBehind {
                if (isDarkTheme) {
                    // Ambient colorful glow orbs in dark mode for depth
                    val pColor = accentColor.copy(alpha = 0.10f)
                    val sColor = Color(0xFF6366F1).copy(alpha = 0.08f)
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(pColor, Color.Transparent),
                            center = Offset(size.width * 0.15f, size.height * 0.15f),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(size.width * 0.15f, size.height * 0.15f)
                    )
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(sColor, Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.85f),
                            radius = size.width * 0.85f
                        ),
                        radius = size.width * 0.85f,
                        center = Offset(size.width * 0.85f, size.height * 0.85f)
                    )
                } else {
                    // Light mode: Ultra-subtle, clean ambient glow without muddy borders or grid lines
                    val subtleAccent = accentColor.copy(alpha = 0.04f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(subtleAccent, Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.1f),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.7f,
                        center = Offset(size.width * 0.8f, size.height * 0.1f)
                    )
                }
            }
    ) {
        content()
    }
}
