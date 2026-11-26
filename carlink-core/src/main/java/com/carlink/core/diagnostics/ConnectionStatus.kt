package com.carlink.core.diagnostics

import com.carlink.core.core.Engine
import com.carlink.core.core.LinkState
import com.carlink.core.core.Transport
import com.carlink.core.settings.WirelessPath

/**
 * Live connection, video, audio and Wi-Fi figures for Settings › Diagnostics › Connection status.
 * Any field may be missing: the UI shows "—" / «غير متوفر».
 */
data class ConnectionStatus(
    val link: LinkState,
    val wirelessPath: WirelessPath? = null,
    val video: VideoLinkStatus? = null,
    val audio: AudioLinkStatus? = null,
    val wifi: WifiLinkReading? = null,
    val uptimeMs: Long? = null,
    val reconnectCount: Int? = null,
)

data class VideoLinkStatus(
    val width: Int? = null,
    val height: Int? = null,
    val configuredFps: Int? = null,
    /** "HEVC" or "H.264". */
    val codec: String? = null,
    val decoderName: String? = null,
    val hardwareDecoder: Boolean? = null,
    val incomingFps: Int? = null,
    val droppedFrames: Long? = null,
    val lateFrames: Long? = null,
    val decodeLatencyMs: Int? = null,
    val bitrateKbps: Int? = null,
)

enum class AudioStreamKind { MEDIA, NAVIGATION, VOICE, CALL }

data class AudioLinkStatus(
    val streams: Set<AudioStreamKind> = emptySet(),
    val sampleRateHz: Int? = null,
    val bufferFillMs: Int? = null,
)

data class WifiLinkReading(
    val band: WifiBand? = null,
    val linkSpeedMbps: Int? = null,
    val rssiDbm: Int? = null,
)

enum class WifiBand { GHZ_2_4, GHZ_5 }

/** Totals a decoder or process publishes; [MediaRateMeter] turns them into per-second rates. */
data class MediaLinkCounters(
    val width: Int? = null,
    val height: Int? = null,
    val configuredFps: Int? = null,
    val codec: String? = null,
    val decoderName: String? = null,
    val hardwareDecoder: Boolean? = null,
    val received: Long = 0,
    val rendered: Long = 0,
    val decodeNanos: Long = 0,
    val decodeSamples: Long = 0,
    val dropped: Long = 0,
    val late: Long = 0,
    val bytes: Long = 0,
    val streams: Set<AudioStreamKind> = emptySet(),
    val sampleRateHz: Int? = null,
    val bufferFillMs: Int? = null,
    val atNanos: Long = 0,
)

data class MediaLinkRates(
    val incomingFps: Int? = null,
    val decodeLatencyMs: Int? = null,
    val bitrateKbps: Int? = null,
)

/**
 * Per-interval incoming fps, decode latency and bitrate from successive [MediaLinkCounters].
 * The first sample, and one taken after a long gap, has no rate.
 */
class MediaRateMeter {
    private var last: MediaLinkCounters? = null

    fun update(counters: MediaLinkCounters?): MediaLinkRates? {
        if (counters == null) {
            last = null
            return null
        }
        val previous = last
        last = counters
        if (previous == null || counters.atNanos <= previous.atNanos) return MediaLinkRates()
        val elapsed = counters.atNanos - previous.atNanos
        if (elapsed < MIN_WINDOW_NS || elapsed > MAX_WINDOW_NS) return MediaLinkRates()
        val seconds = elapsed / 1e9
        val incoming = ((counters.received - previous.received) / seconds).toInt().coerceAtLeast(0)
        val samples = counters.decodeSamples - previous.decodeSamples
        val decode = if (samples > 0) {
            ((counters.decodeNanos - previous.decodeNanos) / samples / 1_000_000).toInt().coerceAtLeast(0)
        } else null
        val bits = (counters.bytes - previous.bytes) * 8
        val kbps = if (bits >= 0) (bits / 1000.0 / seconds).toInt().coerceAtLeast(0) else null
        return MediaLinkRates(
            incomingFps = incoming,
            decodeLatencyMs = decode,
            bitrateKbps = kbps,
        )
    }

    fun reset() { last = null }

    companion object {
        const val MIN_WINDOW_NS = 200_000_000L
        const val MAX_WINDOW_NS = 5_000_000_000L
    }
}

object ConnectionStatusMapping {
    fun codecLabel(raw: String?): String? = when (raw?.uppercase()) {
        "H265", "HEVC", "H.265" -> "HEVC"
        "H264", "AVC", "H.264" -> "H.264"
        null, "" -> null
        else -> raw
    }

    fun hardwareDecoder(name: String?): Boolean? {
        if (name.isNullOrBlank()) return null
        val n = name.lowercase()
        return !(n.startsWith("omx.google.") || n.startsWith("c2.android.") || n.startsWith("omx.ffmpeg.") ||
            n.contains(".sw.") || n.contains("software") || n.contains("ffmpeg"))
    }

    fun audioKind(type: String?): AudioStreamKind? = when (type?.lowercase()) {
        "media", "music", "compatibility" -> AudioStreamKind.MEDIA
        "alert", "navigation", "guidance", "tts" -> AudioStreamKind.NAVIGATION
        "speechrecognition", "speech", "siri", "voice", "assistant" -> AudioStreamKind.VOICE
        "telephony", "call", "voice_call", "ringtone" -> AudioStreamKind.CALL
        else -> null
    }

    fun bandOfFrequencyMhz(frequencyMhz: Int): WifiBand? = when (frequencyMhz) {
        in 2400..2500 -> WifiBand.GHZ_2_4
        in 4900..5925 -> WifiBand.GHZ_5
        else -> null
    }

    fun video(
        counters: MediaLinkCounters?,
        rates: MediaLinkRates?,
    ): VideoLinkStatus? {
        if (counters == null) return null
        return VideoLinkStatus(
            width = counters.width?.takeIf { it > 0 },
            height = counters.height?.takeIf { it > 0 },
            configuredFps = counters.configuredFps?.takeIf { it > 0 },
            codec = codecLabel(counters.codec),
            decoderName = counters.decoderName?.takeIf { it.isNotBlank() },
            hardwareDecoder = counters.hardwareDecoder ?: hardwareDecoder(counters.decoderName),
            incomingFps = rates?.incomingFps,
            droppedFrames = counters.dropped.takeIf { it >= 0 },
            lateFrames = counters.late.takeIf { it >= 0 },
            decodeLatencyMs = rates?.decodeLatencyMs,
            bitrateKbps = rates?.bitrateKbps,
        )
    }

    fun audio(counters: MediaLinkCounters?): AudioLinkStatus? {
        if (counters == null) return null
        if (counters.streams.isEmpty() && counters.sampleRateHz == null && counters.bufferFillMs == null) return null
        return AudioLinkStatus(
            streams = counters.streams,
            sampleRateHz = counters.sampleRateHz?.takeIf { it > 0 },
            bufferFillMs = counters.bufferFillMs?.takeIf { it >= 0 },
        )
    }

    fun wifi(frequencyMhz: Int?, linkSpeedMbps: Int?, rssiDbm: Int?): WifiLinkReading? {
        if (frequencyMhz == null && linkSpeedMbps == null && rssiDbm == null) return null
        return WifiLinkReading(
            band = frequencyMhz?.let { bandOfFrequencyMhz(it) },
            linkSpeedMbps = linkSpeedMbps?.takeIf { it > 0 },
            rssiDbm = rssiDbm?.takeIf { it != 0 && it > -127 },
        )
    }

    fun pathOf(engine: Engine?, transport: Transport?, path: WirelessPath?): WirelessPath? =
        if (transport == Transport.WIRELESS) path else null
}
