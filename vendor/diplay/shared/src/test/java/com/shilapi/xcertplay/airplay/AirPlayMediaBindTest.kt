package com.shilapi.xcertplay.airplay

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class AirPlayMediaBindTest {
    @Test fun nullLocalKeepsTheHistoricalIpv6Wildcard() {
        val address = airPlayMediaBindAddress(null)
        assertTrue(address.isAnyLocalAddress)
        assertTrue(address is java.net.Inet6Address)
    }

    @Test fun anIpv4SessionAddressIsKept() {
        val local = InetAddress.getByName("127.0.0.1")
        assertEquals(local, airPlayMediaBindAddress(local))
    }

    @Test fun aScreenListenerBoundToIpv4AcceptsAnIpv4Peer() {
        val key = ByteArray(32)
        val stream = ScreenStream(key, InetAddress.getLoopbackAddress())
        stream.use {
            val port = stream.listen(object : ScreenStream.Listener {}, InetAddress.getLoopbackAddress())
            Socket().use { client ->
                client.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 1_000)
                assertTrue(client.isConnected)
            }
        }
    }

    private val wildcard = InetAddress.getByName("::")
    private val ipv4 = InetAddress.getByName("192.168.43.1")
    private val linkLocal = InetAddress.getByName("fe80::1")

    @Test fun carHotspotBindsTheSessionsIpv4() {
        assertEquals(ipv4, mediaBindAddress(ipv4, ipv4Only = true))
    }

    // CarLink v1.7.4: the head unit joined an iPhone's Personal Hotspot (existing Wi-Fi): an IPv4 session binds its IPv4.
    @Test fun iphonePersonalHotspotSessionBindsItsIpv4() {
        val onIphoneHotspot = InetAddress.getByName("172.20.10.4")
        assertEquals(onIphoneHotspot, mediaBindAddress(onIphoneHotspot, ipv4Only = true))
    }

    @Test fun everyOtherCaseKeepsTheWildcard() {
        assertEquals(wildcard, mediaBindAddress(ipv4, ipv4Only = false))
        assertEquals(wildcard, mediaBindAddress(linkLocal, ipv4Only = true))
        assertEquals(wildcard, mediaBindAddress(null, ipv4Only = true))
    }
}
