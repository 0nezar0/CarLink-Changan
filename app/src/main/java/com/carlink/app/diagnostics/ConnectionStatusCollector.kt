package com.carlink.app.diagnostics

import android.content.Context
import android.net.wifi.WifiManager
import com.carlink.core.connection.SessionState
import com.carlink.core.core.Engine
import com.carlink.core.core.LinkState
import com.carlink.core.core.Transport
import com.carlink.core.diagnostics.AaMediaSnapshotStore
import com.carlink.core.diagnostics.ConnectionStatus
import com.carlink.core.diagnostics.ConnectionStatusMapping
import com.carlink.core.diagnostics.MediaLinkCounters
import com.carlink.core.diagnostics.MediaRateMeter
import com.carlink.core.diagnostics.WifiLinkReading
import com.shilapi.xcertplay.CarPlayHostIntegration
import com.shilapi.xcertplay.media.LiveMediaSnapshot

/** Reads already-published session, decoder and Wi-Fi figures into a [ConnectionStatus]. */
class ConnectionStatusCollector(
    private val clock: () -> Long = { 0L },
    private val nanoTime: () -> Long = System::nanoTime,
    private val carPlaySnapshot: () -> LiveMediaSnapshot? = { CarPlayHostIntegration.liveMediaSnapshot() },
    private val aaSnapshot: () -> MediaLinkCounters? = { null },
    private val wifiReading: () -> WifiLinkReading? = { null },
) {
    constructor(context: Context) : this(
        clock = android.os.SystemClock::elapsedRealtime,
        carPlaySnapshot = { CarPlayHostIntegration.liveMediaSnapshot() },
        aaSnapshot = { AaMediaSnapshotStore.read(context) },
        wifiReading = { readWifi(context) },
    )

    private val rates = MediaRateMeter()
    private var lastEngine: Engine? = null

    fun collect(
        link: LinkState,
        session: SessionState?,
        connectedSince: Long?,
        now: Long = clock(),
    ): ConnectionStatus {
        val active = session as? SessionState.Active
        val engine = link.engine
        if (engine != lastEngine) {
            rates.reset()
            lastEngine = engine
        }
        val counters = when (engine) {
            Engine.CARPLAY -> carPlaySnapshot()?.toCounters(nanoTime())
            Engine.ANDROID_AUTO -> aaSnapshot()
            null -> null
        }
        val videoRates = rates.update(counters)
        val wifi = if (link.transport == Transport.WIRELESS) wifiReading() else null
        val uptime = connectedSince?.let { (now - it).coerceAtLeast(0) }
        return ConnectionStatus(
            link = link,
            wirelessPath = ConnectionStatusMapping.pathOf(engine, link.transport, active?.path),
            video = ConnectionStatusMapping.video(counters, videoRates),
            audio = ConnectionStatusMapping.audio(counters),
            wifi = wifi,
            uptimeMs = uptime,
            reconnectCount = active?.reconnectCount,
        )
    }

    companion object {
        @Suppress("DEPRECATION")
        fun readWifi(context: Context): WifiLinkReading? {
            val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null
            val info = runCatching { manager.connectionInfo }.getOrNull() ?: return null
            return ConnectionStatusMapping.wifi(
                frequencyMhz = info.frequency.takeIf { it > 0 },
                linkSpeedMbps = info.linkSpeed.takeIf { it > 0 },
                rssiDbm = info.rssi,
            )
        }
    }
}

internal fun LiveMediaSnapshot.toCounters(atNanos: Long): MediaLinkCounters = MediaLinkCounters(
    width = width,
    height = height,
    configuredFps = configuredFps,
    codec = codec,
    decoderName = decoderName,
    hardwareDecoder = ConnectionStatusMapping.hardwareDecoder(decoderName),
    received = received,
    rendered = rendered,
    decodeNanos = decodeNanos,
    decodeSamples = decodeSamples,
    dropped = dropped,
    late = late,
    bytes = bytes,
    streams = audioTypes.mapNotNull { ConnectionStatusMapping.audioKind(it) }.toSet(),
    sampleRateHz = sampleRateHz,
    bufferFillMs = bufferFillMs,
    atNanos = atNanos,
)
