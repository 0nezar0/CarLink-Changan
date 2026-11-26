package com.shilapi.xcertplay.airplay

import java.net.InetAddress

/**
 * CarLink: address for a secondary AirPlay port (screen, timing, events, RTP, keep-alive).
 *
 * The RTSP control listener already binds each family explicitly
 * ([com.shilapi.xcertplay.network.AirPlayPortSelector] — "specific per-family listeners
 * avoid relying on a platform's IPV6_V6ONLY default"). A later `::` wildcard is IPv6-only
 * on Android, so an IPv4 car-hotspot session can finish SETUP and then stall on Picture:
 * the iPhone opens the screen/timing port on the same IPv4 address, and the accept never
 * comes. Android Auto on the same hotspot is unaffected because it does not use these
 * ports. Bind the control session's own local address instead. Null keeps the historical
 * `::` bind for tests.
 */
internal fun airPlayMediaBindAddress(local: InetAddress?): InetAddress =
    local ?: InetAddress.getByName("::")
