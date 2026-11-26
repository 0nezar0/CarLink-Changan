package com.carlink.app.diagnostics

import com.carlink.core.connection.SessionRequest
import com.carlink.core.connection.SessionState
import com.carlink.core.core.Engine
import com.carlink.core.core.LinkStage
import com.carlink.core.core.LinkState
import com.carlink.core.core.Transport
import com.carlink.core.diagnostics.AudioStreamKind
import com.carlink.core.diagnostics.WifiBand
import com.carlink.core.diagnostics.WifiLinkReading
import com.carlink.core.settings.WirelessPath
import com.shilapi.xcertplay.media.LiveMediaSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionStatusCollectorTest {
    private val link = LinkState(
        stage = LinkStage.CONNECTED,
        engine = Engine.CARPLAY,
        transport = Transport.WIRELESS,
        phoneName = "Nezar's iPhone",
    )
    private val session = SessionState.Active(
        SessionRequest(Engine.CARPLAY, Transport.WIRELESS, phoneAddress = null, phoneName = "Nezar's iPhone"),
        WirelessPath.CAR_HOTSPOT, 1, SessionState.Phase.Connected, reconnectCount = 2,
    )

    @Test fun mapsCarPlaySnapshotAndWifi() {
        val first = LiveMediaSnapshot(
            width = 1920, height = 720, configuredFps = 60, codec = "H265",
            decoderName = "c2.qti.hevc.decoder", received = 120, rendered = 118,
            decodeNanos = 120 * 7_000_000L, decodeSamples = 120, dropped = 2, late = 1,
            bytes = 2_000_000, audioTypes = listOf("media", "alert"), sampleRateHz = 48000,
            bufferFillMs = 64,
        )
        val second = first.copy(
            received = 180, rendered = 178,
            decodeNanos = 180 * 7_000_000L, decodeSamples = 180, bytes = 3_250_000,
        )
        val nanos = ArrayDeque(listOf(1_000_000_000L, 2_000_000_000L))
        var snap = first
        val collector = ConnectionStatusCollector(
            clock = { 10_000L },
            nanoTime = { nanos.removeFirst() },
            carPlaySnapshot = { snap },
            wifiReading = { WifiLinkReading(WifiBand.GHZ_5, 433, -52) },
        )
        collector.collect(link, session, connectedSince = 1_000L, now = 10_000L)
        snap = second
        val status = collector.collect(link, session, connectedSince = 1_000L, now = 10_000L)
        assertEquals(WirelessPath.CAR_HOTSPOT, status.wirelessPath)
        assertEquals(2, status.reconnectCount)
        assertEquals(9000L, status.uptimeMs)
        assertEquals("HEVC", status.video?.codec)
        assertEquals(true, status.video?.hardwareDecoder)
        assertEquals(1920, status.video?.width)
        assertEquals(60, status.video?.incomingFps)
        assertEquals(10_000, status.video?.bitrateKbps)
        assertEquals(7, status.video?.decodeLatencyMs)
        assertEquals(setOf(AudioStreamKind.MEDIA, AudioStreamKind.NAVIGATION), status.audio?.streams)
        assertEquals(48000, status.audio?.sampleRateHz)
        assertEquals(64, status.audio?.bufferFillMs)
        assertEquals(WifiBand.GHZ_5, status.wifi?.band)
        assertEquals(433, status.wifi?.linkSpeedMbps)
        assertEquals(-52, status.wifi?.rssiDbm)
    }

    @Test fun usbHidesWifiAndPath() {
        val usb = link.copy(transport = Transport.USB)
        val collector = ConnectionStatusCollector(
            carPlaySnapshot = { null },
            wifiReading = { WifiLinkReading(WifiBand.GHZ_2_4, 65, -70) },
        )
        val usbSession = SessionState.Active(
            SessionRequest(Engine.CARPLAY, Transport.USB, phoneAddress = null, phoneName = null),
            null, 1, SessionState.Phase.Connected,
        )
        val status = collector.collect(usb, usbSession, null)
        assertNull(status.wirelessPath)
        assertNull(status.wifi)
    }

    @Test fun missingFiguresStayNull() {
        val collector = ConnectionStatusCollector(carPlaySnapshot = { null }, wifiReading = { null })
        val status = collector.collect(link, session, null)
        assertNull(status.video)
        assertNull(status.audio)
        assertNull(status.wifi)
        assertEquals(2, status.reconnectCount)
    }
}
