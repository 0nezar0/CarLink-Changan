package com.shilapi.xcertplay.orchestration

// CarLink: new file (see docs/VENDOR_CHANGES.md).

/**
 * CarLink v1.6.6: on a dual-stack car-hotspot session, the iPhone's first AirPlay TCP (or RTSP
 * SETUP) is not proof the path works: on an IPv4-only head unit the old stall was SETUP done, then
 * the Picture / NTP / event / RTP ports never connected. Armed at the first TCP, cancelled by the
 * first rendered frame; when it fires, the session is treated as an IPv6 failure.
 *
 * Only armed for dual stack: the IPv4-only path (CS75 Plus 2023) behaves exactly as in v1.6.5.
 */
internal class PictureStallWatchdog(
    private val timeoutMillis: Long,
    private val schedule: (Long, () -> Unit) -> (() -> Unit),
    private val onStall: () -> Unit,
) {
    private var cancelTimer: (() -> Unit)? = null
    private var done = false

    @get:Synchronized val armed: Boolean get() = cancelTimer != null && !done

    /** The first AirPlay TCP arrived; starts the clock once. */
    @Synchronized fun arm() {
        if (done || cancelTimer != null) return
        cancelTimer = schedule(timeoutMillis, ::fire)
    }

    /** The first picture arrived: the path works. */
    @Synchronized fun frameRendered() = finish()

    @Synchronized fun terminate() = finish()

    private fun finish() {
        done = true
        cancelTimer?.invoke()
        cancelTimer = null
    }

    private fun fire() {
        synchronized(this) {
            if (done) return
            done = true
            cancelTimer = null
        }
        onStall()
    }
}
