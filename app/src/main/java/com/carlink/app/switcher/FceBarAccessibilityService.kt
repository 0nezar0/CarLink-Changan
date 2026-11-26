package com.carlink.app.switcher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityWindowInfo
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Build
import android.view.inputmethod.InputMethodManager

/**
 * Foreground-app watcher used by the Changan status-bar hide modes, following Bar Hider 2.0:
 * [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED], then [FceSystemBar] hide/show broadcasts.
 * Hide is sent again on every change (and on a short timer) because FceSystemUI re-shows the
 * bar when the top package is not on its whitelist.
 *
 * Unlike Bar Hider, this service does not ignore CarLink's own package: both hide modes keep
 * the bar hidden while a CarLink screen or projection is in front.
 */
class FceBarAccessibilityService : AccessibilityService() {

    private val navigationOverlay by lazy { Cs75NavigationOverlay(this) }
    private val handler = Handler(Looper.getMainLooper())
    private var lastSentHide: Boolean? = null
    private var lastTrackedPkg: String? = null

    private val rehideRunnable = Runnable {
        if (lastSentHide == true) {
            FceSystemBar.apply(this, hide = true, reason = "timer")
            scheduleRehide()
        }
    }

    override fun onServiceConnected() {
        navigationOverlay.start()
        instance = this
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        Log.i(FceSystemBar.TAG, "accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) trackProjection(event)
        val pkg = foregroundPackage() ?: event.packageName?.toString() ?: return
        if (shouldIgnore(pkg)) return
        // WINDOWS_CHANGED is frequent; the rehide timer already covers FceSystemUI re-showing the bar.
        if (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED && pkg == lastTrackedPkg) return
        lastTrackedPkg = pkg
        applyForPackage(pkg, reason = "window:$pkg")
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        navigationOverlay.stop()
        handler.removeCallbacks(rehideRunnable)
        lastSentHide = null
        lastTrackedPkg = null
        if (instance === this) instance = null
        Log.i(FceSystemBar.TAG, "accessibility service unbound")
        return super.onUnbind(intent)
    }

    /**
     * Tells the navigation overlay whether CarPlay or Android Auto is in front, from the activity
     * named by a window-state event. Dialogs, popups and System UI leave the last answer as it is.
     */
    private fun trackProjection(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (shouldIgnore(pkg)) return
        val cls = event.className?.toString() ?: return
        val inFront = when {
            pkg != packageName -> false
            cls in PROJECTION_ACTIVITIES -> true
            cls.endsWith("Activity") && !cls.startsWith("android.") -> false
            else -> return
        }
        navigationOverlay.setProjectionInFront(inFront)
    }

    internal fun applyForPackage(pkg: String, reason: String) {
        val hide = FceSystemBar.shouldHideForPackage(this, pkg)
        handler.removeCallbacks(rehideRunnable)
        if (hide) {
            FceSystemBar.apply(this, hide = true, reason = reason)
            lastSentHide = true
            scheduleRehide()
        } else if (lastSentHide != false) {
            FceSystemBar.apply(this, hide = false, reason = reason)
            lastSentHide = false
        }
    }

    private fun scheduleRehide() {
        handler.removeCallbacks(rehideRunnable)
        handler.postDelayed(rehideRunnable, FceSystemBar.REHIDE_INTERVAL_MS)
    }

    /**
     * The app the driver actually sees: prefer the focused/active application window, then
     * the event package. System UI and IME packages are ignored by the caller.
     */
    internal fun foregroundPackage(): String? {
        val windows = runCatching { windows }.getOrNull().orEmpty()
        val preferred = windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        } ?: windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isActive
        }
        preferred?.let { packageOf(it) }?.let { return it }
        return packageOf(runCatching { rootInActiveWindow }.getOrNull())
    }

    private fun packageOf(window: AccessibilityWindowInfo): String? {
        val root = runCatching { window.root }.getOrNull()
        return packageOf(root)
    }

    private fun packageOf(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        return try {
            node.packageName?.toString()
        } finally {
            if (Build.VERSION.SDK_INT < 33) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }
    }

    private fun shouldIgnore(pkg: String): Boolean {
        if (pkg == "com.android.systemui" || pkg == "com.android.fcesystemui") return true
        return isIme(pkg)
    }

    private fun isIme(pkg: String): Boolean {
        val imm = getSystemService(InputMethodManager::class.java) ?: return false
        return imm.inputMethodList.any { it.packageName == pkg }
    }

    companion object {
        /** CarPlay and Android Auto projection screens. */
        private val PROJECTION_ACTIVITIES = setOf(
            "com.shilapi.xcertplay.CarPlayHostActivity",
            "com.shilapi.xcertplay.SameTaskCarPlayHostActivity",
            "com.shilapi.xcertplay.DiPlayActivity",
            "com.andrerinas.openheadunit.aap.AapProjectionActivity",
        )

        @Volatile
        private var instance: FceBarAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, FceBarAccessibilityService::class.java)
            val am = context.getSystemService(AccessibilityManager::class.java)
            val enabled = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            if (enabled != null && enabled.any { ComponentName.unflattenFromString(it.id) == expected }) {
                return true
            }
            val raw = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val want = expected.flattenToString()
            return raw.split(':').any { it.equals(want, ignoreCase = true) }
        }

        /** Re-apply the current setting (for example after the driver changes mode or the app list). */
        fun refresh(context: Context) {
            val service = instance ?: return
            val pkg = service.foregroundPackage() ?: context.packageName
            service.applyForPackage(pkg, reason = "setting-changed")
        }
    }
}
