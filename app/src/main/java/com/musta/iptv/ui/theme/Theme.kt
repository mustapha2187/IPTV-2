package com.musta.iptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private object Palette {
    val Ink = Color(0xFF0B0E14)
    val Panel = Color(0xFF151A23)
    val PanelHigh = Color(0xFF1E2531)
    val Accent = Color(0xFF3D8BFF)
    val OnAccent = Color(0xFF04122B)
    val TextHigh = Color(0xFFEAF0FA)
    val TextMid = Color(0xFF9AA7BC)
}

@OptIn(ExperimentalTvMaterial3Api::class)
private val MustaColors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.OnAccent,
    background = Palette.Ink,
    onBackground = Palette.TextHigh,
    surface = Palette.Ink,
    onSurface = Palette.TextHigh,
    surfaceVariant = Palette.PanelHigh,
    onSurfaceVariant = Palette.TextMid,
    secondary = Palette.Panel,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MustaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MustaColors, content = content)
}
