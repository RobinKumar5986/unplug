package com.kgjr.unplug.sharedpref

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BlockedApp(
    val id: String,
    val packageName: String,
    val appName: String,
    val featureName: String,
    val iconRes: Int,
    var isBlocked: Boolean = false
)

object BlockPreferences {
    private const val TAG  = "BlockPreferences"
    private const val PREF = "unplug_block_prefs"

    private const val KEY_BLOCKED_IDS   = "blocked_ids"
    private const val KEY_CHEAT_ENABLED = "cheat_mode_enabled"
    private const val KEY_SESSION_START = "cheat_session_start"
    private const val KEY_LAST_ACTIVITY = "cheat_last_activity"

    private lateinit var prefs: SharedPreferences

    private val _blockedIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedIds: StateFlow<Set<String>> = _blockedIds

    private val _cheatEnabled = MutableStateFlow(false)
    val cheatEnabled: StateFlow<Boolean> = _cheatEnabled

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        _blockedIds.value = prefs.getStringSet(KEY_BLOCKED_IDS, emptySet()) ?: emptySet()
        _cheatEnabled.value = prefs.getBoolean(KEY_CHEAT_ENABLED, false)
        Log.d(TAG, "Initialized: Blocked=${_blockedIds.value}, CheatMode=${_cheatEnabled.value}")
    }

    fun isBlocked(id: String): Boolean = _blockedIds.value.contains(id)

    fun setBlocked(id: String, blocked: Boolean) {
        val updated = _blockedIds.value.toMutableSet()
        if (blocked) updated.add(id) else updated.remove(id)
        _blockedIds.value = updated
        prefs.edit().putStringSet(KEY_BLOCKED_IDS, updated).apply()
        Log.d(TAG, "Block state changed: $id -> $blocked")
    }

    fun setCheatMode(enabled: Boolean) {
        _cheatEnabled.value = enabled
        prefs.edit().putBoolean(KEY_CHEAT_ENABLED, enabled).apply()
        Log.d(TAG, "Cheat Mode toggled: $enabled")
    }

    var sessionStartTime: Long
        get() = prefs.getLong(KEY_SESSION_START, 0L)
        set(value) {
            prefs.edit().putLong(KEY_SESSION_START, value).apply()
        }

    var lastActivityTime: Long
        get() = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY, value).apply()
        }
}