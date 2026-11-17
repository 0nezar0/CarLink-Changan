package com.carlink.core.settings

import android.content.Context
import com.carlink.core.connection.PictureChoice

/**
 * Lite mode for a weak head-unit CPU (Allwinner T7 class), Settings › Display › Lite mode.
 *
 * It never trades sharpness: the stream's resolution, codec and bitrate stay as Automatic picked
 * them, so the picture is not scaled up or compressed harder. It only takes work off the decoder
 * and the GPU:
 * - both engines at 30 frames a second instead of 60;
 * - CarPlay through Direct video output (a SurfaceView: no extra GPU pass per frame);
 * - Android Auto on a SurfaceView, for the same reason.
 *
 * [OFF] (the default) never applies it, [ALWAYS] always does, [AUTO] does from the session after the decoder
 * was seen falling behind ([LiteModeMemory]). It shapes the Automatic picture only: a Picture the
 * driver chose (Best, Balanced, Light, Custom) is never changed.
 */
enum class LiteMode { OFF, AUTO, ALWAYS }

object LitePicture {
    const val FPS = 30

    /** Whether the next session's Automatic picture is the lite one. */
    fun active(mode: LiteMode, engaged: Boolean): Boolean = when (mode) {
        LiteMode.OFF -> false
        LiteMode.ALWAYS -> true
        LiteMode.AUTO -> engaged
    }

    /** [choice] with the decoder's and GPU's load lowered; resolution, codec and size are untouched. */
    fun apply(choice: PictureChoice): PictureChoice = choice.copy(
        aaFps = minOf(choice.aaFps, FPS),
        carPlayFps = minOf(choice.carPlayFps, FPS),
        carPlayDirectVideo = true,
        aaSurfaceView = true,
        summary = choice.summary + "; Lite mode: $FPS fps, direct video, same resolution",
    )

    /** Whether [apply] changes anything, so Auto does not report a change it cannot make. */
    fun lowers(choice: PictureChoice): Boolean = apply(choice).copy(summary = choice.summary) != choice
}

/**
 * What Lite mode's [LiteMode.AUTO] learned: the decoder fell behind in a session, so the next ones
 * are lite. Kept apart from [UnifiedSettings] so a Settings screen saving an older copy cannot
 * undo it; choosing any Lite mode in Settings clears it ([reset]).
 */
class LiteModeMemory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val engaged: Boolean get() = prefs.getBoolean(KEY_ENGAGED, false)
    val reason: String? get() = prefs.getString(KEY_REASON, null)

    fun engage(reason: String) {
        prefs.edit().putBoolean(KEY_ENGAGED, true).putString(KEY_REASON, reason).apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "carlink_lite_mode"
        private const val KEY_ENGAGED = "engaged"
        private const val KEY_REASON = "reason"
    }
}
