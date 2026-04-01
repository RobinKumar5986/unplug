package com.kgjr.unplug.helper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ── Model ─────────────────────────────────────────────────────────────────────

data class BlockedApp(
    val id: String,            // unique key stored in prefs
    val packageName: String,
    val appName: String,
    val featureName: String,   // e.g. "Reels", "Shorts"
    val iconRes: Int,          // drawable resource id
    val urlPatterns: List<String> = emptyList(),   // for future URL blocking
    var isBlocked: Boolean = false
)

// ── Repository / Preferences ──────────────────────────────────────────────────

object BlockPreferences {
    private const val TAG  = "BlockPreferences"
    private const val PREF = "unplug_block_prefs"

    private lateinit var prefs: SharedPreferences

    // Backing state so UI can observe
    private val _blockedIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedIds: StateFlow<Set<String>> = _blockedIds

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("blocked_ids", emptySet()) ?: emptySet()
        _blockedIds.value = saved
        Log.d(TAG, "Initialized — blocked: $saved")
    }

    fun isBlocked(id: String): Boolean = _blockedIds.value.contains(id)

    fun setBlocked(id: String, blocked: Boolean) {
        val updated = _blockedIds.value.toMutableSet()
        if (blocked) updated.add(id) else updated.remove(id)
        _blockedIds.value = updated
        prefs.edit().putStringSet("blocked_ids", updated).apply()
        Log.d(TAG, "setBlocked id=$id blocked=$blocked → current=$updated")
    }

    fun getBlockedSet(): Set<String> = _blockedIds.value
}