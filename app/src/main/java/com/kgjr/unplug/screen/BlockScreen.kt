package com.kgjr.unplug.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

import com.kgjr.unplug.ui.theme.*
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.kgjr.unplug.navigation.graph.subgraph.ScreenId
import com.kgjr.unplug.sharedpref.BlockPreferences
import com.kgjr.unplug.sharedpref.BlockedApp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

private const val TAG = "BlockScreen"

private val BLOCKABLE_APPS = listOf(
    BlockedApp(id = "instagram_reels", packageName = "com.instagram.android",      appName = "Instagram", featureName = "Reels",  iconRes = 0),
    BlockedApp(id = "youtube_shorts",  packageName = "com.google.android.youtube", appName = "YouTube",   featureName = "Shorts", iconRes = 0)
)

private fun appEmoji(id: String)     = when (id) { "instagram_reels" -> "📸"; "youtube_shorts" -> "▶️"; else -> "📱" }
private fun featureColor(id: String) = when (id) { "instagram_reels" -> Color(0xFFE1306C); "youtube_shorts" -> Color(0xFFFF0000); else -> Accent }

sealed class AvailabilityState {
    object Available                                      : AvailabilityState()
    object HardBlocked                                    : AvailabilityState()
    data class FreeWithAllowance(val remainingMs: Long)   : AvailabilityState()
    data class CooldownPending(val unlocksAtMs: Long)     : AvailabilityState()
}

private fun computeAvailability(
    isBlocked: Boolean,
    cheatEnabled: Boolean,
    accumulatedMs: Long,
    lastBlockTimeMs: Long,
    cheatWindowMs: Long = 15 * 60 * 1000L,
    cooldownMs: Long    = 90 * 60 * 1000L
): AvailabilityState {
    if (!isBlocked)    return AvailabilityState.Available
    if (!cheatEnabled) return AvailabilityState.HardBlocked

    val now = System.currentTimeMillis()
    return if (lastBlockTimeMs > 0 && now - lastBlockTimeMs < cooldownMs) {
        AvailabilityState.CooldownPending(unlocksAtMs = lastBlockTimeMs + cooldownMs)
    } else {
        AvailabilityState.FreeWithAllowance(remainingMs = max(0L, cheatWindowMs - accumulatedMs))
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0s"
    val s = ms / 1000
    return when {
        s >= 3600 -> "${s / 3600}h ${(s % 3600) / 60}m"
        s >= 60   -> "${s / 60}m ${s % 60}s"
        else      -> "${s}s"
    }
}

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(epochMs))

@Composable
fun BlockScreen(
    onMoveTo: (ScreenId) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { BlockPreferences.init(context) }

    val blockedIds    by BlockPreferences.blockedIds.collectAsState()
    val cheatEnabled  by BlockPreferences.cheatEnabled.collectAsState()
    val accumulated   by BlockPreferences.accumulatedTimeFlow.collectAsState()
    val lastBlockTime by BlockPreferences.lastBlockTimeFlow.collectAsState()
    val totalBlocked  = blockedIds.size

    val pulse = rememberInfiniteTransition(label = "pulse")
    val orbScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "orbScale"
    )

    Box(Modifier.fillMaxSize().background(Background)) {
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
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(72.dp).scale(orbScale).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Accent, AccentDim))),
                contentAlignment = Alignment.Center
            ) { Text("🛡️", fontSize = 30.sp) }

            Spacer(Modifier.height(16.dp))
            Text("Unplug", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = OnBg)

            AnimatedContent(targetState = totalBlocked, label = "subtitle") { count ->
                Text(
                    if (count == 0) "Nothing blocked yet" else "$count feature${if (count > 1) "s" else ""} blocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (count > 0) Accent else OnBgMuted
                )
            }

            Spacer(Modifier.height(32.dp))
            StatsStrip(totalBlocked)
            Spacer(Modifier.height(28.dp))

            CheatModeCard(cheatEnabled) {
                Log.d(TAG, "Cheat mode → $it")
                BlockPreferences.setCheatMode(it)
            }

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Short-form content", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = OnBg)
                Spacer(Modifier.weight(1f))
                Text("BETA", style = MaterialTheme.typography.labelSmall, color = AccentDim,
                    modifier = Modifier.border(1.dp, AccentDim, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
            }

            Spacer(Modifier.height(16.dp))

            BLOCKABLE_APPS.forEach { app ->
                val isBlocked    = blockedIds.contains(app.id)
                val availability = computeAvailability(isBlocked, cheatEnabled, accumulated, lastBlockTime)
                AppBlockCard(app, isBlocked, availability) { BlockPreferences.setBlocked(app.id, it) }
                Spacer(Modifier.height(12.dp))
            }

            Surface(
                onClick = { onMoveTo(ScreenId.GRAY_SCREEN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = Surface1,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⬜", fontSize = 22.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Gray Screen mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBg
                        )
                        Text(
                            "Distraction-free mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBgMuted
                        )
                    }
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = OnBgMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ComingSoonCard()
        }
    }
}

@Composable
private fun AppBlockCard(
    app: BlockedApp,
    isBlocked: Boolean,
    availability: AvailabilityState,
    onToggle: (Boolean) -> Unit
) {
    val color   = featureColor(app.id)
    val bgAlpha by animateFloatAsState(if (isBlocked) 0.06f else 0f, tween(300), label = "bgAlpha")

    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, if (isBlocked) color.copy(alpha = 0.6f) else Surface1, RoundedCornerShape(18.dp)),
        color    = if (isBlocked) color.copy(alpha = bgAlpha) else Surface0,
        shape    = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text(appEmoji(app.id), fontSize = 24.sp) }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = OnBg)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(5.dp))
                        Text(app.featureName, style = MaterialTheme.typography.labelMedium, color = color)
                    }
                }

                Switch(
                    checked = isBlocked, onCheckedChange = onToggle,
                    colors  = SwitchDefaults.colors(
                        checkedThumbColor    = White,     checkedTrackColor    = color,
                        uncheckedThumbColor  = OnBgMuted, uncheckedTrackColor  = Surface1, uncheckedBorderColor = Surface1
                    )
                )
            }

            AnimatedVisibility(isBlocked, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Divider(color = color.copy(alpha = 0.2f))
                    Spacer(Modifier.height(10.dp))
                    AvailabilityBanner(availability, color)
                }
            }
        }
    }
}

@Composable
private fun AvailabilityBanner(availability: AvailabilityState, color: Color) {
    when (availability) {

        is AvailabilityState.HardBlocked -> {
            BannerRow("🚫", "Permanently blocked while this toggle is on", color)
        }

        // No penalty active — show static remaining allowance from accumulated time
        is AvailabilityState.FreeWithAllowance -> {
            val minutes = availability.remainingMs / 1000 / 60
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BannerRow("", "Active · ~${minutes}m of allowance remaining", color)
                val progress = (availability.remainingMs / (15 * 60 * 1000f)).coerceIn(0f, 1f)
                LinearProgressBar(progress, color)
            }
        }

        // Penalty active — we know exact start & end so we show a live countdown + unlock time
        is AvailabilityState.CooldownPending -> {
            var remaining by remember { mutableLongStateOf(max(0L, availability.unlocksAtMs - System.currentTimeMillis())) }
            LaunchedEffect(availability.unlocksAtMs) {
                while (remaining > 0) {
                    delay(1_000)
                    remaining = max(0L, availability.unlocksAtMs - System.currentTimeMillis())
                }
            }
            val progress by animateFloatAsState(
                (1f - remaining / (90 * 60 * 1000f)).coerceIn(0f, 1f), tween(800), label = "cooldownBar"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BannerRow("🔒", "Blocked · unlocks at ${formatTime(availability.unlocksAtMs)}", color, bold = true)
                LinearProgressBar(progress, color, trackAlpha = 0.12f)
                BannerRow("⏱️", formatDuration(remaining) + " remaining", OnBgMuted)
            }
        }

        AvailabilityState.Available -> Unit
    }
}

@Composable
private fun BannerRow(icon: String, text: String, color: Color, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(icon, fontSize = 13.sp)
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, color = color)
    }
}

@Composable
private fun LinearProgressBar(progress: Float, color: Color, trackAlpha: Float = 0.18f) {
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = trackAlpha))) {
        Box(
            Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
        )
    }
}

@Composable
private fun CheatModeCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, if (enabled) Accent.copy(alpha = 0.5f) else Surface1, RoundedCornerShape(18.dp)),
        color    = if (enabled) Accent.copy(alpha = 0.05f) else Surface0,
        shape    = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏳", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("Cheat Time", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = OnBg)
                }
                Spacer(Modifier.height(2.dp))
                Text("15m session window. Resets after 1.5h of silence.", style = MaterialTheme.typography.labelSmall, color = OnBgMuted)
            }
            Switch(checked = enabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
        }
    }
}

@Composable
private fun StatsStrip(totalBlocked: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatChip("Blocked",      totalBlocked.toString(), "🚫", Modifier.weight(1f))
        StatChip("Apps watched", "2",                     "👁️", Modifier.weight(1f))
        StatChip("Status",       "Active",                "✅", Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Surface0, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Surface1)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBg)
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnBgMuted)
        }
    }
}

@Composable
private fun ComingSoonCard() {
    val features = listOf(
        "💬  Block toxic comment sections",
        "🔞  Adult site & URL blocking",
        "📊  Weekly screen-time insights",
        "🎯  App usage goals & streaks"
    )
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Surface1, RoundedCornerShape(18.dp)),
        color    = Surface0,
        shape    = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔮", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("Coming soon", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = OnBg)
            }
            Spacer(Modifier.height(12.dp))
            features.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = OnBgMuted, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
    }
}