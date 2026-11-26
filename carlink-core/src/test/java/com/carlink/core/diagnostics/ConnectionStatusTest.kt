package com.carlink.core.diagnostics

import com.carlink.core.core.Engine
import com.carlink.core.core.Transport
import com.carlink.core.settings.WirelessPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ConnectionStatusTest {
    @Test fun codecLabels() {
        assertEquals("HEVC", ConnectionStatusMapping.codecLabel("H265"))
        assertEquals("HEVC", ConnectionStatusMapping.codecLabel("hevc"))
        assertEquals("H.264", ConnectionStatusMapping.codecLabel("H264"))
        assertEquals("H.264", ConnectionStatusMapping.codecLabel("avc"))
        assertNull(ConnectionStatusMapping.codecLabel(null))
        assertNull(ConnectionStatusMapping.codecLabel(""))
    }

    @Test fun hardwareDecoderFromName() {
        assertEquals(true, ConnectionStatusMapping.hardwareDecoder("c2.qti.avc.decoder"))
        assertEquals(true, ConnectionStatusMapping.hardwareDecoder("OMX.qcom.video.decoder.avc"))
        assertEquals(false, ConnectionStatusMapping.hardwareDecoder("OMX.google.h264.decoder"))
        assertEquals(false, ConnectionStatusMapping.hardwareDecoder("c2.android.avc.decoder"))
        assertEquals(false, ConnectionStatusMapping.hardwareDecoder("OMX.ffmpeg.h265dec"))
        assertNull(ConnectionStatusMapping.hardwareDecoder(null))
    }

    @Test fun audioKinds() {
        assertEquals(AudioStreamKind.MEDIA, ConnectionStatusMapping.audioKind("media"))
        assertEquals(AudioStreamKind.NAVIGATION, ConnectionStatusMapping.audioKind("alert"))
        assertEquals(AudioStreamKind.VOICE, ConnectionStatusMapping.audioKind("speechRecognition"))
        assertEquals(AudioStreamKind.CALL, ConnectionStatusMapping.audioKind("telephony"))
        assertNull(ConnectionStatusMapping.audioKind("unknown"))
    }

    @Test fun wifiBands() {
        assertEquals(WifiBand.GHZ_2_4, ConnectionStatusMapping.bandOfFrequencyMhz(2412))
        assertEquals(WifiBand.GHZ_5, ConnectionStatusMapping.bandOfFrequencyMhz(5745))
        assertNull(ConnectionStatusMapping.bandOfFrequencyMhz(0))
        assertNull(ConnectionStatusMapping.bandOfFrequencyMhz(6000))
        val reading = ConnectionStatusMapping.wifi(5745, 433, -55)
        assertEquals(WifiBand.GHZ_5, reading?.band)
        assertEquals(433, reading?.linkSpeedMbps)
        assertEquals(-55, reading?.rssiDbm)
        assertNull(ConnectionStatusMapping.wifi(null, null, null))
    }

    @Test fun wirelessPathOnlyWhenWireless() {
        assertEquals(WirelessPath.CAR_HOTSPOT, ConnectionStatusMapping.pathOf(Engine.CARPLAY, Transport.WIRELESS, WirelessPath.CAR_HOTSPOT))
        assertNull(ConnectionStatusMapping.pathOf(Engine.CARPLAY, Transport.USB, WirelessPath.CAR_HOTSPOT))
        assertNull(ConnectionStatusMapping.pathOf(Engine.CARPLAY, Transport.WIRELESS, null))
    }

    @Test fun rateMeterNeedsTwoSamples() {
        val meter = MediaRateMeter()
        val first = MediaLinkCounters(received = 0, bytes = 0, decodeSamples = 0, decodeNanos = 0, atNanos = 1_000_000_000L)
        assertEquals(MediaLinkRates(), meter.update(first))
        val second = MediaLinkCounters(
            received = 60,
            bytes = 1_500_000,
            decodeSamples = 60,
            decodeNanos = 60 * 8_000_000L,
            atNanos = 2_000_000_000L,
        )
        val rates = meter.update(second)!!
        assertEquals(60, rates.incomingFps)
        assertEquals(8, rates.decodeLatencyMs)
        assertEquals(12_000, rates.bitrateKbps)
    }

    @Test fun rateMeterIgnoresALongGap() {
        val meter = MediaRateMeter()
        meter.update(MediaLinkCounters(received = 0, atNanos = 1_000_000_000L))
        val rates = meter.update(MediaLinkCounters(received = 600, atNanos = 10_000_000_000L))
        assertEquals(MediaLinkRates(), rates)
    }

    @Test fun aaSnapshotRoundTrip() {
        val dir = createTempDirectory("aa-snap").toFile()
        val file = File(dir, "snap.properties")
        val original = MediaLinkCounters(
            width = 1920, height = 1080, configuredFps = 60, codec = "HEVC",
            decoderName = "c2.qti.hevc.decoder", hardwareDecoder = true,
            received = 120, rendered = 118, decodeNanos = 900_000_000, decodeSamples = 100,
            dropped = 2, late = 1, bytes = 3_000_000,
            streams = setOf(AudioStreamKind.MEDIA, AudioStreamKind.NAVIGATION),
            sampleRateHz = 48000, bufferFillMs = 80, atNanos = 5_000_000_000L,
        )
        AaMediaSnapshotStore.write(file, original)
        val read = AaMediaSnapshotStore.read(file, nowNanos = 5_500_000_000L)
        assertEquals(original, read)
        assertNull(AaMediaSnapshotStore.read(file, nowNanos = 9_000_000_000L))
        dir.deleteRecursively()
    }

    @Test fun videoMappingUsesRatesAndKeepsUnavailableNull() {
        val counters = MediaLinkCounters(width = 1920, height = 1080, codec = "H265", decoderName = "c2.qti.hevc.decoder", dropped = 3, late = 1)
        val video = ConnectionStatusMapping.video(counters, MediaLinkRates(incomingFps = 59, decodeLatencyMs = 7, bitrateKbps = 8000))!!
        assertEquals("HEVC", video.codec)
        assertEquals(true, video.hardwareDecoder)
        assertEquals(59, video.incomingFps)
        assertEquals(3L, video.droppedFrames)
        assertEquals(7, video.decodeLatencyMs)
        assertNull(ConnectionStatusMapping.video(null, null))
        assertFalse(ConnectionStatusMapping.hardwareDecoder("c2.android.avc.decoder")!!)
    }
}
