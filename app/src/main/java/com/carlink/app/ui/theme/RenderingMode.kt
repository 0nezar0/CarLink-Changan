package com.carlink.app.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.carlink.core.connection.DeviceCapabilities
import com.carlink.core.connection.DeviceSignals
import com.carlink.core.settings.LitePicture

/** The driver's choice in Settings › General › Visual effects. */
enum class RenderingMode { AUTO, FULL, LITE }

/**
 * Lite rendering (docs/LOW_END_PERFORMANCE.md): the same layout and colours drawn with solid
 * fills. It drops the Atmosphere's key light, state glow, floor and grain, the dial's bloom and
 * lit core gradient, and the glass and button sheens, and steps the connecting animations at a
 * few frames per second instead of every vsync. Nothing moves or changes size.
 */
val LocalLiteRendering = staticCompositionLocalOf { false }

object RenderingPolicy {
    /**
     * Automatic picks lite on low-end hardware ([DeviceSignals.liteRendering]: a low-RAM build, 3 GB
     * of RAM or less, a 32-bit-only processor, four cores or fewer, a weak GPU by its GL renderer
     * name, or no core above 1.7 GHz), the cheap head units whose GPUs are the weakest part. Panel
     * size is not a signal: a small panel is cheap to fill.
     */
    fun lite(mode: RenderingMode, signals: DeviceSignals, liteModeInEffect: Boolean = false): Boolean = when (mode) {
        RenderingMode.FULL -> false
        RenderingMode.LITE -> true
        // Lite mode (Settings › Display, settings/LiteMode) says the screen struggles: CarLink's own
        // layers over the picture (the floating switcher, the waiting view) go plain too. Full stays Full.
        RenderingMode.AUTO -> signals.liteRendering || liteModeInEffect
    }

    /** What Automatic resolves to on this device. */
    fun autoIsLite(context: Context): Boolean = lite(RenderingMode.AUTO, DeviceCapabilities.signals(context), LitePicture.inEffect(context))

    fun lite(context: Context, mode: RenderingMode): Boolean =
        lite(mode, DeviceCapabilities.signals(context), mode == RenderingMode.AUTO && LitePicture.inEffect(context))
}
