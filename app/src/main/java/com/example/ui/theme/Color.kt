package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

// Core Brand & Palette
val DeepNavyPrimary = Color(0xFF0F172A)     // #0F172A (Deep Navy)
val PrimaryLight = Color(0xFF1E293B)        // #1E293B
val PrimarySurface = Color(0xFFF8FAFC)      // #F8FAFC

val ElectricBlue = Color(0xFF3B82F6)        // #3B82F6 (Vibrant Electric Blue)
val ElectricBlueLight = Color(0xFF60A5FA)   // #60A5FA
val CosmicDeepBlue = Color(0xFF1D4ED8)      // #1D4ED8
val CyberPurple = Color(0xFF8B5CF6)         // #8B5CF6

val ElectricOrange = Color(0xFFF97316)      // #F97316 (Electric Orange)
val ElectricOrangeLight = Color(0xFFFB923C) // #FB923C
val ElectricGold = Color(0xFFF59E0B)        // #F59E0B

// Status Colors
val SuccessGreen = Color(0xFF22C55E)       // #22C55E (Green)
val WarningYellow = Color(0xFFEAB308)      // #EAB308 (Yellow)
val DangerRed = Color(0xFFEF4444)          // #EF4444 (Red)
val InfoCyan = Color(0xFF06B6D4)           // #06B6D4 (Cyan)

// Aliases for backward compatibility
val NeonGreen = SuccessGreen
val NeonYellow = WarningYellow
val NeonRed = DangerRed
val CarbonDarkBg = DeepNavyPrimary

val DarkGlassSurface: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xF2FFFFFF) else Color(0xCC0F172A)

val BorderSilver: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFE2E8F0) else Color(0x3364748B)

// Neutral Colors
val BackgroundLight = Color(0xFFF1F5F9)
val BackgroundDark = Color(0xFF030712)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF0F172A)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)
val TextHintLight = Color(0xFF94A3B8)
val BorderLight = Color(0xFFE2E8F0)
val BorderDark = Color(0xFF1E293B)

// Material 3 defaults mappings
val M3BgDark = Color(0xFF030712)
val M3SurfaceDark = Color(0xFF0F172A)
val M3OnSurfaceDark = Color(0xFFF8FAFC)     // Slate-100
val M3OnPrimaryDark = Color(0xFFFFFFFF)
val M3SecondaryDark = Color(0xFF1E293B)


