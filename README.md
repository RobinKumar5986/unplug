# Unplug 📵

**Unplug** is a lightweight Android productivity tool designed to break the cycle of infinite scrolling. By leveraging Android's Accessibility Service, it intelligently monitors and limits time spent on short-form content like **Instagram Reels** and **YouTube Shorts**.

## ✨ Features

-   **Intelligent Interception:** Detects specific UI elements (e.g., `reel_recycler`, `clips_tab`) to target only addictive "discovery" feeds while leaving the rest of the app functional.
-   **Smart Cheat Mode:** A quota-based system that allows a 15-minute daily "grace window" for entertainment.
-   **True Quota Logic:** The timer only counts while you are actively viewing content. Closing the app or switching to a chat "pauses" your 15-minute bank.
-   **Loop Awareness:** Accurately tracks time even if you watch a single video on repeat without scrolling.
-   **Hard Cooldown:** Once the 15-minute quota is exhausted, the app enforces a strict 90-minute break before the quota resets.

## 🛠 Tech Stack

-   **Language:** Kotlin
-   **Framework:** Android SDK
-   **Core Components:** -   `AccessibilityService`: For real-time UI monitoring and interaction.
    -   `StateFlow`: For reactive preference updates.
    -   `SharedPreferences`: For persistent storage of usage metrics.

## 🧠 How the Quota Logic Works

Unlike a standard countdown timer, Unplug uses a **Delta-based Accumulation** strategy:

1.  **Entry:** When a Reel/Short is detected, a `lastScreenEnterTime` anchor is set.
2.  **Active Tracking:** On every UI event (scroll, loop, or content change), the difference between *now* and the *anchor* is added to the `accumulatedTime`.
3.  **The Pause:** If the user leaves the screen, the anchor is nullified, effectively pausing the "spending" of the 15-minute bank.
4.  **The Reset:** The 15-minute bank only refills if the user stays away from the targeted content for a continuous **90 minutes**.

## 🚀 Getting Started

### Prerequisites
-   Android Device (API 24+)
-   Accessibility Permissions enabled for "Unplug Service"

### Installation
1. Clone the repository.
2. Build the APK using Android Studio.
3. Install on your device.
4. Open **Settings > Accessibility** and toggle **Unplug** to ON.

## 🛡 Disclaimer

This app uses the `AccessibilityService` API solely to detect when specific "Reels" or "Shorts" UI components are visible on the screen. It does not collect, store, or transmit any personal user data or keystrokes.

---
*Made with ❤️ to reclaim your focus.*
