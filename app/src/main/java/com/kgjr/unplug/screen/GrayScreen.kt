package com.kgjr.unplug.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kgjr.unplug.ui.theme.Background
import com.kgjr.unplug.ui.theme.OnBg
import com.kgjr.unplug.ui.theme.OnBgMuted

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.kgjr.unplug.sharedpref.BlockPreferences
import com.kgjr.unplug.ui.theme.Accent
import com.kgjr.unplug.ui.theme.AccentGlow
import com.kgjr.unplug.ui.theme.Surface0

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable
)

@Composable
fun GrayScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager

    // Load only user-installed apps
    val apps = remember {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    name = pm.getApplicationLabel(it).toString(),
                    icon = pm.getApplicationIcon(it.packageName)
                )
            }
            .sortedBy { it.name }
    }

    // Pre-populate from shared prefs — only keep packages still installed
    val installedPackages = remember { apps.map { it.packageName }.toSet() }
    val selectedPackages = remember {
        mutableStateOf(
            BlockPreferences.getGrayScreenPackages()
                .filter { it in installedPackages }
                .toMutableSet()
        )
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnBg
                    )
                }
                Text(
                    "Gray Screen Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnBg,
                    modifier = Modifier.weight(1f)
                )
                // Save button
                TextButton(
                    onClick = {
                        BlockPreferences.setGrayScreenPackages(selectedPackages.value)
                        onBack()
                    }
                ) {
                    Text("Save", color = Accent, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Subtitle ──────────────────────────────────────────────────────
            Text(
                "${selectedPackages.value.size} app${if (selectedPackages.value.size != 1) "s" else ""} selected",
                style = MaterialTheme.typography.bodySmall,
                color = OnBgMuted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── App List ──────────────────────────────────────────────────────
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        selected = app.packageName in selectedPackages.value,
                        onToggle = {
                            val updated = selectedPackages.value.toMutableSet()
                            if (app.packageName in updated) updated.remove(app.packageName)
                            else updated.add(app.packageName)
                            selectedPackages.value = updated
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, selected: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        color = if (selected) AccentGlow else Surface0,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                contentDescription = app.name,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(14.dp))

            Text(
                app.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = OnBg,
                modifier = Modifier.weight(1f)
            )

            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Accent,
                    uncheckedColor = OnBgMuted,
                    checkmarkColor = Background
                )
            )
        }
    }
}