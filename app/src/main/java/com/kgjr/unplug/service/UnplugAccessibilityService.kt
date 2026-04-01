package com.kgjr.unplug.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kgjr.unplug.sharedpref.BlockPreferences

@SuppressLint("AccessibilityPolicy")
class UnplugAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG               = "UnplugService"
        private const val PKG_INSTAGRAM     = "com.instagram.android"
        private const val PKG_YOUTUBE       = "com.google.android.youtube"
        private const val YT_REEL_RECYCLER  = "reel_recycler"
        private const val IG_CLIPS_TAB      = "clips_tab"
        private const val BLOCK_COOLDOWN_MS = 2_000L

        // Cheat Mode Logic Constants
        private const val CHEAT_WINDOW_MS      = 15 * 60 * 1000L      // 15 Minutes
        private const val COOLDOWN_REQUIRED_MS = 90 * 60 * 1000L      // 1.5 Hours
    }

    private var lastBlockedPkg  = ""
    private var lastBlockedTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "✅ Unplug Service Connected")
        BlockPreferences.init(applicationContext)
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes          = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType        = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags               = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 300
            packageNames        = arrayOf(PKG_INSTAGRAM, PKG_YOUTUBE)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return

        try {
            when (pkg) {
                PKG_YOUTUBE -> {
                    if (findNodeById(root, YT_REEL_RECYCLER)) handleInterception(PKG_YOUTUBE)
                }
                PKG_INSTAGRAM -> {
                    if (findNodeByIdSelected(root, IG_CLIPS_TAB, true)) handleInterception(PKG_INSTAGRAM)
                }
            }
        } finally {
            root.recycle()
        }
    }

    private fun handleInterception(pkg: String) {
        val id = if(pkg == PKG_YOUTUBE) "youtube_shorts" else "instagram_reels"
        if (!BlockPreferences.isBlocked(id)) return

        if (shouldBlockByCheatLogic(pkg)) {
            Log.w(TAG, "🚫 Block triggered for $pkg")
            throttledBack(pkg)
        }
    }

    private fun shouldBlockByCheatLogic(pkg: String): Boolean {
        if (!BlockPreferences.cheatEnabled.value) {
            Log.v(TAG, "Cheat mode OFF: Immediate block for $pkg")
            return true
        }

        val now = System.currentTimeMillis()
        val sessionStart = BlockPreferences.sessionStartTime
        val lastActivity = BlockPreferences.lastActivityTime

        val timeSinceLastActivity = now - lastActivity
        val timeInSession = now - sessionStart

        // 1. Reset check: If they haven't scrolled for 1.5 hours, give them a fresh 15 mins.
        if (timeSinceLastActivity > COOLDOWN_REQUIRED_MS) {
            Log.i(TAG, "✨ Cooldown expired. Starting fresh 15-min session for $pkg")
            BlockPreferences.sessionStartTime = now
            BlockPreferences.lastActivityTime = now
            return false
        }

        // 2. Window check: Are they within their 15-minute allowance?
        if (timeInSession < CHEAT_WINDOW_MS) {
            val remaining = (CHEAT_WINDOW_MS - timeInSession) / 1000 / 60
            Log.v(TAG, "✅ Within cheat window. $remaining mins left. Updating activity for $pkg.")
            BlockPreferences.lastActivityTime = now // Update activity so the 1.5hr timer starts from NOW
            return false
        }

        // 3. Block: They are past 15 mins and haven't waited 1.5 hours yet.
        val waitLeft = (COOLDOWN_REQUIRED_MS - timeSinceLastActivity) / 1000 / 60
        Log.w(TAG, "🛑 Session expired. Must wait $waitLeft more minutes to reset session.")
        return true
    }

    private fun throttledBack(pkg: String) {
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockedTime < BLOCK_COOLDOWN_MS) return
        lastBlockedPkg  = pkg
        lastBlockedTime = now
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun findNodeById(root: AccessibilityNodeInfo, idFragment: String): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if ((node.viewIdResourceName ?: "").contains(idFragment)) return true
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return false
    }

    private fun findNodeByIdSelected(root: AccessibilityNodeInfo, id: String, sel: Boolean): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if ((node.viewIdResourceName ?: "").contains(id) && node.isSelected == sel) return true
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return false
    }

    override fun onInterrupt() {}
}