package com.carlink.app.ui

import android.view.Window
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides or shows the status bar (notification bar) inside CarLink's own windows only,
 * with immersive-sticky swipe-to-show. It does not send broadcasts or change the bar
 * in other apps. The Changan FceSystemUI hide path is separate and untouched.
 */
internal object AppImmersiveStatusBar {
    fun apply(window: Window, hide: Boolean) {
        apply(WindowInsetsControllerCompat(window, window.decorView), hide)
    }

    fun apply(controller: WindowInsetsControllerCompat, hide: Boolean) {
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val statusBars = WindowInsetsCompat.Type.statusBars()
        if (hide) controller.hide(statusBars) else controller.show(statusBars)
    }
}
