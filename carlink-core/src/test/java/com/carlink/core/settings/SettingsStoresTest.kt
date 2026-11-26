package com.carlink.core.settings

import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class SettingsStoresTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    @Test fun emptyUnifiedStoreLoadsTheDefaults() {
        assertEquals(UnifiedSettings(), UnifiedSettingsStore(app).load())
    }

    @Test fun unifiedSettingsRoundTripEveryField() {
        val settings = UnifiedSettings(
            connectionMethod = ConnectionMethod.CAR_HOTSPOT,
            carHotspot = WifiDetails("BYD-Car ☀", "carpassword"),
            homeWifi = WifiDetails("Home \"Net\"", ""),
            picture = Picture.LIGHT,
            interfaceSize = InterfaceSize.CUSTOM,
            dayNight = DayNight.SCHEDULE,
            nightStartMinute = 21 * 60 + 15,
            nightEndMinute = 5 * 60 + 45,
            rightHandDrive = true,
            soundThroughCarBluetooth = true,
            sendCarLocation = false,
            bydDirections = false,
            showAdvanced = true,
            labUnlocked = true,
            wheelKeys = mapOf(WheelAction.VOICE to 84, WheelAction.HANG_UP to 3002),
            mediaLevelDb = -3,
            navigationLevelDb = 6,
            callLevelDb = -12,
            callAudioAsMedia = true,
            resumeMusic = true,
            duckMusicForDirections = false,
            micGainDb = 9,
            topMargin = true,
            hideCarStatusBarMode = HideCarStatusBarMode.SELECTED_APPS,
            hideCarStatusBarPackages = setOf("com.example.app"),
            hideCarStatusBar = true,
            appLanguage = "ar",
            androidAutoBack = false,
        )
        UnifiedSettingsStore(app).save(settings)
        // A new instance reads what the old one wrote.
        assertEquals(settings, UnifiedSettingsStore(app).load())
        // Every process reads the language from the copy (the :aa process cannot share these prefs).
        assertEquals("ar", AppLanguage.current(app))
    }

    @Test fun theLanguageCopyFollowsEverySaveAndUnknownTagsFallBackToTheDevice() {
        val store = UnifiedSettingsStore(app)
        store.save(UnifiedSettings(appLanguage = "en"))
        assertEquals("en", AppLanguage.current(app))
        store.save(UnifiedSettings(appLanguage = "xx"))
        assertEquals(AppLanguage.SYSTEM, store.load().appLanguage)
        assertEquals(AppLanguage.SYSTEM, AppLanguage.current(app))
    }

    @Test fun theSharedMarginsOfEarlierVersionsTurnTheTopMarginOn() {
        val prefs = app.getSharedPreferences("carlink_unified", Context.MODE_PRIVATE)
        prefs.edit().putInt("margin_left_px", 8).putInt("margin_top_px", 64).putInt("margin_right_px", 16)
            .putInt("margin_bottom_px", 24).commit()
        val migrated = UnifiedSettingsStore(app).load()
        assertTrue(migrated.topMargin)
        // One switch: 48 px at the top only, the same for CarLink, CarPlay and Android Auto.
        assertEquals(48, migrated.marginTopPx)
        assertEquals(ScreenMargins(top = 48), migrated.screenMargins)
        // The first save writes the switch and drops every old margin key.
        UnifiedSettingsStore(app).save(migrated.copy(topMargin = false))
        assertFalse(UnifiedSettingsStore(app).load().topMargin)
        assertTrue(prefs.all.keys.none { it.contains("margin_") || it == "changan_margins_adopted" })
    }

    @Test fun everyEnumValueRoundTrips() {
        val store = UnifiedSettingsStore(app)
        ConnectionMethod.entries.forEach { store.save(UnifiedSettings(connectionMethod = it)); assertEquals(it, store.load().connectionMethod) }
        Picture.entries.forEach { store.save(UnifiedSettings(picture = it)); assertEquals(it, store.load().picture) }
        InterfaceSize.entries.forEach { store.save(UnifiedSettings(interfaceSize = it)); assertEquals(it, store.load().interfaceSize) }
        DayNight.entries.forEach { store.save(UnifiedSettings(dayNight = it)); assertEquals(it, store.load().dayNight) }
        AudioDelay.entries.forEach { store.save(UnifiedSettings(audioDelay = it)); assertEquals(it, store.load().audioDelay) }
    }

    @Test fun audioDelayFollowsTheMusicBufferAnEarlierVersionSetUntilSaved() {
        com.shilapi.xcertplay.AirPlayPersistence.saveMediaBufferMillis(app, wireless = true, millis = 1000)
        assertEquals(AudioDelay.HIGH, UnifiedSettingsStore(app).load().audioDelay)
        UnifiedSettingsStore(app).save(UnifiedSettings(audioDelay = AudioDelay.LOW))
        assertEquals(AudioDelay.LOW, UnifiedSettingsStore(app).load().audioDelay)
    }

    @Test fun savingDefaultsAgainClearsEarlierValues() {
        val store = UnifiedSettingsStore(app)
        store.save(UnifiedSettings(carHotspot = WifiDetails("Car", "carpassword"), rightHandDrive = true))
        store.save(UnifiedSettings())
        assertEquals(UnifiedSettings(), store.load())
    }

    @Test fun unknownEnumNamesFromAnotherVersionFallBackToDefaults() {
        app.getSharedPreferences("carlink_unified", Context.MODE_PRIVATE).edit()
            .putString("connection_method", "BLUETOOTH_ONLY")
            .putString("picture", "ULTRA")
            .putString("interface_size", "")
            .putString("day_night", "dusk")
            .commit()
        val loaded = UnifiedSettingsStore(app).load()
        assertEquals(ConnectionMethod.AUTOMATIC, loaded.connectionMethod)
        assertEquals(Picture.AUTOMATIC, loaded.picture)
        assertEquals(InterfaceSize.MEDIUM, loaded.interfaceSize)
        assertEquals(DayNight.AUTOMATIC, loaded.dayNight)
    }

    @Test fun carLinkPreferencesDefaults() {
        val prefs = CarLinkPreferences(app)
        assertTrue(prefs.autoConnectUsb)
        assertTrue(prefs.autoConnectBluetooth)
        assertTrue("start with the car is on out of the box", prefs.startOnBoot)
        assertFalse(prefs.autoConnectPaused)
    }

    @Test fun carLinkPreferencesPersistAcrossInstances() {
        CarLinkPreferences(app).apply {
            autoConnectUsb = false
            autoConnectBluetooth = false
            startOnBoot = false
            autoConnectPaused = true
        }
        CarLinkPreferences(app).apply {
            assertFalse(autoConnectUsb)
            assertFalse(autoConnectBluetooth)
            assertFalse(startOnBoot)
            assertTrue(autoConnectPaused)
        }
    }

    @Test fun aFreshInstallStartsWithNoMargins() {
        val loaded = UnifiedSettingsStore(app).load()
        assertFalse(loaded.topMargin)
        assertEquals(0, loaded.marginTopPx)
        assertEquals(ScreenMargins(), loaded.screenMargins)
    }

    @Test fun anUpgradeTurnsTheSwitchOnWhenAnyTopMarginWasOn() {
        val prefs = app.getSharedPreferences("carlink_unified", android.content.Context.MODE_PRIVATE)
        fun load() = UnifiedSettingsStore(app).load().topMargin
        // Explicit zeros everywhere: off.
        prefs.edit().clear().putInt("margin_top_px", 0).putInt("cp_margin_top_px", 0).putInt("cp_margin_left_px", 0)
            .putInt("aa_margin_top_px", 0).commit()
        assertFalse(load())
        // Any one top margin (CarLink, CarPlay or Android Auto) on: on.
        for (key in listOf("margin_top_px", "cp_margin_top_px", "aa_margin_top_px")) {
            prefs.edit().clear().putInt("margin_top_px", 0).putInt("cp_margin_top_px", 0).putInt("aa_margin_top_px", 0)
                .putInt(key, 64).commit()
            assertTrue(key, load())
        }
        // Only a side or bottom margin: those are gone, so off.
        prefs.edit().clear().putInt("cp_margin_top_px", 0).putInt("cp_margin_left_px", 40).putInt("aa_margin_bottom_px", 24).commit()
        assertFalse(load())

        // A previous build adopted 48 px and deleted the keys. That install still loads on.
        prefs.edit().clear().putBoolean("changan_margins_adopted", true).commit()
        assertTrue(load())
        // A zero written after that adoption is the driver's choice and stays.
        prefs.edit().putInt("margin_top_px", 0).commit()
        assertFalse(load())

        // Once saved, the switch wins over any old key (a restored old backup included).
        prefs.edit().clear().putBoolean("top_margin", false).putInt("cp_margin_top_px", 48).commit()
        assertFalse(load())
        prefs.edit().clear().putBoolean("top_margin", true).commit()
        assertTrue(load())
    }

    @Test fun hideCarStatusBarModeDefaultsOff() {
        val loaded = UnifiedSettingsStore(app).load()
        assertEquals(HideCarStatusBarMode.OFF, loaded.hideCarStatusBarMode)
        assertFalse(loaded.hideCarStatusBar)
        UnifiedSettingsStore(app).save(UnifiedSettings(hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER))
        val on = UnifiedSettingsStore(app).load()
        assertEquals(HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER, on.hideCarStatusBarMode)
        assertTrue(on.hideCarStatusBar)
    }

    @Test fun marginsApplyOnlyWhileTheCarIsAChangan() {
        val saved = UnifiedSettings(topMargin = true)
        assertEquals(saved, saved.marginsFor(CarBrand.CHANGAN_CS75_PLUS))
        assertEquals(ScreenMargins(top = 48), saved.marginsFor(CarBrand.CHANGAN_CS75_PLUS).screenMargins)
        val cleared = saved.marginsFor(CarBrand.TOYOTA)
        assertEquals(0, cleared.marginTopPx)
        assertEquals(ScreenMargins(), cleared.screenMargins)
        assertEquals(cleared, saved.marginsFor(null))
        assertEquals(cleared, saved.marginsFor(CarBrand.BYD))
        // Choosing another brand does not wipe what a later switch back to Changan uses.
        assertTrue(saved.topMargin)
        assertEquals(48, saved.marginTopPx)
    }

    @Test fun otherCarMarginIsOptInAndSurvivesStorageAndBrandChanges() {
        val store = UnifiedSettingsStore(app)
        val saved = UnifiedSettings(topMargin = true).withMarginFor(CarBrand.TOYOTA, true)
        store.save(saved)
        val loaded = store.load()
        assertEquals(48, loaded.marginsFor(CarBrand.TOYOTA).marginTopPx)
        assertEquals(0, loaded.marginsFor(CarBrand.BYD).marginTopPx)
        val disabled = loaded.withMarginFor(CarBrand.TOYOTA, false)
        assertEquals(0, disabled.marginsFor(CarBrand.TOYOTA).marginTopPx)
        assertEquals(48, disabled.marginsFor(CarBrand.CHANGAN_CS75_PLUS).marginTopPx)
    }

    @Test fun marginsDoNotApplyForGenericChangan() {
        val saved = UnifiedSettings(topMargin = true)
        assertEquals(ScreenMargins(), saved.marginsFor(CarBrand.CHANGAN).screenMargins)
    }

}
