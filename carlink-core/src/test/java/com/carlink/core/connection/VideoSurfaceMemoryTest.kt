package com.carlink.core.connection

import com.carlink.core.settings.LitePicture
import com.carlink.core.settings.Picture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** CarLink turns back the video surfaces it turned on (Lite mode off), and never the driver's. */
class VideoSurfaceMemoryTest {
    private val plain = PicturePolicy.ENGINE_DEFAULTS

    @Test fun liteOffUndoesWhatLiteTurnedOn() {
        val undone = VideoSurfaceMemory.reconcile(plain, Picture.AUTOMATIC, carPlayWritten = true, aaWritten = true)
        assertEquals(false, undone.carPlayDirectVideo)
        assertEquals(false, undone.aaSurfaceView)
        assertEquals("only the surfaces change", plain.copy(carPlayDirectVideo = false, aaSurfaceView = false), undone)
    }

    @Test fun nothingWrittenNothingUndone() {
        val same = VideoSurfaceMemory.reconcile(plain, Picture.AUTOMATIC, carPlayWritten = false, aaWritten = false)
        assertNull(same.carPlayDirectVideo)
        assertNull(same.aaSurfaceView)
    }

    @Test fun aSurfaceStillAskedForStaysOn() {
        val lite = LitePicture.apply(plain)
        assertEquals(lite, VideoSurfaceMemory.reconcile(lite, Picture.AUTOMATIC, carPlayWritten = true, aaWritten = true))
    }

    @Test fun presetsUndoToo_customIsTheDrivers() {
        assertEquals(false, VideoSurfaceMemory.reconcile(plain, Picture.BEST, true, true).aaSurfaceView)
        assertEquals(plain, VideoSurfaceMemory.reconcile(plain, Picture.CUSTOM, true, true))
    }
}
