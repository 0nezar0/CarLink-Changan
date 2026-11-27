package com.shilapi.xcertplay.network

import com.shilapi.xcertplay.transport.Iap2WirelessSecurity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

// CarLink: car hotspot credentials stay, but discovery and AirPlay identity match shared-network.
class CarHotspotSharedLanInfoTest {
    private val apIpv4 = InetAddress.getByName("192.168.43.1")
    private val lanIpv4 = InetAddress.getByName("192.168.43.1")
    private val lanIpv6 = InetAddress.getByName("fe80::1")

    @Test
    fun usesSharedNetworkAddressesAndSavedReceiverIdentity() {
        val ap = WirelessHotspotInfo(
            ssid = "CHANGAN",
            passphrase = "secretpass",
            security = Iap2WirelessSecurity.WPA3_TRANSITION,
            channel = 6,
            frequencyMHz = 2437,
            bssid = "AA:BB:CC:DD:EE:FF",
            interfaceName = "ap0",
            hostAddress = apIpv4,
            bandLabel = "2.4 GHz",
            backend = WirelessHotspotBackend.MANUAL_HOTSPOT,
            hostAddresses = listOf(apIpv4),
        )
        val lan = WirelessHotspotInfo(
            ssid = "CHANGAN",
            passphrase = "secretpass",
            security = Iap2WirelessSecurity.WPA_WPA2,
            channel = 0,
            frequencyMHz = null,
            bssid = null,
            interfaceName = "ap0",
            hostAddress = lanIpv4,
            bandLabel = "Auto",
            backend = WirelessHotspotBackend.EXISTING_WIFI,
            hostAddresses = listOf(lanIpv4, lanIpv6),
        )
        val merged = carHotspotSharedLanInfo(ap, lan)
        assertEquals("CHANGAN", merged.ssid)
        assertEquals("secretpass", merged.passphrase)
        assertEquals(Iap2WirelessSecurity.WPA3_TRANSITION, merged.security)
        assertEquals(6, merged.channel)
        assertEquals(2437, merged.frequencyMHz)
        assertNull(merged.bssid)
        assertEquals("ap0", merged.interfaceName)
        assertEquals(lanIpv4, merged.hostAddress)
        assertEquals(listOf(lanIpv4), merged.hostAddresses)
        assertEquals("2.4 GHz", merged.bandLabel)
        assertEquals(WirelessHotspotBackend.MANUAL_HOTSPOT, merged.backend)
        assertArrayEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()), merged.accessPointBssid)
    }

    @Test
    fun fallsBackToApAddressesWhenTheLanListIsEmpty() {
        val ap = WirelessHotspotInfo(
            ssid = "CHANGAN",
            passphrase = "secretpass",
            security = Iap2WirelessSecurity.WPA_WPA2,
            channel = 0,
            frequencyMHz = null,
            bssid = null,
            interfaceName = "wlan0",
            hostAddress = apIpv4,
            bandLabel = "Auto",
            backend = WirelessHotspotBackend.MANUAL_HOTSPOT,
            hostAddresses = listOf(apIpv4),
        )
        val lan = WirelessHotspotInfo(
            ssid = "CHANGAN",
            passphrase = "secretpass",
            security = Iap2WirelessSecurity.WPA_WPA2,
            channel = 11,
            frequencyMHz = 2462,
            bssid = null,
            interfaceName = null,
            hostAddress = null,
            bandLabel = "Auto",
            backend = WirelessHotspotBackend.EXISTING_WIFI,
        )
        val merged = carHotspotSharedLanInfo(ap, lan)
        assertEquals(apIpv4, merged.hostAddress)
        assertEquals(listOf(apIpv4), merged.hostAddresses)
        assertEquals("wlan0", merged.interfaceName)
        assertEquals(11, merged.channel)
        assertEquals(2462, merged.frequencyMHz)
        assertNull(merged.accessPointBssid)
    }

    @Test
    fun rejectsPlaceholderAndMulticastMacAddresses() {
        assertNull(macStringToBytes(null))
        assertNull(macStringToBytes("02:00:00:00:00:00"))
        assertNull(macStringToBytes("00:00:00:00:00:00"))
        assertNull(macStringToBytes("01:00:00:00:00:01"))
        assertArrayEquals(
            byteArrayOf(0x10, 0x20, 0x30, 0x40, 0x50, 0x60),
            macStringToBytes("10:20:30:40:50:60"),
        )
    }

    @Test
    fun dropsIpv6AddressesFromTheCarHotspotLan() {
        val ap = WirelessHotspotInfo(
            ssid = "CHANGAN",
            passphrase = "secretpass",
            security = Iap2WirelessSecurity.WPA_WPA2,
            channel = 6,
            frequencyMHz = 2437,
            bssid = null,
            interfaceName = "ap0",
            hostAddress = apIpv4,
            bandLabel = "2.4 GHz",
            backend = WirelessHotspotBackend.MANUAL_HOTSPOT,
            hostAddresses = listOf(apIpv4, lanIpv6),
        )
        val lan = WirelessHotspotInfo(
            ssid = "CHANGAN",
            passphrase = "secretpass",
            security = Iap2WirelessSecurity.WPA_WPA2,
            channel = 0,
            frequencyMHz = null,
            bssid = null,
            interfaceName = "ap0",
            hostAddress = lanIpv6,
            bandLabel = "Auto",
            backend = WirelessHotspotBackend.EXISTING_WIFI,
            hostAddresses = listOf(lanIpv6, lanIpv4),
        )
        val merged = carHotspotSharedLanInfo(ap, lan)
        assertEquals(lanIpv4, merged.hostAddress)
        assertEquals(listOf(lanIpv4), merged.hostAddresses)
    }

    @Test
    fun rejectsACarHotspotWithNoIpv4Address() {
        val v6 = WirelessHotspotInfo(
            ssid = "CHANGAN",
            passphrase = "secretpass",
            security = Iap2WirelessSecurity.WPA_WPA2,
            channel = 0,
            frequencyMHz = null,
            bssid = null,
            interfaceName = "ap0",
            hostAddress = lanIpv6,
            bandLabel = "Auto",
            backend = WirelessHotspotBackend.MANUAL_HOTSPOT,
            hostAddresses = listOf(lanIpv6),
        )
        assertThrows(IOException::class.java) { carHotspotSharedLanInfo(v6, v6) }
    }

    @Test
    fun ipv4OnlyHelpersIgnoreLinkLocalAndSharedWifi() {
        assertTrue(isCarHotspotIpv4Only(WirelessHotspotBackend.MANUAL_HOTSPOT))
        assertTrue(isCarHotspotIpv4Only(WirelessHotspotBackend.LOCAL_ONLY_HOTSPOT))
        assertTrue(isCarHotspotIpv4Only(WirelessHotspotBackend.EXISTING_WIFI)) // v1.7.6
        assertFalse(isCarHotspotIpv4Only(WirelessHotspotBackend.WIFI_P2P))
        assertEquals(
            listOf(lanIpv4),
            ipv4OnlyAddresses(listOf(lanIpv6, lanIpv4, InetAddress.getByName("127.0.0.1"))),
        )
    }
}
