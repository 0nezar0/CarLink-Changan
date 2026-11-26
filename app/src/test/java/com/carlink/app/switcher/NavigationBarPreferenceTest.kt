package com.carlink.app.switcher

import android.app.Application
import android.content.Context
import com.carlink.core.settings.CarBrand
import com.carlink.core.settings.CarBrandStore
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
class NavigationBarPreferenceTest {
    private lateinit var app: Application

    @Before fun clear() {
        app = RuntimeEnvironment.getApplication()
        NavigationBarPreference.preferences(app).edit().clear().commit()
        app.getSharedPreferences(CarBrandStore.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun choose(brand: CarBrand) {
        app.getSharedPreferences(CarBrandStore.PREFS, Context.MODE_PRIVATE).edit()
            .putString(CarBrandStore.KEY_BRAND, brand.name).putBoolean(CarBrandStore.KEY_CS75_MIGRATED, true).commit()
    }

    @Test fun legacyGlobalSwitchMigratesToTheCurrentCar() {
        choose(CarBrand.TOYOTA)
        NavigationBarPreference.preferences(app).edit().putString("side", "LEFT").commit()
        assertTrue(NavigationBarPreference.enabled(app))
        val prefs = NavigationBarPreference.preferences(app)
        assertFalse(prefs.contains("side"))
        assertFalse(prefs.contains("enabled"))
        assertTrue(prefs.getBoolean("enabled_TOYOTA", false))
    }

    @Test fun eachCarKeepsItsOwnSwitchAndAnyBrandCanEnableTheBar() {
        choose(CarBrand.TOYOTA)
        NavigationBarPreference.setEnabled(app, true)
        assertTrue(NavigationBarPreference.enabled(app))
        choose(CarBrand.BYD)
        assertFalse(NavigationBarPreference.enabled(app))
        NavigationBarPreference.setEnabled(app, true)
        assertTrue(NavigationBarPreference.enabled(app))
        choose(CarBrand.TOYOTA)
        assertTrue(NavigationBarPreference.enabled(app))
    }
}
