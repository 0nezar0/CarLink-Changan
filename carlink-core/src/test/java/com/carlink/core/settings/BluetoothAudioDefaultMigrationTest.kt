package com.carlink.core.settings

import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class BluetoothAudioDefaultMigrationTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val prefs get() = ctx.getSharedPreferences(UnifiedSettingsStore.PREFS, Context.MODE_PRIVATE)

    @Before fun clear() {
        prefs.edit().clear().commit()
    }

    @Test fun missingKeyIsTheOldDefaultAndBecomesBluetoothOnce() {
        assertFalse(prefs.contains(UnifiedSettingsStore.KEY_SOUND_CAR_BLUETOOTH))
        BluetoothAudioDefaultMigration.apply(ctx)
        assertTrue(UnifiedSettingsStore(ctx).load().soundThroughCarBluetooth)
        assertTrue(prefs.getBoolean(UnifiedSettingsStore.KEY_SOUND_CAR_BLUETOOTH, false))
        assertTrue(prefs.getBoolean(BluetoothAudioDefaultMigration.KEY_APPLIED, false))
        // A later explicit Through-CarLink choice is not taken back.
        UnifiedSettingsStore(ctx).save(UnifiedSettingsStore(ctx).load().copy(soundThroughCarBluetooth = false))
        BluetoothAudioDefaultMigration.apply(ctx)
        assertFalse(UnifiedSettingsStore(ctx).load().soundThroughCarBluetooth)
    }

    @Test fun explicitThroughCarLinkIsKept() {
        prefs.edit().putBoolean(UnifiedSettingsStore.KEY_SOUND_CAR_BLUETOOTH, false).commit()
        BluetoothAudioDefaultMigration.apply(ctx)
        assertFalse(UnifiedSettingsStore(ctx).load().soundThroughCarBluetooth)
        assertTrue(prefs.getBoolean(BluetoothAudioDefaultMigration.KEY_APPLIED, false))
    }

    @Test fun explicitBluetoothIsKept() {
        prefs.edit().putBoolean(UnifiedSettingsStore.KEY_SOUND_CAR_BLUETOOTH, true).commit()
        BluetoothAudioDefaultMigration.apply(ctx)
        assertTrue(UnifiedSettingsStore(ctx).load().soundThroughCarBluetooth)
    }

    @Test fun freshLoadWithoutTheKeyDefaultsToBluetooth() {
        assertTrue(UnifiedSettings().soundThroughCarBluetooth)
        assertTrue(UnifiedSettingsStore(ctx).load().soundThroughCarBluetooth)
    }
}
