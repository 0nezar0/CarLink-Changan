package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadUnitServerProbePolicyTest {

    @Test fun onlyNativeAaWithTheHostFlagProbes() {
        assertTrue(HeadUnitServerProbePolicy.probesAlongsideNative(3, true))
        assertFalse(HeadUnitServerProbePolicy.probesAlongsideNative(3, false))
        // Mode 1 is the probe itself; it never needs the ride-along.
        assertFalse(HeadUnitServerProbePolicy.probesAlongsideNative(1, true))
        assertFalse(HeadUnitServerProbePolicy.probesAlongsideNative(2, true))
    }

    @Test fun carAsHotspotSweepsTheAccessPointSubnet() {
        // The car hosts 192.168.43.0/24 on ap0 and has a cellular modem; wlan0 is down (not listed).
        val addresses = listOf("rmnet_data0" to "10.172.4.9", "ap0" to "192.168.43.1")
        assertEquals(listOf("192.168.43"), HeadUnitServerProbePolicy.subnets(addresses))
        // The car is x.x.x.1 itself there: no gateway suspect to dial, the sweep finds the phone.
        assertEquals(emptyList<String>(), HeadUnitServerProbePolicy.gatewaySuspects(addresses))
    }

    @Test fun carOnThePhonesHotspotTriesThePhoneFirst() {
        val addresses = listOf("wlan0" to "10.135.22.74", "p2p-wlan0-0" to "192.168.49.1")
        assertEquals(listOf("10.135.22.1"), HeadUnitServerProbePolicy.gatewaySuspects(addresses))
        assertEquals(listOf("10.135.22", "192.168.49"), HeadUnitServerProbePolicy.subnets(addresses))
    }

    @Test fun bothInterfacesAndDuplicatesAreHandled() {
        val addresses = listOf("wlan0" to "192.168.1.20", "wlan1" to "192.168.43.1", "wlan0" to "192.168.1.21")
        assertEquals(listOf("192.168.1", "192.168.43"), HeadUnitServerProbePolicy.subnets(addresses))
        assertEquals(listOf("192.168.1.1"), HeadUnitServerProbePolicy.gatewaySuspects(addresses))
    }

    @Test fun publicCellularAndTunnelsAreNeverSwept() {
        assertFalse(HeadUnitServerProbePolicy.isPrivate("100.64.1.2"))
        assertFalse(HeadUnitServerProbePolicy.isPrivate("8.8.8.8"))
        assertFalse(HeadUnitServerProbePolicy.isPrivate("172.32.0.1"))
        assertTrue(HeadUnitServerProbePolicy.isPrivate("172.20.10.2")) // iPhone-style hotspot range
        assertFalse(HeadUnitServerProbePolicy.isPrivate("not.an.ip"))
        assertEquals(emptyList<String>(), HeadUnitServerProbePolicy.subnets(listOf("tun0" to "10.8.0.2", "ccmni1" to "10.1.1.1")))
        assertTrue(HeadUnitServerProbePolicy.sweepable("ap0"))
        assertTrue(HeadUnitServerProbePolicy.sweepable("softap0"))
        assertTrue(HeadUnitServerProbePolicy.sweepable(null))
    }

    @Test fun theSweepIsBounded() {
        val many = (1..10).map { "wlan$it" to "192.168.$it.5" }
        assertEquals(HeadUnitServerProbePolicy.MAX_SUBNETS, HeadUnitServerProbePolicy.subnets(many).size)
    }
}
