package com.shoop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ShoopBackground = Color(0xFF0E0E12)
val ShoopSurface = Color(0xFF1A1A21)
val ShoopGreen = Color(0xFF4ADE80)
val ShoopYellow = Color(0xFFFACC15)
val ShoopRed = Color(0xFFF87171)
val ShoopBlue = Color(0xFF60A5FA)
val ShoopText = Color(0xFFF4F4F5)
val ShoopMuted = Color(0xFF9CA3AF)

private val ShoopDark = darkColorScheme(
    primary = ShoopGreen,
    onPrimary = Color.Black,
    background = ShoopBackground,
    onBackground = ShoopText,
    surface = ShoopSurface,
    onSurface = ShoopText,
    surfaceVariant = ShoopSurface,
    onSurfaceVariant = ShoopMuted,
    secondary = ShoopBlue,
    tertiary = ShoopYellow,
)

@Composable
fun ShoopTheme(content: @Composable () -> Unit) {
    // Shoop is dark-only by design (readability at a glance, night use).
    MaterialTheme(colorScheme = ShoopDark, content = content)
}

fun recoveryColor(score: Int): Color = when {
    score >= 67 -> ShoopGreen
    score >= 34 -> ShoopYellow
    else -> ShoopRed
}
