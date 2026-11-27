package com.carlink.app.ui

import androidx.core.view.WindowInsetsControllerCompat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.activity.ComponentActivity
import android.app.Application

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class AppImmersiveStatusBarTest {
    @Test fun hideUsesTransientSwipeBehavior() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        AppImmersiveStatusBar.apply(controller, hide = true)
        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            controller.systemBarsBehavior,
        )
        AppImmersiveStatusBar.apply(controller, hide = false)
        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            controller.systemBarsBehavior,
        )
    }
}
