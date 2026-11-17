package com.carlink.core.connection

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import com.carlink.core.core.Engine
import com.carlink.core.diagnostics.EventLog
import com.carlink.core.settings.LiteMode
import com.carlink.core.settings.LiteModeMemory
import com.carlink.core.settings.LitePicture
import com.carlink.core.settings.Picture
import com.carlink.core.settings.UnifiedSettingsStore

/**
 * Lite mode's [LiteMode.AUTO] (settings/LiteMode): hears the engines say their video decoder is
 * falling behind and makes the next sessions lite. The running session is not renegotiated: a
 * picture change mid-drive costs a black screen and a reconnect, worse than the stutter.
 *
 * Android Auto reports it once per session after two 5 s windows that shed frames while waiting
 * for the codec (DiAuto VideoBackpressurePolicy); CarPlay reports each "video decoder input
 * stalled" recovery, so it counts [CARPLAY_STALLS] of them within [CARPLAY_WINDOW_MILLIS].
 */
object LiteModeMonitor {
    const val CARPLAY_STALLS = 2
    const val CARPLAY_WINDOW_MILLIS = 3 * 60_000L

    /** The last Automatic picture handed to an engine, so Auto knows whether Lite would change it. */
    @Volatile var lastAutomatic: PictureChoice? = null

    private val carPlayStalls = ArrayDeque<Long>()

    /** Whether one strain report makes Auto engage: pure, for tests. */
    fun shouldEngage(mode: LiteMode, picture: Picture, alreadyEngaged: Boolean, lastAutomatic: PictureChoice?, enoughEvidence: Boolean) =
        mode == LiteMode.AUTO && picture == Picture.AUTOMATIC && !alreadyEngaged && enoughEvidence &&
            // Lite would change nothing (already 30 fps and direct video): do not claim it helped.
            (lastAutomatic == null || LitePicture.lowers(lastAutomatic))

    /** Counts a CarPlay stall at [now]; true once [CARPLAY_STALLS] fell within [CARPLAY_WINDOW_MILLIS]. */
    @Synchronized
    fun carPlayStall(now: Long): Boolean {
        carPlayStalls.addLast(now)
        while (now - carPlayStalls.first() > CARPLAY_WINDOW_MILLIS) carPlayStalls.removeFirst()
        return carPlayStalls.size >= CARPLAY_STALLS
    }

    /** An engine's decoder fell behind ([detail] for the log). Never throws. */
    fun onDecoderStrain(context: Context, engine: Engine, detail: String?) {
        runCatching {
            val enough = engine != Engine.CARPLAY || carPlayStall(SystemClock.elapsedRealtime())
            val settings = UnifiedSettingsStore(context).load()
            val memory = LiteModeMemory(context)
            val engage = shouldEngage(settings.liteMode, settings.picture, memory.engaged, lastAutomatic, enough)
            if (!engage && !enough) return
            val what = "$engine video decoder fell behind${detail?.let { " ($it)" } ?: ""}"
            EventLog.add("CarLink", "$what; Lite mode ${settings.liteMode}: " + if (engage) "on from the next connection" else "no change")
            if (!engage) return
            memory.engage(what)
            val app = context.applicationContext
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(app, com.carlink.core.R.string.carlink_core_lite_mode_engaged, Toast.LENGTH_LONG).show()
            }
        }
    }
}
