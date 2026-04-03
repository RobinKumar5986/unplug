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
        private const val CHEAT_WINDOW_MS      = 15 * 60 * 1000L
        private const val COOLDOWN_REQUIRED_MS = 90 * 60 * 1000L
    }

    private var lastBlockedPkg      = ""
    private var lastBlockedTime     = 0L
    private var lastScreenEnterTime: Long? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "✅ Service connected")
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
        val pkg  = event?.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return

        try {
            val inShorts = when (pkg) {
                PKG_YOUTUBE   -> findNodeById(root, YT_REEL_RECYCLER)
                PKG_INSTAGRAM -> findNodeByIdSelected(root, IG_CLIPS_TAB, true)
                else          -> false
            }

            if (inShorts) handleInterception(pkg)
            else lastScreenEnterTime = null

        } finally {
            root.recycle()
        }
    }

    private fun handleInterception(pkg: String) {
        val id = if (pkg == PKG_YOUTUBE) "youtube_shorts" else "instagram_reels"
        if (!BlockPreferences.isBlocked(id)) return
        if (shouldBlock(pkg)) {
            Log.w(TAG, "🚫 Blocking $pkg")
            throttledBack(pkg)
        }
    }

    private fun shouldBlock(pkg: String): Boolean {
        if (!BlockPreferences.cheatEnabled.value) return true

        val now           = System.currentTimeMillis()
        val lastBlockTime = BlockPreferences.lastBlockTime

        // Still within the 1.5hr penalty window → block without updating anything
        if (lastBlockTime > 0 && now - lastBlockTime < COOLDOWN_REQUIRED_MS) {
            val waitLeft = (COOLDOWN_REQUIRED_MS - (now - lastBlockTime)) / 1000 / 60
            Log.v(TAG, "🛑 In penalty window. $waitLeft min left.")
            return true
        }

        // Cooldown passed (or never set) → user gets/has a fresh 15-min window
        if (lastBlockTime > 0 && now - lastBlockTime >= COOLDOWN_REQUIRED_MS) {
            Log.i(TAG, "✨ Penalty expired. Resetting for $pkg")
            BlockPreferences.accumulatedTime = 0L
            BlockPreferences.lastBlockTime   = 0L
            lastScreenEnterTime              = now
        }

        // Accumulate time the user has spent in shorts this session
        val delta = lastScreenEnterTime?.let { now - it } ?: 0L
        val total = BlockPreferences.accumulatedTime + delta
        lastScreenEnterTime = now

        if (total < CHEAT_WINDOW_MS) {
            val remaining = (CHEAT_WINDOW_MS - total) / 1000 / 60
            Log.v(TAG, "✅ Within window. $remaining min left for $pkg")
            BlockPreferences.accumulatedTime = total
            return false
        }

        // Window exhausted → record the block start time and block
        Log.w(TAG, "⏰ Cheat window exhausted for $pkg. Starting 1.5hr penalty.")
        BlockPreferences.lastBlockTime   = now
        BlockPreferences.accumulatedTime = total
        lastScreenEnterTime              = null
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