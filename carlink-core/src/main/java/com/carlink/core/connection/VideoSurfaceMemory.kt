package com.carlink.core.connection

import android.content.Context
import com.carlink.core.core.Engine
import com.carlink.core.settings.Picture

/**
 * The video-surface switches CarLink itself turned on in an engine's own settings: DiPlay's
 * `direct_video_output` and DiAuto's `view-mode` SurfaceView, written by Automatic picture (slow
 * hardware, a stereo quirk or Lite mode). Both are stored by the engine and outlive the session, so
 * without this a driver who turned Lite mode off kept its SurfaceView for good.
 *
 * Who owns the switch: under Automatic and the presets (Best, Balanced, Low resolution) CarLink
 * does, and turns back what it turned on once nothing asks for it; under Custom the driver does
 * (changing Settings › Display › Direct video output or Lab › Video surface sets Picture to Custom),
 * so the record is dropped and the engine's value is left as it is.
 */
object VideoSurfaceMemory {
    private const val PREFS = "carlink_video_surface"
    private const val KEY_CARPLAY = "carplay_direct_video"
    private const val KEY_AA = "aa_surface_view"

    /**
     * [choice] with an undo for a switch CarLink turned on before and nothing asks for now: false
     * (DiPlay's default off; DiAuto's own default surface). Pure, for tests.
     */
    fun reconcile(choice: PictureChoice, picture: Picture, carPlayWritten: Boolean, aaWritten: Boolean): PictureChoice =
        if (picture == Picture.CUSTOM) choice
        else choice.copy(
            carPlayDirectVideo = choice.carPlayDirectVideo ?: if (carPlayWritten) false else null,
            aaSurfaceView = choice.aaSurfaceView ?: if (aaWritten) false else null,
        )

    /** [reconcile] with what this device remembers. */
    fun reconcile(context: Context, choice: PictureChoice, picture: Picture): PictureChoice {
        val prefs = prefs(context)
        return reconcile(choice, picture, prefs.getBoolean(KEY_CARPLAY, false), prefs.getBoolean(KEY_AA, false))
    }

    /** After [engine]'s settings were written with [choice] for [picture]: remember what is CarLink's now. */
    fun written(context: Context, engine: Engine, choice: PictureChoice, picture: Picture) {
        val key = if (engine == Engine.CARPLAY) KEY_CARPLAY else KEY_AA
        val value = if (engine == Engine.CARPLAY) choice.carPlayDirectVideo else choice.aaSurfaceView
        val edit = prefs(context).edit()
        when {
            picture == Picture.CUSTOM -> edit.remove(KEY_CARPLAY).remove(KEY_AA) // the driver's now
            value == true -> edit.putBoolean(key, true)
            value == false -> edit.remove(key) // turned back
            else -> return
        }
        edit.apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
