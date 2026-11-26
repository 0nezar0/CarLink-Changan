package com.carlink.app.ui

import android.content.Context
import com.carlink.app.ui.theme.ThemeMode

/**
 * v1.6.5 (versionCode 10041): CarLink's own screens default to dark mode for every user
 * and every car entry.
 *
 * Fresh installs have no `theme_mode` key, so they get [ThemeMode.DARK]. Existing installs
 * are switched once only when that key is still missing — the untouched old default was
 * "follow the car". An explicit Light (or Dark, or Follow the car) choice is left alone.
 */
object DarkThemeDefaultMigration {
    const val VERSION_CODE = 10041
    const val KEY_APPLIED = "dark_theme_default_v10041"

    fun apply(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(UiPreferences.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_APPLIED, false)) return
        val edit = prefs.edit().putBoolean(KEY_APPLIED, true)
        if (!prefs.contains(UiPreferences.KEY_THEME)) {
            edit.putString(UiPreferences.KEY_THEME, ThemeMode.DARK.name)
        }
        edit.apply()
    }
}
