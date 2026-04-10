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

    private const val KEY_BLOCKED_IDS      = "blocked_ids"
    private const val KEY_CHEAT_ENABLED    = "cheat_mode_enabled"
    private const val KEY_LAST_BLOCK_TIME  = "cheat_last_block_time"   // when cheat window exhausted & block started
    private const val KEY_ACCUMULATED_TIME = "cheat_accumulated_time"  // scroll time used in current window

    private const val KEY_GRAY_SCREEN_PACKAGES = "gray_screen_packages"

    private lateinit var prefs: SharedPreferences

    private val _blockedIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedIds: StateFlow<Set<String>> = _blockedIds

    private val _cheatEnabled = MutableStateFlow(false)
    val cheatEnabled: StateFlow<Boolean> = _cheatEnabled

    private val _accumulatedTime = MutableStateFlow(0L)
    val accumulatedTimeFlow: StateFlow<Long> = _accumulatedTime

    // Exposed so UI can show "unlocks in X" countdown
    private val _lastBlockTime = MutableStateFlow(0L)
    val lastBlockTimeFlow: StateFlow<Long> = _lastBlockTime

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        _blockedIds.value     = prefs.getStringSet(KEY_BLOCKED_IDS, emptySet()) ?: emptySet()
        _cheatEnabled.value   = prefs.getBoolean(KEY_CHEAT_ENABLED, false)
        _accumulatedTime.value = prefs.getLong(KEY_ACCUMULATED_TIME, 0L)
        _lastBlockTime.value  = prefs.getLong(KEY_LAST_BLOCK_TIME, 0L)
        Log.d(TAG, "Init: blocked=${_blockedIds.value}, cheat=${_cheatEnabled.value}, accumulated=${_accumulatedTime.value}, lastBlock=${_lastBlockTime.value}")
    }

    fun isBlocked(id: String): Boolean = _blockedIds.value.contains(id)

    fun setBlocked(id: String, blocked: Boolean) {
        val updated = _blockedIds.value.toMutableSet()
        if (blocked) updated.add(id) else updated.remove(id)
        _blockedIds.value = updated
        prefs.edit().putStringSet(KEY_BLOCKED_IDS, updated).apply()
        Log.d(TAG, "Block state: $id -> $blocked")
    }

    fun setCheatMode(enabled: Boolean) {
        _cheatEnabled.value = enabled
        prefs.edit().putBoolean(KEY_CHEAT_ENABLED, enabled).apply()
        Log.d(TAG, "Cheat mode -> $enabled")
    }

    fun getGrayScreenPackages(): Set<String> =
        prefs.getStringSet(KEY_GRAY_SCREEN_PACKAGES, emptySet()) ?: emptySet()

    fun setGrayScreenPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_GRAY_SCREEN_PACKAGES, packages).apply()
        Log.d(TAG, "Gray screen packages saved: $packages")
    }

    // Timestamp of when the cheat window was exhausted and blocking began
    var lastBlockTime: Long
        get() = prefs.getLong(KEY_LAST_BLOCK_TIME, 0L)
        set(value) {
            _lastBlockTime.value = value
            prefs.edit().putLong(KEY_LAST_BLOCK_TIME, value).apply()
        }

    var accumulatedTime: Long
        get() = prefs.getLong(KEY_ACCUMULATED_TIME, 0L)
        set(value) {
            _accumulatedTime.value = value
            prefs.edit().putLong(KEY_ACCUMULATED_TIME, value).apply()
        }
}