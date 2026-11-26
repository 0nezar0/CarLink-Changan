// CarLink: not upstream (see docs/VENDOR_CHANGES.md).
package com.andrerinas.openheadunit.aap

import java.util.UUID

/**
 * Giving the phone's hands-free link back to this unit's own Bluetooth stack after a wake poke.
 *
 * A poke is a raw RFCOMM connection to the phone's hands-free (or headset) gateway record. The phone
 * has a single hands-free slot, so the unit's own hands-free client is dropped to make room
 * (measured: `HfpClientConnectionService` disconnects 4 ms after the poke's `connect()`), and when
 * the poke lets go nothing brings it back. The phone then shows this unit as connected for media
 * only ("no phone"), calls come out of the phone, and only a Bluetooth off/on on the phone repairs
 * it. A CS75 Plus 2023 with a Xiaomi 14T Pro showed exactly that on every Connect.
 *
 * So after a poke that reached the phone, on a unit whose own stack plays the hands-free role (it
 * advertises Hands-Free, so no stand-in record was published), this unit's hands-free client is
 * asked to connect to that phone again, unless it already has. Never during the handshake or a
 * handoff, which must not share the radio with a new connection, and never during a call.
 *
 * Pure and unit-tested; the Bluetooth calls live in `NativeAaHandshakeManager` and `BluetoothHelper`.
 */
object HandsFreeRestorePolicy {

    /** How long after the poke socket closed each reconnect attempt is made. */
    val ATTEMPT_DELAYS_MS: List<Long> = listOf(2_000L, 6_000L, 15_000L)

    /** How long an attempt waits for a handshake or handoff to finish before it is skipped. */
    const val BUSY_WAIT_MS = 30_000L

    /** How often that wait looks again. */
    const val BUSY_POLL_MS = 1_000L

    /**
     * Whether a poke that ended took a hands-free link this unit should get back.
     *
     * [slotTaken]: the poke's `connect()` succeeded. [touched]: the record it connected to.
     * [standingInForHfp]: this app published its own stand-in record because the unit has no
     * Hands-Free of its own; there is then no client to give the link back to. [apple]: an iPhone
     * is never poked, and is CarPlay's.
     */
    fun shouldRestore(slotTaken: Boolean, touched: UUID?, standingInForHfp: Boolean, apple: Boolean): Boolean =
        slotTaken && !standingInForHfp && !apple &&
            (touched == BluetoothWakePolicy.HFP_AG_UUID || touched == BluetoothWakePolicy.HSP_AG_UUID)

    /** This unit's hands-free client's link to the poked phone, as read from the client profile. */
    enum class ClientLink {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,

        /** The hidden client profile would not answer on this ROM. */
        UNREADABLE;

        companion object {
            /** From `BluetoothProfile.getConnectionState`: 2 connected, 1 connecting, 0/3 not; null unreadable. */
            fun of(state: Int?): ClientLink = when (state) {
                null -> UNREADABLE
                2 -> CONNECTED
                1 -> CONNECTING
                else -> DISCONNECTED
            }
        }
    }

    enum class Action {
        /** The link is back: stop. */
        DONE,

        /** It is on its way: look again at the next attempt without asking. */
        WAIT,

        /** Ask this unit's hands-free client to connect to the phone. */
        CONNECT,

        /** Not now (handshake, handoff or a call): try at the next attempt. */
        DEFER,

        /** Attempts are spent, or the client cannot be read: leave it. */
        GIVE_UP,
    }

    /**
     * What attempt [attempt] (0-based) does. An unreadable client is still asked once, because the
     * request is harmless when the link is up and it is the only repair the driver does not have
     * to make by hand; after that the unreadable answer is taken as final.
     */
    fun next(attempt: Int, link: ClientLink, busy: Boolean, callActive: Boolean): Action = when {
        link == ClientLink.CONNECTED -> Action.DONE
        attempt >= ATTEMPT_DELAYS_MS.size -> Action.GIVE_UP
        busy || callActive -> Action.DEFER
        link == ClientLink.CONNECTING -> Action.WAIT
        link == ClientLink.UNREADABLE && attempt > 0 -> Action.GIVE_UP
        else -> Action.CONNECT
    }
}
