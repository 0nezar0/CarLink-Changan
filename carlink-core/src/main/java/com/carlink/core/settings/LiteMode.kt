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
 * driver chose (Best, Balanced, Low resolution, Custom) is never changed, and Settings says so.
 *
 * Where it meets the other picture rules (docs/LOW_END_PERFORMANCE.md, "Lite mode"), in the order
 * [com.carlink.core.connection.Connection] applies them: PicturePolicy (the decoder), StereoQuirks
 * (a known stereo), ChanganPicture (Changan CarPlay at 30 fps), then Lite mode. Lite only lowers
 * the frame rate (never raises one a rule before it lowered) and only turns a video surface on
 * where no earlier rule turned it off: a stereo quirk that says a surface breaks the picture wins.
 * What the engines already do on their own is not repeated here: both negotiate the frame rate
 * with the phone (no frame is dropped before the decoder), DiPlay sets the realtime priority and
 * operating rate, and DiAuto drops only already-decoded late frames at the renderer.
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

    /**
     * Whether Lite mode shapes the next session: the mode says so and Picture is Automatic (the
     * only picture it changes). Settings and Visual effects read this too.
     */
    fun inEffect(mode: LiteMode, engaged: Boolean, picture: Picture): Boolean =
        picture == Picture.AUTOMATIC && active(mode, engaged)

    /** [inEffect] for this device's stored settings. Never throws. */
    fun inEffect(context: Context): Boolean = runCatching {
        val settings = UnifiedSettingsStore(context).load()
        inEffect(settings.liteMode, LiteModeMemory(context).engaged, settings.picture)
    }.getOrDefault(false)

    /** [choice] with the decoder's and GPU's load lowered; resolution, codec and size are untouched. */
    fun apply(choice: PictureChoice): PictureChoice = choice.copy(
        aaFps = minOf(choice.aaFps, FPS),
        carPlayFps = minOf(choice.carPlayFps, FPS),
        // An explicit false is a stereo quirk's "this surface breaks the picture here": keep it.
        carPlayDirectVideo = choice.carPlayDirectVideo ?: true,
        aaSurfaceView = choice.aaSurfaceView ?: true,
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
