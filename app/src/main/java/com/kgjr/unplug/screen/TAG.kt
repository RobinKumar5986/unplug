package com.kgjr.unplug.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

import com.kgjr.unplug.ui.theme.*
import android.util.Log
import com.kgjr.unplug.helper.BlockPreferences
import com.kgjr.unplug.helper.BlockedApp

private const val TAG = "BlockScreen"

// ── App registry ──────────────────────────────────────────────────────────────

private val BLOCKABLE_APPS = listOf(
    BlockedApp(
        id = "instagram_reels",
        packageName = "com.instagram.android",
        appName = "Instagram",
        featureName = "Reels",
        iconRes = 0   // replaced by emoji in UI for now
    ),
    BlockedApp(
        id          = "youtube_shorts",
        packageName = "com.google.android.youtube",
        appName     = "YouTube",
        featureName = "Shorts",
        iconRes     = 0
    )
)

// Emoji stand-ins until vector assets are added
private fun appEmoji(id: String) = when (id) {
    "instagram_reels" -> "📸"
    "youtube_shorts"  -> "▶️"
    else              -> "📱"
}

private fun featureColor(id: String) = when (id) {
    "instagram_reels" -> Color(0xFFE1306C)   // IG pink
    "youtube_shorts"  -> Color(0xFFFF0000)   // YT red
    else              -> Accent
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun BlockScreen() {
    val context = LocalContext.current

    // Init prefs
    LaunchedEffect(Unit) {
        BlockPreferences.init(context)
    }

    // Observe blocked set
    val blockedIds by BlockPreferences.blockedIds.collectAsState()
    val totalBlocked = blockedIds.size

    // Pulse for the shield orb
    val pulse = rememberInfiniteTransition(label = "pulse")
    val orbScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "orbScale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Background glow
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentGlow, Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.15f),
                    radius = size.width * 0.6f
                )
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                Modifier
                    .size(72.dp)
                    .scale(orbScale)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Accent, AccentDim))),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡️", fontSize = 30.sp)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Unplug",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = OnBg
            )

            AnimatedContent(
                targetState = totalBlocked,
                label = "subtitle"
            ) { count ->
                Text(
                    if (count == 0) "Nothing blocked yet"
                    else "$count feature${if (count > 1) "s" else ""} blocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (count > 0) Accent else OnBgMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Stats strip ───────────────────────────────────────────────────
            StatsStrip(totalBlocked = totalBlocked)

            Spacer(Modifier.height(28.dp))

            // ── Section label ─────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Short-form content",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBg
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "BETA",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentDim,
                    modifier = Modifier
                        .border(1.dp, AccentDim, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Toggle to block the selected feature inside each app. Works even when Unplug is closed.",
                style = MaterialTheme.typography.bodySmall,
                color = OnBgMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── App cards ─────────────────────────────────────────────────────
            BLOCKABLE_APPS.forEach { app ->
                val isBlocked = blockedIds.contains(app.id)
                AppBlockCard(
                    app      = app,
                    isBlocked = isBlocked,
                    onToggle = { enabled ->
                        Log.d(TAG, "Toggle ${app.id} → $enabled")
                        BlockPreferences.setBlocked(app.id, enabled)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            // ── Coming soon ───────────────────────────────────────────────────
            ComingSoonCard()

            Spacer(Modifier.height(16.dp))

            Text(
                "All processing is on-device. No data leaves your phone.",
                style = MaterialTheme.typography.labelSmall,
                color = OnBgMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Stats strip ───────────────────────────────────────────────────────────────

@Composable
private fun StatsStrip(totalBlocked: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(
            label = "Blocked",
            value = totalBlocked.toString(),
            icon  = "🚫",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Apps watched",
            value = "2",
            icon  = "👁️",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Status",
            value = "Active",
            icon  = "✅",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Surface0,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Surface1)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBg)
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnBgMuted)
        }
    }
}

// ── App block card ────────────────────────────────────────────────────────────

@Composable
private fun AppBlockCard(
    app: BlockedApp,
    isBlocked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val color = featureColor(app.id)
    val borderColor = if (isBlocked) color.copy(alpha = 0.6f) else Surface1

    // Animated background tint
    val bgAlpha by animateFloatAsState(
        targetValue = if (isBlocked) 0.06f else 0f,
        animationSpec = tween(300),
        label = "bgAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
        color = if (isBlocked) color.copy(alpha = bgAlpha) else Surface0,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // App emoji badge
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(appEmoji(app.id), fontSize = 24.sp)
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = OnBg)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            app.featureName,
                            style = MaterialTheme.typography.labelMedium,
                            color = color
                        )
                    }
                }

                Switch(
                    checked = isBlocked,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor     = White,
                        checkedTrackColor     = color,
                        uncheckedThumbColor   = OnBgMuted,
                        uncheckedTrackColor   = Surface1,
                        uncheckedBorderColor  = Surface1
                    )
                )
            }

            // Status pill
            AnimatedVisibility(
                visible = isBlocked,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Divider(color = color.copy(alpha = 0.2f))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        Text(
                            "${app.featureName} will be blocked automatically",
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

// ── Coming soon ───────────────────────────────────────────────────────────────

@Composable
private fun ComingSoonCard() {
    val features = listOf(
        "⏱️  Scroll timer — 15 min then gradual cool-down",
        "💬  Block toxic comment sections",
        "🔞  Adult site & URL blocking",
        "📊  Weekly screen-time insights",
        "🎯  App usage goals & streaks"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Surface1, RoundedCornerShape(18.dp)),
        color = Surface0,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔮", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Coming soon",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBg
                )
            }
            Spacer(Modifier.height(12.dp))
            features.forEach { feat ->
                Text(
                    feat,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBgMuted,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}