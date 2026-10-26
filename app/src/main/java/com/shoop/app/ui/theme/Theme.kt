package com.shoop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Shoop dark palette - own shades in a fitness-tracker night aesthetic.
val ShoopBackground = Color(0xFF0B0D0C)
val ShoopCard = Color(0xFF191D1B)
val ShoopCardAlt = Color(0xFF222725)
val ShoopGreen = Color(0xFF35E0A1)
val ShoopYellow = Color(0xFFF5C04E)
val ShoopRed = Color(0xFFF26D6D)
val ShoopBlue = Color(0xFF5BA8F5)
val ShoopTeal = Color(0xFF3ECFC0)
val ShoopText = Color(0xFFF2F5F3)
val ShoopMuted = Color(0xFF8A938F)
val ShoopTrack = Color(0xFF2A302D)

private val ShoopDark = darkColorScheme(
    primary = ShoopGreen,
    onPrimary = Color.Black,
    background = ShoopBackground,
    onBackground = ShoopText,
    surface = ShoopCard,
    onSurface = ShoopText,
    surfaceVariant = ShoopCard,
    onSurfaceVariant = ShoopMuted,
    secondary = ShoopTeal,
    tertiary = ShoopYellow,
)

@Composable
fun ShoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ShoopDark, content = content)
}

fun recoveryColor(score: Int): Color = when {
    score >= 67 -> ShoopGreen
    score >= 34 -> ShoopYellow
    else -> ShoopRed
}
