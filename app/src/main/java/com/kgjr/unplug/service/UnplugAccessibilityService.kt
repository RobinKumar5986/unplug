package com.kgjr.unplug.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kgjr.unplug.helper.BlockPreferences

/**
 * Core accessibility service that runs even when the Unplug app is closed.
 *
 * Detection strategy
 * ──────────────────
 * Instagram Reels  → look for the BottomNavigationView / tab-bar child whose
 *                    content description contains "Reels" while the foreground
 *                    package is com.instagram.android.
 *
 * YouTube Shorts   → look for a node whose resource-id ends with
 *                    "shorts_pivot_item" OR whose content-description contains
 *                    "Shorts" while the foreground package is com.google.android.youtube.
 *
 * When detected the service performs a GLOBAL_ACTION_BACK to navigate the user
 * away from the short-form content section.
 *
 * All screen content is logged for debugging (TAG = UnplugService).
 */
class UnplugAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "UnplugService"

        // Package names
        private const val PKG_INSTAGRAM = "com.instagram.android"
        private const val PKG_YOUTUBE   = "com.google.android.youtube"

        // Text / description signals (lower-cased for comparison)
        private val INSTAGRAM_REELS_SIGNALS = listOf("reels", "reel")
        private val YOUTUBE_SHORTS_SIGNALS  = listOf("shorts", "short")

        // Resource-id fragments
        private val YOUTUBE_SHORTS_IDS = listOf("shorts_pivot_item", "shorts_shelf")
    }

    private var lastBlockedPkg = ""
    private var lastBlockedTime = 0L

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Service connected")
        BlockPreferences.init(applicationContext)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = arrayOf(PKG_INSTAGRAM, PKG_YOUTUBE)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    // ── Event handling ────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return

        // Log all screen content for debugging
        logEvent(event, pkg)

        when (pkg) {
            PKG_INSTAGRAM -> handleInstagram(event)
            PKG_YOUTUBE   -> handleYouTube(event)
        }
    }

    // ── Instagram ─────────────────────────────────────────────────────────────

    private fun handleInstagram(event: AccessibilityEvent) {
        if (!BlockPreferences.isBlocked("instagram_reels")) return

        val root = rootInActiveWindow ?: return
        val reelsDetected = findNodeByDescriptions(root, INSTAGRAM_REELS_SIGNALS, selected = true)
            || findNodeByDescriptions(root, INSTAGRAM_REELS_SIGNALS, className = "android.widget.FrameLayout")

        if (reelsDetected) {
            Log.w(TAG, "Instagram Reels DETECTED — navigating back")
            throttledBack(PKG_INSTAGRAM)
        }
        root.recycle()
    }

    // ── YouTube ───────────────────────────────────────────────────────────────

    private fun handleYouTube(event: AccessibilityEvent) {
        if (!BlockPreferences.isBlocked("youtube_shorts")) return

        val root = rootInActiveWindow ?: return

        val shortsDetected =
            findNodeByResourceIds(root, YOUTUBE_SHORTS_IDS) ||
            findNodeByDescriptions(root, YOUTUBE_SHORTS_SIGNALS, selected = true)

        if (shortsDetected) {
            Log.w(TAG, "YouTube Shorts DETECTED — navigating back")
            throttledBack(PKG_YOUTUBE)
        }
        root.recycle()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Walk the node tree looking for a node whose contentDescription or text
     * contains any of [signals] (case-insensitive).
     * Optionally require [selected] == true or match a specific [className].
     */
    private fun findNodeByDescriptions(
        root: AccessibilityNodeInfo,
        signals: List<String>,
        selected: Boolean? = null,
        className: String? = null
    ): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""

            val matchesSignal = signals.any { sig -> desc.contains(sig) || text.contains(sig) }
            val matchesSelected = selected == null || node.isSelected == selected
            val matchesClass = className == null || node.className?.toString() == className

            if (matchesSignal && matchesSelected && matchesClass) {
                Log.d(TAG, "  ✓ node matched desc='$desc' text='$text' selected=${node.isSelected}")
                return true
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    /**
     * Walk the node tree looking for a node whose viewIdResourceName contains
     * any of [ids].
     */
    private fun findNodeByResourceIds(
        root: AccessibilityNodeInfo,
        ids: List<String>
    ): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val resId = node.viewIdResourceName?.lowercase() ?: ""

            if (ids.any { resId.contains(it) }) {
                Log.d(TAG, "  ✓ resourceId matched: $resId")
                return true
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    /** Prevent spamming back() — one back per 2 seconds per package. */
    private fun throttledBack(pkg: String) {
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockedTime < 2_000) return
        lastBlockedPkg = pkg
        lastBlockedTime = now
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    // ── Logging ───────────────────────────────────────────────────────────────

    private fun logEvent(event: AccessibilityEvent, pkg: String) {
        val type = AccessibilityEvent.eventTypeToString(event.eventType)
        val cls  = event.className?.toString() ?: "?"
        val text = event.text.joinToString("|")
        Log.v(TAG, "[$pkg] $type cls=$cls text=$text")

        // Optionally log the full view tree on window state changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            rootInActiveWindow?.let { root ->
                Log.v(TAG, "── View tree ──────────────────")
                logTree(root, 0)
                root.recycle()
            }
        }
    }

    private fun logTree(node: AccessibilityNodeInfo, depth: Int) {
        val indent = "  ".repeat(depth)
        val desc  = node.contentDescription ?: ""
        val text  = node.text ?: ""
        val resId = node.viewIdResourceName ?: ""
        val cls   = node.className ?: ""
        Log.v(TAG, "$indent[$cls] desc='$desc' text='$text' id='$resId' selected=${node.isSelected}")
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                logTree(child, depth + 1)
                child.recycle()
            }
        }
    }
}