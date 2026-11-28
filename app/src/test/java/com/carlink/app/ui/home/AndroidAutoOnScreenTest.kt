package com.carlink.app.ui.home

import com.carlink.app.gallery.HomeFakes
import com.carlink.app.R
import com.carlink.app.ui.hasNativeAndroidAuto
import com.carlink.app.ui.offersAndroidAutoOnScreen
import com.carlink.core.core.LinkProblem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Home offers "Run Android Auto on this screen" only with the switch on, Android Auto installed, and Home idle. */
class AndroidAutoOnScreenTest {
    @Test fun offeredOnlyWhenSwitchedOnInstalledAndIdle() {
        val on = HomeFakes.prefs.copy(androidAutoOnScreen = true, androidAutoInstalled = true, androidAutoCanRunCar = true)
        val offeredIn = HomeMode.entries.filter { on.offersAndroidAutoOnScreen(it.isIdle()) }.toSet()
        assertEquals(setOf(HomeMode.READY_EMPTY, HomeMode.READY_PHONE, HomeMode.CABLE_ONLY, HomeMode.BLUETOOTH_OFF), offeredIn)
        assertEquals(false, on.copy(androidAutoOnScreen = false).offersAndroidAutoOnScreen(true))
        assertEquals(false, on.copy(androidAutoInstalled = false).offersAndroidAutoOnScreen(true))
        // Installed but Google switched its car mode off here (a Samsung tablet): no button that can only fail.
        assertEquals(false, on.copy(androidAutoCanRunCar = false).offersAndroidAutoOnScreen(true))
    }

    @Test fun nativeAndroidAutoIsInstalledAndCanRunACarSession() {
        val native = HomeFakes.prefs.copy(androidAutoInstalled = true, androidAutoCanRunCar = true)
        assertEquals(true, native.hasNativeAndroidAuto)
        assertEquals(false, native.copy(androidAutoInstalled = false).hasNativeAndroidAuto)
        assertEquals(false, native.copy(androidAutoCanRunCar = false).hasNativeAndroidAuto)
    }

    @Test fun aStuckServerWithoutNativeAndroidAutoUsesThePhoneInstructions() {
        val (_, body) = failureCopyRes(LinkProblem.HEAD_UNIT_SERVER_STUCK, nativeAndroidAuto = false)
        assertEquals(R.string.carlink_home_error_hu_server_stuck_body_phone, body)
        val (_, nativeBody) = failureCopyRes(LinkProblem.HEAD_UNIT_SERVER_STUCK, nativeAndroidAuto = true)
        assertEquals(R.string.carlink_home_error_hu_server_stuck_body, nativeBody)
        val (_, off) = failureCopyRes(LinkProblem.HEAD_UNIT_SERVER_OFF, nativeAndroidAuto = false)
        assertEquals(R.string.carlink_home_error_hu_server_body_no_app, off)
        val (_, offNative) = failureCopyRes(LinkProblem.HEAD_UNIT_SERVER_OFF, nativeAndroidAuto = true)
        assertEquals(R.string.carlink_home_error_hu_server_body, offNative)
    }
}
