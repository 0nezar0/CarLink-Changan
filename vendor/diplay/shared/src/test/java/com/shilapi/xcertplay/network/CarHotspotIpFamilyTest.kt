package com.shilapi.xcertplay.network

import com.shilapi.xcertplay.network.CarHotspotIpFamily.Ipv6Candidate
import com.shilapi.xcertplay.network.CarHotspotIpFamily.MODE_DUAL_STACK
import com.shilapi.xcertplay.network.CarHotspotIpFamily.MODE_IPV4_ONLY
import com.shilapi.xcertplay.network.CarHotspotIpFamily.Probe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet6Address
import java.net.InetAddress

// CarLink v1.6.6: automatic IPv4 / IPv4+IPv6 choice for the car hotspot.
class CarHotspotIpFamilyTest {
    private fun v6(text: String) = InetAddress.getByName(text) as Inet6Address
    private fun linkLocal(scope: Int) = Inet6Address.getByAddress(null,
        v6("fe80::1234:56ff:fe78:9abc").address, scope)

    private val global = Ipv6Candidate(v6("2001:db8:1::1"), tentative = false, dadFailed = false, deprecated = false, bindOk = true)
    private val scopedLinkLocal = Ipv6Candidate(linkLocal(12), tentative = false, dadFailed = false, deprecated = false, bindOk = true)

    /** The CS75 Plus 2023 SoftAP: 192.168.43.1 on wlan0 plus only a link-local fe80:: address. */
    private val cs75Probe = Probe(ipv4Present = true, candidates = listOf(scopedLinkLocal), ipv6Disabled = false, ipv6Route = false)
    private val dualProbe = Probe(ipv4Present = true, candidates = listOf(global, scopedLinkLocal), ipv6Disabled = false, ipv6Route = true)

    private class MapStore : CarHotspotIpFamily.Memory.Store {
        val map = mutableMapOf<String, String>()
        override fun get(key: String) = map[key]
        override fun put(key: String, value: String?) { if (value == null) map.remove(key) else map[key] = value }
    }

    // ---- Probe decisions ----

    @Test fun cs75LinkLocalOnlyStaysIpv4OnlyOnTheFirstTry() {
        val decision = CarHotspotIpFamily.decide(cs75Probe, memory = null, knownIpv4Only = false)
        assertEquals(MODE_IPV4_ONLY, decision.mode)
        assertEquals(CarHotspotIpFamily.REASON_LINK_LOCAL_ONLY, decision.reason)
        assertFalse(decision.ipv6Supported)
        assertTrue(decision.ipv6Addresses.isEmpty())
    }

    @Test fun cs75ProfileStaysIpv4OnlyEvenIfTheProbeSawGlobalIpv6() {
        // E.g. Android IPv6 tethering handed wlan0 a global prefix from mobile data.
        val decision = CarHotspotIpFamily.decide(dualProbe, memory = null, knownIpv4Only = true)
        assertEquals(MODE_IPV4_ONLY, decision.mode)
        assertEquals(CarHotspotIpFamily.REASON_KNOWN_IPV4_UNIT, decision.reason)
        assertTrue(decision.ipv6Addresses.isEmpty())
    }

    @Test fun noIpv6AtAllIsIpv4Only() {
        val d = CarHotspotIpFamily.decide(Probe(true, emptyList()), null, false)
        assertEquals(MODE_IPV4_ONLY, d.mode)
        assertEquals(CarHotspotIpFamily.REASON_NO_IPV6, d.reason)
    }

    @Test fun disabledTentativeDeprecatedOrUnboundIpv6IsIpv4Only() {
        assertEquals(CarHotspotIpFamily.REASON_IPV6_DISABLED,
            CarHotspotIpFamily.decide(dualProbe.copy(ipv6Disabled = true), null, false).reason)
        for (bad in listOf(global.copy(tentative = true), global.copy(dadFailed = true),
            global.copy(deprecated = true), global.copy(bindOk = false))) {
            val d = CarHotspotIpFamily.decide(Probe(true, listOf(bad)), null, false)
            assertEquals(MODE_IPV4_ONLY, d.mode)
            assertEquals(CarHotspotIpFamily.REASON_IPV6_NOT_USABLE, d.reason)
        }
    }

    @Test fun unscopedLinkLocalIsNotUsable() {
        val unscoped = scopedLinkLocal.copy(address = linkLocal(0))
        assertEquals(CarHotspotIpFamily.REASON_IPV6_NOT_USABLE,
            CarHotspotIpFamily.decide(Probe(true, listOf(unscoped)), null, false).reason)
    }

    @Test fun missingIpv6RouteIsIpv4Only() {
        assertEquals(CarHotspotIpFamily.REASON_NO_IPV6_ROUTE,
            CarHotspotIpFamily.decide(dualProbe.copy(ipv6Route = false), null, false).reason)
    }

    @Test fun noIpv4StillChoosesTheIpv4Path() {
        assertEquals(MODE_IPV4_ONLY, CarHotspotIpFamily.decide(dualProbe.copy(ipv4Present = false), null, false).mode)
    }

    @Test fun clearlyUsableIpv6GivesDualStackGlobalFirst() {
        val d = CarHotspotIpFamily.decide(dualProbe, null, false)
        assertEquals(MODE_DUAL_STACK, d.mode)
        assertEquals(CarHotspotIpFamily.REASON_IPV6_USABLE, d.reason)
        assertTrue(d.ipv6Supported)
        assertEquals(listOf(global.address, scopedLinkLocal.address), d.ipv6Addresses)
        // An unreadable route table (Android 10+) does not block a usable global address.
        assertEquals(MODE_DUAL_STACK, CarHotspotIpFamily.decide(dualProbe.copy(ipv6Route = null), null, false).mode)
    }

    // ---- Per-unit memory and fallback ----

    @Test fun pictureStallFallbackIsRememberedForThisUnitOnly() {
        val store = MapStore()
        val memory = CarHotspotIpFamily.Memory(store) { 1_000L }
        val unit = CarHotspotIpFamily.unitKey("CS75-AP", "wlan0", "Allwinner/T7/sun8iw17")
        val other = CarHotspotIpFamily.unitKey("Other-AP", "wlan0", "Allwinner/T7/sun8iw17")
        assertEquals(MODE_DUAL_STACK, CarHotspotIpFamily.decide(dualProbe, memory.read(unit), false).mode)

        memory.recordIpv6Failure(unit, CarHotspotIpFamily.REASON_FALLBACK_PICTURE_STALL)
        val next = CarHotspotIpFamily.decide(dualProbe, memory.read(unit), false)
        assertEquals(MODE_IPV4_ONLY, next.mode)
        assertEquals(CarHotspotIpFamily.REASON_REMEMBERED_IPV4, next.reason)
        assertTrue("the probe still reports IPv6 support", next.ipv6Supported)
        assertEquals(CarHotspotIpFamily.REASON_FALLBACK_PICTURE_STALL, memory.read(unit)!!.reason)
        // A different network (SSID) is a new unit: probed afresh.
        assertNull(memory.read(other))
        assertEquals(MODE_DUAL_STACK, CarHotspotIpFamily.decide(dualProbe, memory.read(other), false).mode)
    }

    @Test fun noTcpFallbackIsRemembered() {
        val memory = CarHotspotIpFamily.Memory(MapStore()) { 5L }
        memory.recordIpv6Failure("u", CarHotspotIpFamily.REASON_FALLBACK_NO_TCP)
        assertEquals(MODE_IPV4_ONLY, CarHotspotIpFamily.decide(dualProbe, memory.read("u"), false).mode)
    }

    @Test fun successfulDualStackIsRememberedButReprobedWhenIpv6Disappears() {
        val memory = CarHotspotIpFamily.Memory(MapStore()) { 5L }
        memory.recordSuccess("u", MODE_DUAL_STACK)
        assertEquals(CarHotspotIpFamily.REASON_REMEMBERED_DUAL,
            CarHotspotIpFamily.decide(dualProbe, memory.read("u"), false).reason)
        // Network changed: no usable IPv6 now → IPv4 only, never a stale dual stack.
        val d = CarHotspotIpFamily.decide(cs75Probe, memory.read("u"), false)
        assertEquals(MODE_IPV4_ONLY, d.mode)
        assertEquals(CarHotspotIpFamily.REASON_LINK_LOCAL_ONLY, d.reason)
    }

    @Test fun ipv4OnlyVerdictExpiresOrIsReprobedAfterRepeatedFailures() {
        var now = 0L
        val memory = CarHotspotIpFamily.Memory(MapStore()) { now }
        memory.recordIpv6Failure("u", CarHotspotIpFamily.REASON_FALLBACK_PICTURE_STALL)
        repeat(CarHotspotIpFamily.REPROBE_AFTER_FAILURES - 1) { memory.recordIpv4Failure("u") }
        assertEquals(MODE_IPV4_ONLY, memory.read("u")!!.mode)
        memory.recordIpv4Failure("u")
        assertNull("re-probe after repeated IPv4-only failures", memory.read("u"))

        memory.recordIpv6Failure("u", CarHotspotIpFamily.REASON_FALLBACK_NO_TCP)
        now = CarHotspotIpFamily.REMEMBER_IPV4_ONLY_MILLIS + 1
        assertNull("re-probe after the verdict expires", memory.read("u"))
    }

    @Test fun successOnIpv4ResetsTheFailureCount() {
        val memory = CarHotspotIpFamily.Memory(MapStore()) { 0L }
        memory.recordIpv6Failure("u", CarHotspotIpFamily.REASON_FALLBACK_NO_TCP)
        memory.recordIpv4Failure("u")
        memory.recordSuccess("u", MODE_IPV4_ONLY)
        assertEquals(0, memory.read("u")!!.ipv4Failures)
        assertEquals(MODE_IPV4_ONLY, memory.read("u")!!.mode)
    }

    @Test fun unitKeyIsStableAndDependsOnNetworkAndModel() {
        val a = CarHotspotIpFamily.unitKey("AP", "wlan0", "m")
        assertEquals(a, CarHotspotIpFamily.unitKey("AP", "wlan0", "m"))
        assertNotEquals(a, CarHotspotIpFamily.unitKey("AP2", "wlan0", "m"))
        assertNotEquals(a, CarHotspotIpFamily.unitKey("AP", "ap0", "m"))
        assertNotEquals(a, CarHotspotIpFamily.unitKey("AP", "wlan0", "m2"))
        assertFalse(a.contains("AP"))
    }

    @Test fun lastDecisionRoundTrips() {
        val memory = CarHotspotIpFamily.Memory(MapStore())
        assertNull(memory.last())
        val last = CarHotspotIpFamily.LastDecision(MODE_IPV4_ONLY, CarHotspotIpFamily.REASON_FALLBACK_PICTURE_STALL,
            ipv6Supported = true, decidedAtMillis = 1234L, fellBack = true, remembered = true)
        memory.recordLast(last)
        assertEquals(last, memory.last())
    }

    @Test fun carHotspotMemoryNeverUsesTheSharedWifiFamilyKey() {
        val store = MapStore()
        val memory = CarHotspotIpFamily.Memory(store)
        memory.recordIpv6Failure("u", CarHotspotIpFamily.REASON_FALLBACK_NO_TCP)
        memory.recordSuccess("v", MODE_DUAL_STACK)
        assertTrue(store.map.keys.none { it.contains("hotspot_address_family") })
        // And the shared-Wi-Fi plan still ignores any remembered family on the car hotspot.
        assertEquals(HotspotAddressPolicy.FAMILY_IPV4,
            WirelessAddressFamilyPlan.choose(WirelessHotspotBackend.MANUAL_HOTSPOT, HotspotAddressPolicy.FAMILY_IPV6, 36).preferredFamily)
    }

    // ---- /proc parsing ----

    @Test fun parsesIfInet6FlagsForTheInterface() {
        val text = """
            fe80000000000000123456fffe789abc 0c 40 20 80     wlan0
            20010db8000100000000000000000001 0c 40 00 40     wlan0
            20010db8000200000000000000000001 03 40 00 00     rmnet0
        """.trimIndent()
        val flags = CarHotspotIpFamily.parseIfInet6(text, "wlan0")
        assertEquals(2, flags.size)
        assertEquals(0x40, flags["20010db8000100000000000000000001"])
    }

    @Test fun routeTableNeedsANonLinkLocalRouteOnTheInterface() {
        val linkLocalOnly = """
            fe800000000000000000000000000000 40 00000000000000000000000000000000 00 00000000000000000000000000000000 00000100 00000001 00000000 00000001     wlan0
            ff000000000000000000000000000000 08 00000000000000000000000000000000 00 00000000000000000000000000000000 00000100 00000001 00000000 00000001     wlan0
        """.trimIndent()
        assertFalse(CarHotspotIpFamily.hasGlobalRoute(linkLocalOnly, "wlan0"))
        val global = linkLocalOnly + "\n" +
            "20010db8000100000000000000000000 40 00000000000000000000000000000000 00 00000000000000000000000000000000 00000100 00000001 00000000 00000001     wlan0"
        assertTrue(CarHotspotIpFamily.hasGlobalRoute(global, "wlan0"))
        assertFalse(CarHotspotIpFamily.hasGlobalRoute(global, "ap0"))
    }
}
