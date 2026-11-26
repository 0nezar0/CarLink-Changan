package com.carlink.app.gallery

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.widget.LinearLayout
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.carlink.app.R
import com.carlink.app.switcher.Cs75NavigationOverlay
import com.carlink.app.switcher.FceBarAccessibilityService
import com.carlink.app.switcher.InstalledAppsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "ar-rSA-ldrtl-w1280dp-h720dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstalledAppsScreenTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun navigationButtonShowsLaunchableAppsAndSupportsAppInfo() {
        val app = RuntimeEnvironment.getApplication()
        val pm = shadowOf(app.packageManager)
        val radio = "com.carlink.test.radio"
        val servicePkg = "com.carlink.test.service"
        fun install(pkg: String, label: String): ApplicationInfo {
            val info = ApplicationInfo().apply { packageName = pkg; nonLocalizedLabel = label; enabled = true }
            pm.installPackage(PackageInfo().apply { packageName = pkg; applicationInfo = info })
            return info
        }
        val radioInfo = install(radio, "راديو التجربة")
        install(servicePkg, "خدمة السيارة التجريبية")
        val activity = ActivityInfo().apply {
            packageName = radio
            name = "$radio.RadioActivity"
            applicationInfo = radioInfo
            enabled = true
            exported = true
        }
        val entry = ResolveInfo().apply { activityInfo = activity; nonLocalizedLabel = radioInfo.nonLocalizedLabel }
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.addResolveInfoForIntent(main, entry)
        pm.addResolveInfoForIntent(Intent(main).setPackage(radio), entry)

        val service = Robolectric.buildService(FceBarAccessibilityService::class.java).create().get()
        val bar = LinearLayout(service)
        Cs75NavigationOverlay(service).addNavigationButtons(bar)
        val label = service.getString(R.string.carlink_cs75_nav_apps)
        (0 until bar.childCount).map(bar::getChildAt).single { it.contentDescription == label }.performClick()
        val destination = shadowOf(app).nextStartedActivity
        assertEquals(InstalledAppsActivity::class.java.name, destination.component?.className)
        ActivityScenario.launch<InstalledAppsActivity>(destination).use { scenario ->
            compose.waitUntil(30000) {
                compose.onAllNodesWithText("راديو التجربة").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("خدمة السيارة التجريبية").assertDoesNotExist()
            if (System.getProperty("carlink.screenshots") == "true") {
                scenario.onActivity {
                    val view = it.window.decorView
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    val file = File(System.getProperty("carlink.repoRoot"), "docs/screenshots/settings/installed-apps-ar.png")
                    file.parentFile!!.mkdirs()
                    file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                }
            }
            val radioTile = compose.onNodeWithText("راديو التجربة")
            radioTile.performTouchInput { longClick() }
            compose.waitForIdle()
            val details = shadowOf(app).nextStartedActivity
            assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, details?.action)
            assertEquals("package:$radio", details?.dataString)
            assertNull(shadowOf(app).nextStartedActivity)
            radioTile.performClick()
            assertEquals(ComponentName(radio, activity.name), shadowOf(app).nextStartedActivity?.component)
        }
    }
}
