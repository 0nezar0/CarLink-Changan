package com.carlink.app.ui.diagnostics

import com.carlink.app.R
import com.shilapi.xcertplay.network.CarHotspotIpFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v1.6.6: Diagnostics shows the car hotspot's automatic IP family decision.
class IpFamilyCardTest {
    private val reasons = listOf(
        CarHotspotIpFamily.REASON_KNOWN_IPV4_UNIT, CarHotspotIpFamily.REASON_REMEMBERED_IPV4,
        CarHotspotIpFamily.REASON_REMEMBERED_DUAL, CarHotspotIpFamily.REASON_NO_IPV4,
        CarHotspotIpFamily.REASON_IPV6_DISABLED, CarHotspotIpFamily.REASON_NO_IPV6,
        CarHotspotIpFamily.REASON_IPV6_NOT_USABLE, CarHotspotIpFamily.REASON_LINK_LOCAL_ONLY,
        CarHotspotIpFamily.REASON_NO_IPV6_ROUTE, CarHotspotIpFamily.REASON_IPV6_USABLE,
        CarHotspotIpFamily.REASON_FALLBACK_NO_TCP, CarHotspotIpFamily.REASON_FALLBACK_PICTURE_STALL,
    )

    @Test fun everyEngineReasonHasItsOwnText() {
        val texts = reasons.map(::ipFamilyReasonRes)
        assertTrue(texts.none { it == R.string.carlink_ipfamily_r_unknown })
        assertEquals(reasons.size, texts.toSet().size)
        assertEquals(R.string.carlink_ipfamily_r_unknown, ipFamilyReasonRes("something_new"))
        assertEquals(R.string.carlink_ipfamily_r_fb_picture, ipFamilyReasonRes(CarHotspotIpFamily.REASON_FALLBACK_PICTURE_STALL))
    }

    @Test fun mapsTheEngineRecord() {
        assertNull(IpFamilyUi.from(null))
        val ui = IpFamilyUi.from(CarHotspotIpFamily.LastDecision(CarHotspotIpFamily.MODE_IPV4_ONLY,
            CarHotspotIpFamily.REASON_LINK_LOCAL_ONLY, ipv6Supported = false, decidedAtMillis = 7L, fellBack = false, remembered = false))!!
        assertFalse(ui.dualStack)
        assertFalse(ui.ipv6Supported)
        assertEquals(7L, ui.decidedAtMillis)
        val dual = IpFamilyUi.from(CarHotspotIpFamily.LastDecision(CarHotspotIpFamily.MODE_DUAL_STACK,
            CarHotspotIpFamily.REASON_IPV6_USABLE, ipv6Supported = true, decidedAtMillis = 8L, fellBack = false, remembered = false))!!
        assertTrue(dual.dualStack)
        assertNotEquals(ui, dual)
    }
}
