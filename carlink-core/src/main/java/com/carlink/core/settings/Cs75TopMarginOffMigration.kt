package com.carlink.core.settings

import android.content.Context

/**
 * v1.6.5 (versionCode 10041): the CS75 Plus 48 px top-margin switch defaults to off.
 *
 * Earlier builds turned it on automatically when the driver answered Yes to the first-launch
 * CS75 question (together with status-bar hide in every app except the launcher). That stored
 * on-value is reset once, and only when it still matches that old default package — not when
 * the driver turned the margin on alone (bar hide still off) or picked a custom hide mode.
 *
 * A fresh CS75 selection no longer writes the margin on. The one-shot flag lives in
 * [CarBrandStore.PREFS] so an update from 10040 applies it once.
 */
object Cs75TopMarginOffMigration {
    const val VERSION_CODE = 10041
    const val KEY_APPLIED = "cs75_top_margin_off_v10041"

    fun apply(context: Context) {
        val app = context.applicationContext
        val flagPrefs = app.getSharedPreferences(CarBrandStore.PREFS, Context.MODE_PRIVATE)
        if (flagPrefs.getBoolean(KEY_APPLIED, false)) return

        if (CarBrandStore(app).chosen()?.isChanganCs75Plus == true) {
            val store = UnifiedSettingsStore(app)
            val settings = store.load()
            if (cameFromOldDefault(settings)) {
                // Write only this key so a full save cannot materialize other defaults
                // (for example the v1.6.5 Bluetooth audio default) as if they were chosen.
                app.getSharedPreferences(UnifiedSettingsStore.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(UnifiedSettingsStore.KEY_TOP_MARGIN, false)
                    .apply()
            }
        }
        // Flag last: a crash before this leaves the migration free to run again.
        flagPrefs.edit().putBoolean(KEY_APPLIED, true).apply()
    }

    /** True when [UnifiedSettings.topMargin] is on only as part of the old first-launch defaults. */
    fun cameFromOldDefault(settings: UnifiedSettings): Boolean =
        settings.topMargin && settings.hideCarStatusBarMode == HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER
}
