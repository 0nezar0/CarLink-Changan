package com.andrerinas.openheadunit.aap

/**
 * CarLink: the head unit server probe that rides along with Native AA (mode 3).
 *
 * Android Auto's developer setting "Start head unit server" opens TCP 5277 on the phone. When the
 * car and the phone share an IP network (the car on the phone's hotspot, the phone on the car's
 * hotspot, or one router), the car can dial that server directly, with no Bluetooth handshake at
 * all. CarLink asks for that by writing [SETTING_KEY]; the engine then runs one
 * [com.andrerinas.openheadunit.connection.NetworkDiscovery] sweep, restricted to port 5277, next to
 * the normal Native AA bring-up. Whichever reaches the car first carries the session. The probe
 * hands its connected socket straight to the connection, exactly as mode 1 does, because the
 * phone's server binds to the first connection it accepts and is deaf after a stray one.
 *
 * The sweep goes to the default gateway and each interface's x.x.x.1 first, then every private
 * /24 the car is on — the access-point interface (ap0, wlan1, softap0) included, because when the
 * car is the hotspot the phone is a DHCP client somewhere in that /24 and no route points at it.
 */
object HeadUnitServerProbePolicy {
    /** DiAuto preference CarLink writes per session. */
    const val SETTING_KEY = "carlink-headunit-server-probe"

    /** Subnets swept in one bring-up at most (254 hosts each, 300 ms per probe, 64 in parallel). */
    const val MAX_SUBNETS = 4

    /** The probe runs only in Native AA, and only where the host asked for it. */
    fun probesAlongsideNative(mode: Int, enabled: Boolean): Boolean = mode == 3 && enabled

    /**
     * Interfaces never worth sweeping: cellular modems (a carrier's CGNAT block, not the phone),
     * VPN tunnels and dummies. Wi-Fi client, access point and Wi-Fi Direct group interfaces stay.
     */
    fun sweepable(interfaceName: String?): Boolean {
        val name = interfaceName?.lowercase() ?: return true
        return BLOCKED_PREFIXES.none { name.startsWith(it) }
    }

    /** RFC 1918 only: the phone and car share a private network on every route this covers. */
    fun isPrivate(ip: String): Boolean {
        val p = octets(ip) ?: return false
        return p[0] == 10 || (p[0] == 172 && p[1] in 16..31) || (p[0] == 192 && p[1] == 168)
    }

    /**
     * The /24 prefixes ("192.168.43") to sweep for [addresses] (interface name to IPv4 address),
     * in interface order, de-duplicated, private and sweepable only, at most [MAX_SUBNETS].
     */
    fun subnets(addresses: List<Pair<String?, String>>): List<String> =
        addresses.asSequence()
            .filter { (iface, ip) -> sweepable(iface) && isPrivate(ip) }
            .map { (_, ip) -> ip.substringBeforeLast('.') }
            .distinct()
            .take(MAX_SUBNETS)
            .toList()

    /**
     * Gateway suspects ("x.x.x.1" of each address) the sweep checks first, without the car's own
     * addresses: on its own hotspot the car usually is x.x.x.1.
     */
    fun gatewaySuspects(addresses: List<Pair<String?, String>>): List<String> {
        val own = addresses.map { it.second }.toSet()
        return addresses.asSequence()
            .filter { (iface, ip) -> sweepable(iface) && isPrivate(ip) }
            .map { (_, ip) -> ip.substringBeforeLast('.') + ".1" }
            .filter { it !in own }
            .distinct()
            .toList()
    }

    private fun octets(ip: String): List<Int>? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        val n = parts.map { it.toIntOrNull() ?: return null }
        return n.takeIf { it.all { o -> o in 0..255 } }
    }

    private val BLOCKED_PREFIXES = listOf("rmnet", "ccmni", "pdp", "ppp", "tun", "dummy", "lo", "v4-", "clat", "ims")
}
