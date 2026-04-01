package com.kgjr.unplug.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────────
val Background   = Color(0xFF0A0A0F)
val Surface0     = Color(0xFF13131A)
val Surface1     = Color(0xFF1C1C27)
val Accent       = Color(0xFF7C6AF7)
val AccentDim    = Color(0xFF4A3FA0)
val AccentGlow   = Color(0x337C6AF7)
val Danger       = Color(0xFFFF5D6C)
val DangerDim    = Color(0x33FF5D6C)
val OnBg         = Color(0xFFEAE8FF)
val OnBgMuted    = Color(0xFF7A78A0)
val White        = Color(0xFFFFFFFF)

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
    onError          = White,
)

@Composable
fun UnplugTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography   = Typography(),
        content      = content
    )
}