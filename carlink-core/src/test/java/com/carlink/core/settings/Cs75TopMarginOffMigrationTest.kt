package com.carlink.core.settings

import android.app.Application
import android.content.Context
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
class Cs75TopMarginOffMigrationTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val vehicle get() = ctx.getSharedPreferences(CarBrandStore.PREFS, Context.MODE_PRIVATE)
    private val store get() = UnifiedSettingsStore(ctx)

    @Before fun clear() {
        vehicle.edit().clear().commit()
        ctx.getSharedPreferences("carlink_unified", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun firstLaunchDefaultPackageIsResetOnce() {
        CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS)
        store.save(
            store.load().copy(
                topMargin = true,
                hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER,
            ),
        )
        Cs75TopMarginOffMigration.apply(ctx)
        assertFalse(store.load().topMargin)
        assertTrue(vehicle.getBoolean(Cs75TopMarginOffMigration.KEY_APPLIED, false))
        // A later explicit on-value is not taken back after the one-shot flag.
        store.save(store.load().copy(topMargin = true, hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER))
        Cs75TopMarginOffMigration.apply(ctx)
        assertTrue(store.load().topMargin)
    }

    @Test fun anExplicitMarginsYesWithBarHideOffIsKept() {
        CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS)
        store.save(
            store.load().copy(
                topMargin = true,
                hideCarStatusBarMode = HideCarStatusBarMode.OFF,
            ),
        )
        Cs75TopMarginOffMigration.apply(ctx)
        assertTrue(store.load().topMargin)
        assertTrue(vehicle.getBoolean(Cs75TopMarginOffMigration.KEY_APPLIED, false))
    }

    @Test fun aCustomHideModeWithMarginsOnIsKept() {
        CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS)
        store.save(
            store.load().copy(
                topMargin = true,
                hideCarStatusBarMode = HideCarStatusBarMode.SELECTED_APPS,
            ),
        )
        Cs75TopMarginOffMigration.apply(ctx)
        assertTrue(store.load().topMargin)
    }

    @Test fun otherBrandsAreLeftAlone() {
        CarBrandStore(ctx).set(CarBrand.TOYOTA)
        store.save(
            store.load().copy(
                topMargin = true,
                hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER,
            ),
        )
        Cs75TopMarginOffMigration.apply(ctx)
        assertTrue(store.load().topMargin)
        assertTrue(vehicle.getBoolean(Cs75TopMarginOffMigration.KEY_APPLIED, false))
    }

    @Test fun alreadyOffStaysOff() {
        CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS)
        store.save(store.load().copy(topMargin = false, hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER))
        Cs75TopMarginOffMigration.apply(ctx)
        assertFalse(store.load().topMargin)
    }

    @Test fun cameFromOldDefaultDetectsTheFirstLaunchPackageOnly() {
        assertTrue(
            Cs75TopMarginOffMigration.cameFromOldDefault(
                UnifiedSettings(topMargin = true, hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER),
            ),
        )
        assertFalse(
            Cs75TopMarginOffMigration.cameFromOldDefault(
                UnifiedSettings(topMargin = true, hideCarStatusBarMode = HideCarStatusBarMode.OFF),
            ),
        )
        assertFalse(
            Cs75TopMarginOffMigration.cameFromOldDefault(
                UnifiedSettings(topMargin = false, hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER),
            ),
        )
    }
}
