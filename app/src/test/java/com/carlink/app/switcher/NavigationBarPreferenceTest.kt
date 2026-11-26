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
        assertTrue(NavigationBarPreference.enabled(app)) // nothing saved for BYD: the default is on
        NavigationBarPreference.setEnabled(app, false)
        assertFalse(NavigationBarPreference.enabled(app))
        choose(CarBrand.TOYOTA)
        assertTrue(NavigationBarPreference.enabled(app)) // Toyota keeps its own On
    }

    @Test fun theBarIsOnByDefaultForEveryCarAndForNoCarChosen() {
        assertTrue(NavigationBarPreference.enabled(app)) // no brand chosen
        CarBrand.entries.forEach { brand ->
            choose(brand)
            assertTrue(brand.name, NavigationBarPreference.enabled(app))
        }
    }

    @Test fun anExplicitOffIsKeptAndSurvivesChoosingAnotherCar() {
        NavigationBarPreference.setEnabled(app, false) // no car chosen yet
        assertFalse(NavigationBarPreference.enabled(app))
        choose(CarBrand.CHANGAN_CS75_PLUS)
        assertFalse(NavigationBarPreference.enabled(app)) // not undone by the new default
        NavigationBarPreference.setEnabled(app, true)
        assertTrue(NavigationBarPreference.enabled(app))
        choose(CarBrand.OTHER)
        assertTrue(NavigationBarPreference.enabled(app))
    }

    @Test fun legacyOffStaysOff() {
        choose(CarBrand.TOYOTA)
        NavigationBarPreference.preferences(app).edit().putString("side", "OFF").commit()
        assertFalse(NavigationBarPreference.enabled(app))
    }

    @Test fun defaultRule() {
        assertTrue(NavigationBarPreference.defaultFor(emptyList()))
        assertFalse(NavigationBarPreference.defaultFor(listOf(false, false)))
        assertTrue(NavigationBarPreference.defaultFor(listOf(false, true)))
    }

    @Test fun stripsStayUpWhileProjectionIsInFront() {
        assertTrue(NavigationBarStrips.shouldAttach(enabled = true, projectionInFront = true))
        assertTrue(NavigationBarStrips.shouldAttach(enabled = true, projectionInFront = false))
        assertFalse(NavigationBarStrips.shouldAttach(enabled = false, projectionInFront = true))
        assertFalse(NavigationBarStrips.shouldAttach(enabled = false, projectionInFront = false))
    }
}
