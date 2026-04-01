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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.kgjr.unplug.helper.PermissionHelper
import com.kgjr.unplug.ui.theme.*

// ── Data ─────────────────────────────────────────────────────────────────────

private data class PermItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val why: String,
    val check: (android.content.Context) -> Boolean,
    val open: (android.content.Context) -> Unit
)

private val permissions = listOf(
    PermItem(
        id       = "accessibility",
        icon     = Icons.Outlined.Face,
        title    = "Accessibility Service",
        subtitle = "Required to detect & block content",
        why      = "Unplug reads the screen to detect Reels / Shorts tabs and navigates away. No data leaves your device.",
        check    = { PermissionHelper.hasAccessibilityPermission(it) },
        open     = { PermissionHelper.openAccessibilitySettings(it) }
    ),
    PermItem(
        id       = "usage",
        icon     = Icons.Outlined.CheckCircle,
        title    = "Usage Access",
        subtitle = "Know which app is in the foreground",
        why      = "Lets Unplug know when Instagram or YouTube is open so blocking is only applied to those apps.",
        check    = { PermissionHelper.hasUsageStatsPermission(it) },
        open     = { PermissionHelper.openUsageStatsSettings(it) }
    ),
    PermItem(
        id       = "overlay",
        icon     = Icons.Outlined.Menu,
        title    = "Draw Over Apps",
        subtitle = "Show nudge overlays (future feature)",
        why      = "Reserved for the upcoming 'cool-down overlay' that gently interrupts doom-scrolling sessions.",
        check    = { PermissionHelper.hasOverlayPermission(it) },
        open     = { PermissionHelper.openOverlaySettings(it) }
    )
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    // Refresh state when the composable recomposes (after returning from settings)
    var grantedMap by remember {
        mutableStateOf(permissions.associate { it.id to it.check(context) })
    }

    val allGranted = grantedMap.values.all { it }

    // Pulse animation for the logo orb
    val pulse = rememberInfiniteTransition(label = "pulse")
    val orbScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "orbScale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Radial glow behind orb
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentGlow, Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.18f),
                    radius = size.width * 0.55f
                )
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Orb logo ─────────────────────────────────────────────────────
            Box(
                Modifier
                    .size(80.dp)
                    .scale(orbScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Accent, AccentDim))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Unplug",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = OnBg
            )
            Text(
                "Reclaim your attention",
                style = MaterialTheme.typography.bodyMedium,
                color = OnBgMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            Text(
                "Permissions needed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnBg,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Unplug needs a few special permissions to watch for short-form content even when the app is closed. Everything runs on-device.",
                style = MaterialTheme.typography.bodySmall,
                color = OnBgMuted,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── Permission cards ──────────────────────────────────────────────
            permissions.forEach { perm ->
                val granted = grantedMap[perm.id] == true
                PermCard(
                    item = perm,
                    granted = granted,
                    onClick = {
                        perm.open(context)
                        // State will refresh in onResume via MainActivity
                        grantedMap = permissions.associate { it.id to it.check(context) }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(28.dp))

            // ── CTA ───────────────────────────────────────────────────────────
            AnimatedVisibility(visible = allGranted) {
                Button(
                    onClick = onPermissionsGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = White)
                    Spacer(Modifier.width(8.dp))
                    Text("All set — let's go!", color = White, fontWeight = FontWeight.Bold)
                }
            }

            if (!allGranted) {
                Text(
                    "Grant all permissions above to continue",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnBgMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Permission Card ───────────────────────────────────────────────────────────

@Composable
private fun PermCard(item: PermItem, granted: Boolean, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = if (granted) Accent else Surface1
    val iconBg      = if (granted) AccentGlow else DangerDim

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        color = Surface0,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Icon badge
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (granted) Accent else Danger,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = OnBg)
                    Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = OnBgMuted)
                }

                Spacer(Modifier.width(8.dp))

                if (granted) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Granted", tint = Accent, modifier = Modifier.size(22.dp))
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, Accent)
                    ) {
                        Text("Grant", color = Accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Expandable "why" section
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = Surface1)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Why is this needed?",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBgMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(item.why, style = MaterialTheme.typography.bodySmall, color = OnBgMuted)
                }
            }

            // Expand chevron hint
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    tint = OnBgMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}