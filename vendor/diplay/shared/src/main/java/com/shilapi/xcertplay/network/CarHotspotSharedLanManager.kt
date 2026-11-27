package com.shilapi.xcertplay.network

import android.content.Context
import com.shilapi.xcertplay.orchestration.ManualHotspotBand
import com.shilapi.xcertplay.orchestration.ManualHotspotSecurity
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress

/**
 * CarLink: once the car's own hotspot is up, attach to that AP interface with the same
 * [ExistingWifiManager] path used by shared-network (SAME_WIFI): the same host-address
 * listing, the same link-change callback, and the same saved AirPlay receiver identity.
 *
 * SoftAP is not a ConnectivityManager station network, so [ManualHotspotManager] still
 * locates and validates the AP; [ExistingWifiManager] is then started with that interface
 * as `preferredInterface`.
 */
class CarHotspotSharedLanManager(
    context: Context,
    ssid: String,
    passphrase: String,
    band: ManualHotspotBand,
    channel: Int,
    security: ManualHotspotSecurity,
    private val onDiagnostic: (String) -> Unit = {},
    isCancelled: () -> Boolean = { false },
    preferredInterface: String? = null,
    onNetworkChanged: () -> Unit = {},
) : WirelessHotspotManager {
    private val appContext = context.applicationContext
    private val savedSsid = ssid
    private val savedPassphrase = passphrase
    private val manual = ManualHotspotManager(
        context = appContext,
        ssid = ssid,
        passphrase = passphrase,
        band = band,
        channel = channel,
        security = security,
        onDiagnostic = onDiagnostic,
        isCancelled = isCancelled,
        preferredInterface = preferredInterface,
    )
    private val onNetworkChanged = onNetworkChanged
    private var existing: ExistingWifiManager? = null

    @Volatile
    private var closed = false

    override fun start(timeoutMillis: Long): WirelessHotspotInfo {
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
        val started = System.nanoTime()
        val apInfo = try {
            manual.start(timeoutMillis)
        } catch (error: Exception) {
            close()
            throw error
        }
        if (closed) {
            close()
            throw IOException("Car hotspot attachment was cancelled")
        }
        val iface = apInfo.interfaceName?.trim()?.takeIf { it.isNotEmpty() }
            ?: run {
                close()
                throw IOException("Car hotspot interface was not identified")
            }
        val remaining = (timeoutMillis - (System.nanoTime() - started) / 1_000_000L).coerceAtLeast(1L)
        val lanManager = ExistingWifiManager(
            appContext,
            savedSsid,
            savedPassphrase,
            onDiagnostic,
            onNetworkChanged,
            preferredInterface = iface,
        )
        existing = lanManager
        val lanInfo = try {
            lanManager.start(remaining)
        } catch (error: Exception) {
            close()
            throw IOException(
                "Could not attach to the car hotspot as a shared network: " +
                    (error.message ?: error.javaClass.simpleName),
                error,
            )
        }
        val merged = carHotspotSharedLanInfo(apInfo, lanInfo)
        onDiagnostic(
            "CarLink: car hotspot attached as shared LAN iface=${merged.interfaceName} " +
                "host=${merged.hostAddress?.hostAddress} " +
                "base=IPv4 family=auto(CarHotspotIpFamily) " + // CarLink v1.6.6
                "identity=saved addresses=${merged.hostAddresses.size}",
        )
        return merged
    }

    override fun validateReady() {
        manual.validateReady()
        existing?.validateReady()
    }

    override fun onCarPlayConfirmed() {
        manual.onCarPlayConfirmed()
        existing?.onCarPlayConfirmed()
    }

    override fun observeClients(listener: (Int) -> Unit) {
        manual.observeClients(listener)
    }

    override fun connectionDiagnosticSnapshot(): String =
        "carHotspotSharedLan manual=${manual.connectionDiagnosticSnapshot()} " +
            "lan=${existing?.connectionDiagnosticSnapshot() ?: "not_attached"}"

    override fun close() {
        closed = true
        existing?.close()
        existing = null
        manual.close()
    }
}

/**
 * CarLink: the car's own AP (and local-only hotspot, and from v1.7.5 an existing Wi-Fi such as an iPhone's
 * Personal Hotspot) must stay IPv4-only. A link-local
 * IPv6 address on the same interface conflicted with AirPlay/Bonjour on the CS75 Plus.
 */
internal fun isCarHotspotIpv4Only(backend: WirelessHotspotBackend): Boolean =
    backend == WirelessHotspotBackend.MANUAL_HOTSPOT ||
        backend == WirelessHotspotBackend.LOCAL_ONLY_HOTSPOT ||
        backend == WirelessHotspotBackend.EXISTING_WIFI // CarLink v1.7.5: an iPhone's Personal Hotspot too

internal fun ipv4OnlyAddresses(addresses: Iterable<InetAddress>): List<InetAddress> =
    addresses.filterIsInstance<Inet4Address>().filter { address ->
        !address.isLoopbackAddress && !address.isAnyLocalAddress &&
            !address.isLinkLocalAddress && !address.isMulticastAddress
    }.distinct()

/**
 * CarLink: keep the car-hotspot SSID, passphrase, live security and channel, but publish
 * only IPv4 addresses and the saved AirPlay receiver identity (`bssid` left unset so the
 * saved device id is advertised, not the AP MAC).
 */
internal fun carHotspotSharedLanInfo(
    ap: WirelessHotspotInfo,
    lan: WirelessHotspotInfo,
): WirelessHotspotInfo {
    val ipv4Hosts = ipv4OnlyAddresses(
        (lan.hostAddresses.ifEmpty { ap.hostAddresses }) +
            listOfNotNull(lan.hostAddress, ap.hostAddress),
    )
    val host = ipv4OnlyAddresses(listOfNotNull(lan.hostAddress, ap.hostAddress)).firstOrNull()
        ?: ipv4Hosts.firstOrNull()
        ?: throw IOException("Car hotspot did not provide a usable IPv4 host address")
    return WirelessHotspotInfo(
        ssid = ap.ssid,
        passphrase = ap.passphrase,
        security = ap.security,
        channel = if (ap.channel > 0) ap.channel else lan.channel,
        frequencyMHz = ap.frequencyMHz ?: lan.frequencyMHz,
        bssid = null,
        interfaceName = lan.interfaceName ?: ap.interfaceName,
        hostAddress = host,
        hostAddresses = ipv4Hosts,
        bandLabel = ap.bandLabel,
        backend = WirelessHotspotBackend.MANUAL_HOTSPOT,
        accessPointBssid = lan.accessPointBssid ?: macStringToBytes(ap.bssid),
    )
}

internal fun macStringToBytes(text: String?): ByteArray? {
    if (text == null || !text.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) return null
    val bytes = text.split(':').map { it.toInt(16).toByte() }.toByteArray()
    if (bytes.all { it == 0.toByte() } || bytes[0].toInt() and 1 != 0 ||
        text.equals("02:00:00:00:00:00", ignoreCase = true)
    ) {
        return null
    }
    return bytes
}
