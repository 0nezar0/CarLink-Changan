package com.carlink.app.ui

import android.app.Application
import android.content.Context
import com.carlink.app.ui.theme.ThemeMode
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
class DarkThemeDefaultMigrationTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val prefs get() = ctx.getSharedPreferences(UiPreferences.PREFS, Context.MODE_PRIVATE)

    @Before fun clear() {
        prefs.edit().clear().commit()
    }

    @Test fun missingKeyIsTheOldDefaultAndBecomesDarkOnce() {
        assertFalse(prefs.contains(UiPreferences.KEY_THEME))
        DarkThemeDefaultMigration.apply(ctx)
        assertEquals(ThemeMode.DARK, UiPreferences(ctx).themeMode)
        assertEquals(ThemeMode.DARK.name, prefs.getString(UiPreferences.KEY_THEME, null))
        assertTrue(prefs.getBoolean(DarkThemeDefaultMigration.KEY_APPLIED, false))
        // A later explicit Follow-the-car or Light choice is not taken back.
        UiPreferences(ctx).themeMode = ThemeMode.LIGHT
        DarkThemeDefaultMigration.apply(ctx)
        assertEquals(ThemeMode.LIGHT, UiPreferences(ctx).themeMode)
    }

    @Test fun explicitLightIsKept() {
        UiPreferences(ctx).themeMode = ThemeMode.LIGHT
        DarkThemeDefaultMigration.apply(ctx)
        assertEquals(ThemeMode.LIGHT, UiPreferences(ctx).themeMode)
        assertTrue(prefs.getBoolean(DarkThemeDefaultMigration.KEY_APPLIED, false))
    }

    @Test fun explicitFollowTheCarIsKept() {
        UiPreferences(ctx).themeMode = ThemeMode.FOLLOW_SYSTEM
        DarkThemeDefaultMigration.apply(ctx)
        assertEquals(ThemeMode.FOLLOW_SYSTEM, UiPreferences(ctx).themeMode)
    }

    @Test fun alreadyDarkStaysDark() {
        UiPreferences(ctx).themeMode = ThemeMode.DARK
        DarkThemeDefaultMigration.apply(ctx)
        assertEquals(ThemeMode.DARK, UiPreferences(ctx).themeMode)
    }
}
