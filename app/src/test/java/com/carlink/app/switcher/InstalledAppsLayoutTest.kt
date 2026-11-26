package com.carlink.app.switcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledAppsLayoutTest {
    @Test fun iconsAreLargeEnoughToTapOnACarScreen() {
        assertEquals(104f, InstalledAppsLayout.IconSize.value)
        assertEquals(200f, InstalledAppsLayout.MinCellSize.value)
        assertTrue(InstalledAppsLayout.MinCellSize > InstalledAppsLayout.IconSize)
        // About twice the old 48 dp icons, and inside the 96–112 dp range.
        assertTrue(InstalledAppsLayout.IconSize.value in 96f..112f)
        assertTrue(InstalledAppsLayout.IconSize.value >= 96f)
    }

    @Test fun theGridKeepsAComfortableNumberOfColumnsOnWideLandscapeScreens() {
        // 1280 dp (the installed-apps screenshot qualifier) and a 1920×720-class panel at mdpi.
        assertEquals(6, InstalledAppsLayout.columnsForWidth(1280))
        assertEquals(9, InstalledAppsLayout.columnsForWidth(1920))
        // A 1920 px panel that reports xhdpi is 960 dp wide.
        assertEquals(4, InstalledAppsLayout.columnsForWidth(960))
    }
}
