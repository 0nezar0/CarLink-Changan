package com.carlink.core.settings

import android.content.Context
import com.shilapi.xcertplay.AirPlayPersistence

/**
 * CarPlay's HEVC (`hevc_enabled`) and who set it. A value the driver set (the Settings toggle,
 * the floating panel's HEVC tile, or an answer to the HEVC question) is remembered here as the
 * driver's choice. The picture policy ([com.carlink.core.connection.CarPlaySettings.apply]) writes
 * HEVC at the start of each session under Automatic picture; it must not turn it back on after the
 * driver chose Off. It may still turn it off for safety (no hardware decoder, a stall, a quirk),
 * and a driver's On is not touched by that: the policy's next "on" is then allowed to write again.
 */
object HevcChoice {
    private const val PREFS = "carlink_hevc"
    private const val KEY_CHOICE = "user_choice"
    private const val ON = "on"
    private const val OFF = "off"

    /** The driver's own choice, or null when they never set it (the stored value then came from a default or the policy). */
    fun userChoice(context: Context): Boolean? =
        when (prefs(context).getString(KEY_CHOICE, null)) {
            ON -> true
            OFF -> false
            else -> null
        }

    /** The driver set HEVC: stores the value for DiPlay and remembers it as their choice. */
    fun setByUser(context: Context, enabled: Boolean) {
        AirPlayPersistence.saveHevcEnabled(context, enabled)
        prefs(context).edit().putString(KEY_CHOICE, if (enabled) ON else OFF).apply()
    }

    /** The policy wants [incoming]; true when the write must be skipped because the driver chose Off. */
    fun blocksPolicyWrite(userChoice: Boolean?, incoming: Boolean): Boolean = incoming && userChoice == false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
