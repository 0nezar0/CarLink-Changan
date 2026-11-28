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
        CarHotspotIpFamily.REASON_FORCED_IPV4,
    )

    @Test fun everyEngineReasonHasItsOwnText() {
        val texts = reasons.map(::ipFamilyReasonRes)
        assertTrue(texts.none { it == R.string.carlink_ipfamily_r_unknown })
        assertEquals(reasons.size, texts.toSet().size)
        assertEquals(R.string.carlink_ipfamily_r_unknown, ipFamilyReasonRes("something_new"))
        assertEquals(R.string.carlink_ipfamily_r_fb_picture, ipFamilyReasonRes(CarHotspotIpFamily.REASON_FALLBACK_PICTURE_STALL))
    }

    // v1.7.8: IPv4 is forced on every hotspot path, so the card always says so.
    @Test fun alwaysShowsIpv4ForcedWhateverTheEngineRecorded() {
        val none = IpFamilyUi.from(null)
        assertEquals(CarHotspotIpFamily.REASON_FORCED_IPV4, none.reason)
        assertNull(none.decidedAtMillis)
        assertEquals(R.string.carlink_ipfamily_r_forced, ipFamilyReasonRes(none.reason))
        val forced = IpFamilyUi.from(CarHotspotIpFamily.forcedIpv4Decision(7L))
        assertEquals(CarHotspotIpFamily.REASON_FORCED_IPV4, forced.reason)
        assertEquals(7L, forced.decidedAtMillis)
        // A v1.6.6 record (dual stack, remembered, fallback) must not be shown as the current decision.
        val old = IpFamilyUi.from(CarHotspotIpFamily.LastDecision(CarHotspotIpFamily.MODE_DUAL_STACK,
            CarHotspotIpFamily.REASON_IPV6_USABLE, ipv6Supported = true, decidedAtMillis = 8L, fellBack = false, remembered = false))
        assertEquals(CarHotspotIpFamily.REASON_FORCED_IPV4, old.reason)
        assertNull(old.decidedAtMillis)
        assertEquals(old, none)
        assertNotEquals(old, forced)
    }

    @Test fun theForcedDecisionIsIpv4OnlyAndNeverDualStack() {
        val d = CarHotspotIpFamily.forcedIpv4Decision(1L)
        assertEquals(CarHotspotIpFamily.MODE_IPV4_ONLY, d.mode)
        assertFalse(d.ipv6Supported)
        assertFalse(d.fellBack)
        assertFalse(d.remembered)
    }
}
