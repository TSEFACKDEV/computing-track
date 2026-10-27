package com.klein.ictcomputing.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ContestColors {
    val Background = Color(0xFF0A0B10)
    val Surface = Color(0xFF12141C)
    val SurfaceHigh = Color(0xFF1A1D28)
    val SurfaceLow = Color(0xFF0F1118)
    val Border = Color(0xFF232734)
    val OnSurface = Color(0xFFE8EAF2)
    val OnSurfaceMuted = Color(0xFF9AA3B5)
    val AccentRed = Color(0xFFFF2D55)
    val AccentRedDeep = Color(0xFF99001F)
    val AccentCyan = Color(0xFF22D3EE)
    val AccentGreen = Color(0xFF34D399)
    val AccentAmber = Color(0xFFFBBF24)
    val AccentPurple = Color(0xFFA78BFA)
}

private val DarkScheme = darkColorScheme(
    primary = ContestColors.AccentRed,
    onPrimary = Color.White,
    secondary = ContestColors.AccentCyan,
    onSecondary = Color(0xFF062026),
    tertiary = ContestColors.AccentGreen,
    background = ContestColors.Background,
    onBackground = ContestColors.OnSurface,
    surface = ContestColors.Surface,
    onSurface = ContestColors.OnSurface,
    surfaceVariant = ContestColors.SurfaceHigh,
    onSurfaceVariant = ContestColors.OnSurfaceMuted,
    outline = ContestColors.Border,
    error = Color(0xFFF87171),
)

@Composable
fun IctTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography(
            bodyMedium = TextStyle(fontSize = 15.sp, fontFamily = FontFamily.SansSerif),
            bodyLarge = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.SansSerif),
            bodySmall = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.SansSerif),
            titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif),
            titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif),
            titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif),
            labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif),
        ),
    ) {
        content()
    }
}

val FontMono = FontFamily.Monospace

const val SUBJECT_LABEL = "subjectLabel"

fun subjectColor(subject: String): Color = when (subject) {
    "openEuler" -> ContestColors.AccentCyan
    "openGauss" -> ContestColors.AccentGreen
    else -> ContestColors.AccentPurple
}

fun phaseColor(phase: String): Color = when (phase) {
    "HCIA" -> ContestColors.AccentCyan
    "HCIP" -> ContestColors.AccentGreen
    else -> ContestColors.AccentPurple
}
