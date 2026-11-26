package com.carlink.core.settings

import android.content.Context

/**
 * v1.6.5 (versionCode 10041): Settings › Sound plays defaults to the car's Bluetooth
 * for every user and every car entry.
 *
 * Fresh installs have no `sound_car_bluetooth` key, so they get Bluetooth. Existing
 * installs are switched once only when that key is still missing — the untouched old
 * default was "through CarLink". An explicit Through-CarLink or Bluetooth choice is
 * left alone. The flag is written last so a crash can retry the migration.
 *
 * Apply this before any [UnifiedSettingsStore.save] of a full snapshot, so a
 * neighboring migration cannot persist the old default as if the driver chose it.
 */
object BluetoothAudioDefaultMigration {
    const val VERSION_CODE = 10041
    const val KEY_APPLIED = "bluetooth_audio_default_v10041"

    fun apply(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(UnifiedSettingsStore.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_APPLIED, false)) return
        if (!prefs.contains(UnifiedSettingsStore.KEY_SOUND_CAR_BLUETOOTH)) {
            prefs.edit().putBoolean(UnifiedSettingsStore.KEY_SOUND_CAR_BLUETOOTH, true).apply()
        }
        prefs.edit().putBoolean(KEY_APPLIED, true).apply()
    }
}
