package com.carlink.core.settings

import android.app.Application
import android.content.Context
import com.carlink.core.connection.CarPlaySettingsPolicy
import com.carlink.core.connection.PicturePolicy
import com.shilapi.xcertplay.AirPlayPersistence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Automatic picture's HEVC write must never undo what the driver chose; a safety "off" still goes through. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class HevcChoiceTest {
    private val ctx get() = RuntimeEnvironment.getApplication()

    @Before fun clear() {
        listOf("xcertplay_airplay", "carlink_hevc", CarBrandStore.PREFS, "carlink_unified").forEach {
            ctx.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    /** One session start under Automatic picture where the policy says [policyHevc]. */
    private fun sessionStart(policyHevc: Boolean) {
        val picture = PicturePolicy.ENGINE_DEFAULTS.copy(carPlayHevc = policyHevc)
        CarPlaySettingsPolicy.apply(ctx, CarPlaySettingsPolicy.map(
            UnifiedSettings(picture = Picture.AUTOMATIC), null, VehicleProfile.GENERIC, true, picture = picture))
    }

    private val hevc get() = AirPlayPersistence.loadHevcEnabled(ctx)

    @Test fun noChoiceMeansTheDriverChoiceIsNull() {
        assertNull(HevcChoice.userChoice(ctx))
    }

    @Test fun withNoChoiceThePolicyDecides() {
        sessionStart(policyHevc = true); assertTrue(hevc)
        sessionStart(policyHevc = false); assertFalse(hevc)
        sessionStart(policyHevc = true); assertTrue(hevc)
    }

    @Test fun aManualOffIsNeverTurnedBackOnByTheOnPolicy() {
        HevcChoice.setByUser(ctx, false)
        assertEquals(false, HevcChoice.userChoice(ctx))
        repeat(3) { sessionStart(policyHevc = true); assertFalse(hevc) }
    }

    @Test fun aManualOffSurvivesOnAnyBrand() {
        CarBrandStore(ctx).set(CarBrand.OTHER)
        HevcChoice.setByUser(ctx, false)
        sessionStart(policyHevc = true)
        assertFalse(hevc)
        CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS)
        sessionStart(policyHevc = true)
        assertFalse(hevc)
    }

    @Test fun aManualOnStaysOnWhenThePolicyAgrees() {
        HevcChoice.setByUser(ctx, true)
        assertEquals(true, HevcChoice.userChoice(ctx))
        sessionStart(policyHevc = true); assertTrue(hevc)
    }

    @Test fun theSafetyOffStillWinsOverAManualOn() {
        HevcChoice.setByUser(ctx, true)
        sessionStart(policyHevc = false) // no hardware decoder, a failed attempt, or the Spreadtrum quirk
        assertFalse(hevc)
        assertEquals(true, HevcChoice.userChoice(ctx)) // the driver's choice is kept
        sessionStart(policyHevc = true) // the policy allows it again: back to what the driver chose
        assertTrue(hevc)
    }

    @Test fun theLastManualChoiceWins() {
        HevcChoice.setByUser(ctx, false)
        HevcChoice.setByUser(ctx, true)
        sessionStart(policyHevc = true); assertTrue(hevc)
    }

    @Test fun theSettingsToggleRecordsTheChoice() {
        val toggle = AdvancedSettings.all.first { it.id == "cp_hevc" }.control as Control.Toggle
        val access = EngineAccess(ctx)
        toggle.write(access, false)
        assertEquals(false, HevcChoice.userChoice(ctx))
        sessionStart(policyHevc = true); assertFalse(hevc)
    }

    // ---- Changan CS75 Plus: HEVC is on unless the driver chose Off ----

    private fun cs75() { CarBrandStore(ctx).set(CarBrand.CHANGAN_CS75_PLUS) }

    @Test fun cs75HevcIsOnByDefault() {
        cs75()
        assertTrue(hevc)
        assertEquals(true, Cs75ProjectionDefaults.hevcOverride(ctx))
        assertNull(HevcChoice.userChoice(ctx)) // a default, not a driver choice
    }

    @Test fun cs75IgnoresThePolicysOff() {
        cs75()
        // No hardware decoder found, the Spreadtrum quirk, or a failed attempt: all arrive as policy hevc = false.
        repeat(3) { sessionStart(policyHevc = false); assertTrue(hevc) }
        sessionStart(policyHevc = true); assertTrue(hevc)
    }

    @Test fun cs75StoredOffFromAnEarlierPolicyWriteIsRepaired() {
        AirPlayPersistence.saveHevcEnabled(ctx, false) // an earlier build's policy wrote it; the driver never chose it
        cs75()
        assertTrue(hevc)
    }

    @Test fun cs75PresetPictureAlsoKeepsItOn() {
        cs75()
        AirPlayPersistence.saveHevcEnabled(ctx, false) // something wrote Off behind the app's back
        // Preset pictures send no HEVC value at all; the override still applies.
        CarPlaySettingsPolicy.apply(ctx, CarPlaySettingsPolicy.map(
            UnifiedSettings(picture = Picture.BEST), null, VehicleProfile.GENERIC, true))
        assertTrue(hevc)
    }

    @Test fun cs75ManualOffIsTheOnlyThingThatTurnsItOff() {
        cs75()
        HevcChoice.setByUser(ctx, false)
        assertEquals(false, Cs75ProjectionDefaults.hevcOverride(ctx))
        repeat(3) { sessionStart(policyHevc = true); assertFalse(hevc) }
        CarBrandStore(ctx).chosen() // reading the brand again does not repair a driver's Off
        assertFalse(hevc)
        HevcChoice.setByUser(ctx, true)
        sessionStart(policyHevc = false); assertTrue(hevc)
    }

    @Test fun cs75IsExemptFromTheFailedAttemptMemory() {
        val memory = com.carlink.core.connection.CarPlayHevcMemory(ctx)
        val wantsHevc = PicturePolicy.ENGINE_DEFAULTS.copy(carPlayHevc = true)
        assertEquals(true, memory.apply(wantsHevc, exempt = true).carPlayHevc)
        assertFalse(memory.onNoPicture()) // nothing is suspected, so nothing is remembered as failed
        memory.onPicture()
        assertFalse(memory.failed)
        // Other brands keep the fallback.
        assertEquals(true, memory.apply(wantsHevc).carPlayHevc)
        assertTrue(memory.onNoPicture())
        assertEquals(false, memory.apply(wantsHevc).carPlayHevc)
    }

    @Test fun otherBrandsKeepTheSafetyOffAndTheStoredOff() {
        CarBrandStore(ctx).set(CarBrand.OTHER)
        assertNull(Cs75ProjectionDefaults.hevcOverride(ctx))
        sessionStart(policyHevc = false); assertFalse(hevc)
        sessionStart(policyHevc = true); assertTrue(hevc)
    }

    @Test fun pureRule() {
        assertTrue(HevcChoice.blocksPolicyWrite(false, true))
        assertFalse(HevcChoice.blocksPolicyWrite(false, false))
        assertFalse(HevcChoice.blocksPolicyWrite(true, true))
        assertFalse(HevcChoice.blocksPolicyWrite(true, false))
        assertFalse(HevcChoice.blocksPolicyWrite(null, true))
    }
}
