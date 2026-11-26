package com.carlink.core.settings

import android.app.Application
import android.content.Context
import com.shilapi.xcertplay.AirPlayPersistence
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
class Cs75ProjectionDefaultsTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val air get() = ctx.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)
    private val vehicle get() = ctx.getSharedPreferences(CarBrandStore.PREFS, Context.MODE_PRIVATE)

    @Before fun clear() {
        air.edit().clear().commit()
        vehicle.edit().clear().commit()
        ctx.getSharedPreferences("carlink_unified", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun firstCs75SelectionWritesWidthHevcAndCustomSize() {
        CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS)
        assertEquals(Cs75ProjectionDefaults.CARPLAY_WIDTH_MM, air.getInt("display_width_physical_mm", 0))
        assertTrue(air.getBoolean("hevc_enabled", false))
        assertEquals(InterfaceSize.CUSTOM, UnifiedSettingsStore(ctx).load().interfaceSize)
        assertTrue(vehicle.getBoolean(Cs75ProjectionDefaults.KEY_APPLIED, false))
    }

    @Test fun aLaterExplicitChangeIsNotOverwritten() {
        CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS)
        AirPlayPersistence.saveWidthPhysicalMm(ctx, 250)
        AirPlayPersistence.saveHevcEnabled(ctx, false)
        CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS)
        Cs75ProjectionDefaults.applyIfUnset(ctx)
        assertEquals(250, air.getInt("display_width_physical_mm", 0))
        assertFalse(air.getBoolean("hevc_enabled", true))
    }

    @Test fun customInterfaceKeepsAnExistingWidth() {
        UnifiedSettingsStore(ctx).save(UnifiedSettingsStore(ctx).load().copy(interfaceSize = InterfaceSize.CUSTOM))
        AirPlayPersistence.saveWidthPhysicalMm(ctx, 180)
        Cs75ProjectionDefaults.applyIfUnset(ctx)
        assertEquals(180, air.getInt("display_width_physical_mm", 0))
        assertTrue(air.getBoolean("hevc_enabled", false))
        assertEquals(InterfaceSize.CUSTOM, UnifiedSettingsStore(ctx).load().interfaceSize)
    }

    @Test fun genericChanganDoesNotGetTheDefaults() {
        CarBrandStore(ctx).set(CarBrand.CHANGAN)
        assertFalse(air.contains("display_width_physical_mm"))
        assertFalse(air.contains("hevc_enabled"))
        assertFalse(vehicle.getBoolean(Cs75ProjectionDefaults.KEY_APPLIED, false))
    }

    @Test fun migratingASavedChanganAppliesTheDefaultsOnce() {
        vehicle.edit().putString(CarBrandStore.KEY_BRAND, CarBrand.CHANGAN.name).commit()
        assertEquals(CarBrand.CHANGAN_CS75_PLUS, CarBrandStore(ctx).chosen())
        assertEquals(Cs75ProjectionDefaults.CARPLAY_WIDTH_MM, air.getInt("display_width_physical_mm", 0))
        assertTrue(air.getBoolean("hevc_enabled", false))
    }
}
