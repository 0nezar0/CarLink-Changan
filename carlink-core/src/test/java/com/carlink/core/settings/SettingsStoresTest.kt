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

    @Test fun carHotspotStartIsOnByDefaultAndAnExplicitChoiceIsKept() {
        app.getSharedPreferences("carlink", Context.MODE_PRIVATE).edit().clear().commit()
        assertTrue(CarLinkPreferences(app).autoCarHotspot)
        CarLinkPreferences(app).autoCarHotspot = false
        assertFalse(CarLinkPreferences(app).autoCarHotspot)
        CarLinkPreferences(app).autoCarHotspot = true
        assertTrue(CarLinkPreferences(app).autoCarHotspot)
    }

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
            hideCarStatusBarMode = HideCarStatusBarMode.SELECTED_APPS,
            hideCarStatusBarPackages = setOf("com.example.app"),
            hideCarStatusBar = true,
            hideAppStatusBar = true,
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

    @Test fun leftoverMarginKeysAreDroppedAndHaveNoEffect() {
        val prefs = app.getSharedPreferences("carlink_unified", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("top_margin", true)
            .putStringSet("cars_with_top_margin", setOf("TOYOTA"))
            .putBoolean("changan_margins_adopted", true)
            .putInt("margin_left_px", 8)
            .putInt("margin_top_px", 64)
            .putInt("cp_margin_top_px", 48)
            .putInt("aa_margin_top_px", 48)
            .commit()
        val loaded = UnifiedSettingsStore(app).load()
        assertEquals(UnifiedSettings(), loaded)
        assertTrue(prefs.all.keys.none {
            it.contains("margin") || it == "top_margin" || it == "cars_with_top_margin" || it == "changan_margins_adopted"
        })
    }

    @Test fun savingSettingsAlsoDropsLeftoverMarginKeys() {
        val prefs = app.getSharedPreferences("carlink_unified", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("top_margin", true).putInt("cp_margin_top_px", 48).commit()
        UnifiedSettingsStore(app).save(UnifiedSettings(rightHandDrive = true))
        assertFalse(prefs.contains("top_margin"))
        assertFalse(prefs.contains("cp_margin_top_px"))
        assertTrue(UnifiedSettingsStore(app).load().rightHandDrive)
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


    @Test fun hideCarStatusBarModeDefaultsOff() {
        val loaded = UnifiedSettingsStore(app).load()
        assertEquals(HideCarStatusBarMode.OFF, loaded.hideCarStatusBarMode)
        assertFalse(loaded.hideCarStatusBar)
        UnifiedSettingsStore(app).save(UnifiedSettings(hideCarStatusBarMode = HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER))
        val on = UnifiedSettingsStore(app).load()
        assertEquals(HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER, on.hideCarStatusBarMode)
        assertTrue(on.hideCarStatusBar)
    }

    @Test fun hideAppStatusBarDefaultsOffAndAnExplicitChoiceIsKept() {
        val store = UnifiedSettingsStore(app)
        assertFalse(store.load().hideAppStatusBar)
        store.save(store.load().copy(hideAppStatusBar = true))
        assertTrue(UnifiedSettingsStore(app).load().hideAppStatusBar)
        store.save(store.load().copy(hideAppStatusBar = false))
        assertFalse(UnifiedSettingsStore(app).load().hideAppStatusBar)
        // The Changan broadcast hide is a separate setting and stays untouched.
        assertEquals(HideCarStatusBarMode.OFF, store.load().hideCarStatusBarMode)
    }

}
