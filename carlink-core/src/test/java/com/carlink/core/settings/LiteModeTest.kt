package com.carlink.core.settings

import com.carlink.core.connection.LiteModeMonitor
import com.carlink.core.connection.PictureChoice
import com.carlink.core.connection.PicturePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lite mode: fewer frames and direct video, never a lower resolution, and when Auto engages. */
class LiteModeTest {
    private val sharp = PictureChoice(aaResolutionId = 3, aaFps = 60, aaCodec = "H265", carPlayScalePercent = 100, carPlayFps = 60,
        carPlayHevc = true, summary = "1080p")

    @Test fun liteKeepsResolutionCodecAndScaleAndHalvesTheFrames() {
        val lite = LitePicture.apply(sharp)
        assertEquals(30, lite.aaFps)
        assertEquals(30, lite.carPlayFps)
        assertEquals(true, lite.carPlayDirectVideo)
        assertEquals(true, lite.aaSurfaceView)
        assertEquals(sharp.aaResolutionId, lite.aaResolutionId)
        assertEquals(sharp.aaCodec, lite.aaCodec)
        assertEquals(sharp.carPlayScalePercent, lite.carPlayScalePercent)
        assertEquals(sharp.carPlayHevc, lite.carPlayHevc)
        assertEquals("never raises a lower rate", 24, LitePicture.apply(sharp.copy(aaFps = 24)).aaFps)
        assertFalse("already lite", LitePicture.lowers(lite))
        assertTrue(LitePicture.lowers(PicturePolicy.ENGINE_DEFAULTS))
    }

    @Test fun eachModeSaysWhenItApplies() {
        assertFalse(LitePicture.active(LiteMode.OFF, engaged = true))
        assertTrue(LitePicture.active(LiteMode.ALWAYS, engaged = false))
        assertFalse(LitePicture.active(LiteMode.AUTO, engaged = false))
        assertTrue(LitePicture.active(LiteMode.AUTO, engaged = true))
        assertEquals("optional: off until the driver turns it on", LiteMode.OFF, UnifiedSettings().liteMode)
    }

    @Test fun autoEngagesOnlyUnderAutomaticPictureWithEvidenceAndSomethingToLower() {
        assertTrue(LiteModeMonitor.shouldEngage(LiteMode.AUTO, Picture.AUTOMATIC, false, sharp, true))
        assertFalse("a Picture the driver chose stays", LiteModeMonitor.shouldEngage(LiteMode.AUTO, Picture.BEST, false, sharp, true))
        assertFalse(LiteModeMonitor.shouldEngage(LiteMode.OFF, Picture.AUTOMATIC, false, sharp, true))
        assertFalse(LiteModeMonitor.shouldEngage(LiteMode.AUTO, Picture.AUTOMATIC, true, sharp, true))
        assertFalse(LiteModeMonitor.shouldEngage(LiteMode.AUTO, Picture.AUTOMATIC, false, sharp, false))
        assertFalse(LiteModeMonitor.shouldEngage(LiteMode.AUTO, Picture.AUTOMATIC, false, LitePicture.apply(sharp), true))
    }

    @Test fun carPlayNeedsTwoStallsWithinTheWindow() {
        val t = 1_000_000_000L
        assertFalse(LiteModeMonitor.carPlayStall(t))
        assertFalse("the first one aged out", LiteModeMonitor.carPlayStall(t + LiteModeMonitor.CARPLAY_WINDOW_MILLIS + 1))
        assertTrue(LiteModeMonitor.carPlayStall(t + LiteModeMonitor.CARPLAY_WINDOW_MILLIS + 2))
    }
}
