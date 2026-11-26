package com.carlink.app.switcher

import com.carlink.core.settings.HideCarStatusBarMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FceSystemBarTest {
    @Test fun hidesOnlyWhenCs75ModeIsOnAndAProjectionIsInFront() {
        assertTrue(FceSystemBar.shouldHide(changanSelected = true, settingEnabled = true, projectionInFront = true))
        assertFalse(FceSystemBar.shouldHide(changanSelected = false, settingEnabled = true, projectionInFront = true))
        assertFalse(FceSystemBar.shouldHide(changanSelected = true, settingEnabled = false, projectionInFront = true))
        assertFalse(FceSystemBar.shouldHide(changanSelected = true, settingEnabled = true, projectionInFront = false))
    }

    @Test fun allExceptLauncherHidesEverywhereButTheCarHomeAndAlwaysHidesCarLink() {
        val own = "com.carlink.app.hudtest"
        fun hide(pkg: String) = FceSystemBar.shouldHideForPackage(
            pkg, HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER, emptySet(), own,
        )
        assertTrue(hide(own))
        assertTrue(hide("com.android.chrome"))
        assertFalse(hide("com.faurecia.app.in"))
        assertFalse(hide("com.incall.apps.in"))
        assertFalse(FceSystemBar.shouldHideForPackage(own, HideCarStatusBarMode.OFF, emptySet(), own))
    }

    @Test fun selectedAppsHidesOnlyTickedPackagesAndAlwaysHidesCarLink() {
        val own = "com.carlink.app.hudtest"
        val selected = setOf("com.example.nav")
        fun hide(pkg: String) = FceSystemBar.shouldHideForPackage(
            pkg, HideCarStatusBarMode.SELECTED_APPS, selected, own,
        )
        assertTrue(hide(own))
        assertTrue(hide("com.example.nav"))
        assertFalse(hide("com.android.chrome"))
        assertFalse(hide("com.faurecia.app.in"))
    }
}
