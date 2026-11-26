package com.carlink.core.settings

import com.carlink.core.connection.PictureChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ChanganPictureTest {
    private val sixty = PictureChoice(1, 60, null, 100, 60, true, "probe")

    @Test fun aChanganHoldsCarPlayAt30AndLeavesTheRest() {
        val held = ChanganPicture.apply(CarBrand.CHANGAN_CS75_PLUS, sixty)
        assertEquals(30, held.carPlayFps)
        assertEquals(sixty.copy(carPlayFps = 30, summary = held.summary), held)
        assertEquals(60, held.aaFps)
    }

    @Test fun otherBrandsAndNoBrandKeepTheirFrameRate() {
        assertSame(sixty, ChanganPicture.apply(CarBrand.TOYOTA, sixty))
        assertSame(sixty, ChanganPicture.apply(CarBrand.BYD, sixty))
        assertSame(sixty, ChanganPicture.apply(null, sixty))
    }

    @Test fun thirtyOrLessIsUnchanged() {
        val thirty = sixty.copy(carPlayFps = 30)
        assertSame(thirty, ChanganPicture.apply(CarBrand.CHANGAN_CS75_PLUS, thirty))
    }

    @Test fun liteModeOnTopStaysAt30() {
        assertEquals(30, LitePicture.apply(ChanganPicture.apply(CarBrand.CHANGAN_CS75_PLUS, sixty)).carPlayFps)
    }
}
