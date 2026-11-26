package com.carlink.app.switcher

import android.content.Context
import android.content.Intent
import android.util.Log
import com.carlink.core.settings.CarBrand
import com.carlink.core.settings.CarBrandStore
import com.carlink.core.settings.HideCarStatusBarMode
import com.carlink.core.settings.UnifiedSettings
import com.carlink.core.settings.UnifiedSettingsStore

/**
 * Hides or shows the Changan CS75 Plus head unit's floating FceSystemUI status bar
 * (`com.android.fcesystemui`) by the same implicit broadcasts Bar Hider uses.
 *
 * Only used when the driver has chosen [CarBrand.CHANGAN_CS75_PLUS] and a hide mode other than Off.
 * The receiver is registered by the stock System UI with no broadcast permission.
 *
 * FceSystemUI re-shows the bar when the task stack changes if the top package is not on its
 * whitelist (CarLink, including `com.carlink.app.hudtest`, is not). Hide is therefore sent
 * again on every foreground-app change and on a short timer.
 */
object FceSystemBar {
    const val ACTION_HIDE = "com_fce_system_statusbar_hide"
    const val ACTION_SHOW = "com_fce_system_statusbar_show"
    const val TAG = "CarLinkFceBar"

    /** Same interval Bar Hider used, so a re-show from System UI is hidden again quickly. */
    const val REHIDE_INTERVAL_MS = 500L

    /** The CS75 Plus home/launcher packages: Mode 1 leaves the bar visible on these. */
    val LAUNCHER_PACKAGES: Set<String> = setOf(
        "com.faurecia.app.in",
        "com.incall.apps.in",
    )

    fun modeEnabled(mode: HideCarStatusBarMode): Boolean = mode != HideCarStatusBarMode.OFF

    fun shouldHide(changanSelected: Boolean, settingEnabled: Boolean, projectionInFront: Boolean): Boolean =
        changanSelected && settingEnabled && projectionInFront

    fun changanAndEnabled(context: Context): Boolean {
        if (CarBrandStore(context).chosen()?.isChanganCs75Plus != true) return false
        return modeEnabled(UnifiedSettingsStore(context).load().hideCarStatusBarMode)
    }

    fun shouldHide(context: Context, projectionInFront: Boolean): Boolean = shouldHide(
        changanSelected = CarBrandStore(context).chosen()?.isChanganCs75Plus == true,
        settingEnabled = modeEnabled(UnifiedSettingsStore(context).load().hideCarStatusBarMode),
        projectionInFront = projectionInFront,
    )

    /**
     * Whether the bar should be hidden while [packageName] is the foreground app.
     * CarLink itself is always hidden in both on modes.
     */
    fun shouldHideForPackage(
        packageName: String,
        mode: HideCarStatusBarMode,
        selectedPackages: Set<String>,
        ownPackage: String,
    ): Boolean {
        if (mode == HideCarStatusBarMode.OFF) return false
        if (packageName == ownPackage) return true
        return when (mode) {
            HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER -> packageName !in LAUNCHER_PACKAGES
            HideCarStatusBarMode.SELECTED_APPS -> packageName in selectedPackages
            HideCarStatusBarMode.OFF -> false
        }
    }

    fun shouldHideForPackage(context: Context, packageName: String): Boolean {
        if (CarBrandStore(context).chosen()?.isChanganCs75Plus != true) return false
        val settings: UnifiedSettings = UnifiedSettingsStore(context).load()
        return shouldHideForPackage(
            packageName = packageName,
            mode = settings.hideCarStatusBarMode,
            selectedPackages = settings.hideCarStatusBarPackages,
            ownPackage = context.packageName,
        )
    }

    fun apply(context: Context, hide: Boolean, reason: String) {
        val action = if (hide) ACTION_HIDE else ACTION_SHOW
        context.sendBroadcast(Intent(action))
        Log.i(TAG, "sent $action reason=$reason package=${context.packageName}")
    }
}
