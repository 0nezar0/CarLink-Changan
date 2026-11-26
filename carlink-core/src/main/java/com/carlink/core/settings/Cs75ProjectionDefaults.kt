package com.carlink.core.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.carlink.core.engine.AndroidAutoSettings
import com.shilapi.xcertplay.AirPlayPersistence

/**
 * CarPlay and Android Auto starting values for [CarBrand.CHANGAN_CS75_PLUS] only.
 *
 * Applied the first time that entry is selected, and once when a saved generic Changan
 * choice is migrated to it:
 * - CarPlay width **400 mm** (Advanced setting `cp_width`, the "400 mm" option).
 * - CarPlay **HEVC** on (Advanced setting `cp_hevc`).
 * - Android Auto pixel density **200** (Advanced setting `aa_dpi`, key `dpi-pixel-density`).
 * - Car status-bar hide **on** (all apps except the car's home screen), with no question asked.
 *   Written once ([KEY_HIDE_BAR_DEFAULT_APPLIED]) and only when the driver never stored a hide
 *   mode, so an explicit Off, or a saved mode, is kept. It also reaches a CS75 Plus choice that
 *   was made before this default existed.
 *
 * Values the driver already set in Advanced are left alone. Other brands, including generic
 * [CarBrand.CHANGAN], are not written. A one-shot flag in [CarBrandStore.PREFS] stops a later
 * explicit change from being overwritten. Interface size is set to [InterfaceSize.CUSTOM] when
 * the width or DPI default is written, so [com.carlink.core.connection.CarPlaySettingsPolicy]
 * and Android Auto session mapping do not replace them with the Medium preset on the next
 * connection.
 */
object Cs75ProjectionDefaults {
    const val CARPLAY_WIDTH_MM = 400
    const val ANDROID_AUTO_DPI = 200
    const val KEY_APPLIED = "cs75_projection_defaults_applied"
    const val KEY_HIDE_BAR_DEFAULT_APPLIED = "cs75_hide_bar_default_applied"

    private const val AIRPLAY_PREFS = "xcertplay_airplay"
    private const val KEY_WIDTH_PHYSICAL_MM = "display_width_physical_mm"
    private const val KEY_HEVC_ENABLED = "hevc_enabled"
    const val AA_DPI_KEY = "dpi-pixel-density"

    fun applyIfUnset(context: Context) {
        val app = context.applicationContext
        val flagPrefs = app.getSharedPreferences(CarBrandStore.PREFS, Context.MODE_PRIVATE)
        applyHideBarDefaultOnce(app, flagPrefs)
        if (flagPrefs.getBoolean(KEY_APPLIED, false)) return

        val store = UnifiedSettingsStore(app)
        val settings = store.load()
        val air = airplayPrefs(app)
        val custom = settings.interfaceSize == InterfaceSize.CUSTOM

        // Size presets rewrite width and DPI every session unless the size is Custom, so a
        // non-Custom size means those values came from the preset, not from Advanced.
        if (!custom) {
            AirPlayPersistence.saveWidthPhysicalMm(app, CARPLAY_WIDTH_MM)
            store.save(settings.copy(interfaceSize = InterfaceSize.CUSTOM))
            writeAaDpi(app, onlyIfUnset = false)
        } else {
            if (!air.contains(KEY_WIDTH_PHYSICAL_MM)) {
                AirPlayPersistence.saveWidthPhysicalMm(app, CARPLAY_WIDTH_MM)
            }
            writeAaDpi(app, onlyIfUnset = true)
        }

        if (!air.contains(KEY_HEVC_ENABLED)) {
            AirPlayPersistence.saveHevcEnabled(app, true)
        }

        flagPrefs.edit().putBoolean(KEY_APPLIED, true).apply()
    }

    private fun applyHideBarDefaultOnce(app: Context, flagPrefs: SharedPreferences) {
        if (flagPrefs.getBoolean(KEY_HIDE_BAR_DEFAULT_APPLIED, false)) return
        val store = UnifiedSettingsStore(app)
        if (!store.hasHideCarStatusBarMode()) {
            store.save(store.load().copy(hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER))
        }
        flagPrefs.edit().putBoolean(KEY_HIDE_BAR_DEFAULT_APPLIED, true).apply()
    }

    /**
     * When Automatic picture wants to turn HEVC on, keep a stored off value for CS75
     * (the driver already chose, or a previous policy write is already in prefs).
     * Incoming `false` is still written, so [com.carlink.core.connection.CarPlayHevcMemory]
     * can fall back to H.264 after a failed attempt.
     */
    fun shouldSuppressHevcWrite(context: Context, incoming: Boolean): Boolean {
        if (!incoming || !isCs75Selected(context)) return false
        val prefs = airplayPrefs(context)
        return prefs.contains(KEY_HEVC_ENABLED) && !AirPlayPersistence.loadHevcEnabled(context)
    }

    /** Keep a width already stored for CS75, so a size preset cannot replace 400 mm or a typed value. */
    fun shouldPreserveWidth(context: Context): Boolean {
        if (!isCs75Selected(context)) return false
        return airplayPrefs(context).contains(KEY_WIDTH_PHYSICAL_MM)
    }

    private fun isCs75Selected(context: Context): Boolean =
        CarBrandStore(context).chosen()?.isChanganCs75Plus == true

    private fun airplayPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(AIRPLAY_PREFS, Context.MODE_PRIVATE)

    private fun writeAaDpi(context: Context, onlyIfUnset: Boolean) {
        runCatching {
            val existing = AndroidAutoSettings.read(context, listOf(AA_DPI_KEY))
            val present = existing.containsKey(AA_DPI_KEY)
            val value = if (present) existing.getInt(AA_DPI_KEY) else null
            val write = if (onlyIfUnset) !present else (!present || value == 0)
            if (write) {
                AndroidAutoSettings.write(context, Bundle().apply { putInt(AA_DPI_KEY, ANDROID_AUTO_DPI) })
            }
        }
    }
}
