package com.carlink.app.ui.settings

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class LaunchableAppsTest {

    @Test fun queryFlagsAreNotMatchDefaultOnly() {
        assertEquals(0, LaunchableApps.QUERY_FLAGS)
        assertTrue(LaunchableApps.QUERY_FLAGS and PackageManager.MATCH_DEFAULT_ONLY == 0)
    }

    @Test fun pickerSourceDoesNotUseMatchDefaultOnly() {
        val text = listOf(
            "src/main/java/com/carlink/app/ui/settings/SettingsChangan.kt",
            "app/src/main/java/com/carlink/app/ui/settings/SettingsChangan.kt",
        ).map { File(it) }.first { it.isFile }.readText()
        assertFalse(
            "SettingsChangan must not query launchable apps with MATCH_DEFAULT_ONLY",
            "MATCH_DEFAULT_ONLY" in text,
        )
        assertTrue("LaunchableApps" in text)
    }

    @Test fun listsLauncherActivitiesThatOmitCategoryDefault() {
        // This is the car bug: MATCH_DEFAULT_ONLY hid apps whose launcher activity
        // has MAIN/LAUNCHER but not CATEGORY_DEFAULT.
        val pm = RuntimeEnvironment.getApplication().packageManager
        val shadow = Shadows.shadowOf(pm)
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        shadow.addResolveInfoForIntent(launch, resolveInfo("com.fce.hiddenapp", "Hidden"))
        val names = LaunchableApps.list(pm).map { it.packageName }
        assertTrue("expected hiddenapp in $names", "com.fce.hiddenapp" in names)
    }

    @Test fun packagesWithoutLaunchActivitiesAreLeftOut() {
        val pm = RuntimeEnvironment.getApplication().packageManager
        val pkg = "com.carlink.test.background"
        val info = android.content.pm.PackageInfo().apply {
            packageName = pkg
            applicationInfo = ApplicationInfo().apply { packageName = pkg; nonLocalizedLabel = "Background" }
        }
        Shadows.shadowOf(pm).installPackage(info)
        assertFalse(LaunchableApps.list(pm).any { it.packageName == pkg })
    }

    @Test fun sortedPutsOwnPackageFirstThenSelectedThenLabel() {
        val apps = listOf(
            LaunchableApps.App("com.b", "Beta", null),
            LaunchableApps.App("com.a", "Alpha", null),
            LaunchableApps.App("com.own", "CarLink", null),
            LaunchableApps.App("com.s", "Selected", null),
        )
        val sorted = LaunchableApps.sorted(apps, ownPackage = "com.own", selected = setOf("com.s"))
        assertEquals(listOf("com.own", "com.s", "com.a", "com.b"), sorted.map { it.packageName })
    }

    @Test fun searchMatchesLabelOrPackage() {
        val app = LaunchableApps.App("com.fce.radio", "راديو", null)
        assertTrue(LaunchableApps.matchesSearch(app, "راد"))
        assertTrue(LaunchableApps.matchesSearch(app, "FCE"))
        assertTrue(LaunchableApps.matchesSearch(app, "  radio  "))
        assertFalse(LaunchableApps.matchesSearch(app, "settings"))
        assertTrue(LaunchableApps.matchesSearch(app, ""))
    }

    private fun resolveInfo(pkg: String, label: String): ResolveInfo = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            packageName = pkg
            name = "$pkg.Main"
            applicationInfo = ApplicationInfo().apply { packageName = pkg }
        }
        nonLocalizedLabel = label
    }
}
