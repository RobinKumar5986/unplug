package com.kgjr.unplug.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp




// ── Shapes ───────────────────────────────────────────────────────────────────
private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)


// ── Color Scheme ─────────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary          = Accent,
    onPrimary        = White,

    secondary        = AccentDim,
    onSecondary      = White,

    background       = Background,
    onBackground     = OnBg,

    surface          = Surface0,
    onSurface        = OnBg,

    surfaceVariant   = Surface1,
    onSurfaceVariant = OnBgMuted,

    error            = Danger,
    onError          = White
)


// ── Theme ────────────────────────────────────────────────────────────────────
@Composable
fun UnplugTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}