package com.carlink.app.update

import android.app.Application
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * The manual install fallback's intent: the verified APK in Downloads/CarLink is handed to Android
 * through CarLinkFileProvider as a content:// URI with read access, never as a file:// path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class ManualInstallIntentTest {
    private val app: Application = RuntimeEnvironment.getApplication()

    /** FileProvider caches its roots per authority for the whole JVM; every test has its own temp roots. */
    @Before fun forgetCachedRoots() {
        val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (cache.get(null) as MutableMap<*, *>).clear()
    }

    private fun apkInOwnDownloads(): File {
        // On a device getExternalFilesDir(DIRECTORY_DOWNLOADS) is <files dir>/Download; Robolectric lays
        // type directories out differently, so the real layout is spelled out here.
        val dir = File(File(androidx.core.content.ContextCompat.getExternalFilesDirs(app, null)[0], Environment.DIRECTORY_DOWNLOADS), UpdateFiles.FOLDER_NAME).apply { mkdirs() }
        return File(dir, "CarLink-CS75Plus2023-v1.1.apk").apply { writeText("apk") }
    }

    @Test fun opensTheApkWithTheViewIntentOnAContentUri() {
        val file = apkInOwnDownloads()
        assertTrue(PackageInstallerUpdater(app).openForManualInstall(file))
        val intent = shadowOf(app).nextStartedActivity
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertEquals("content", intent.data!!.scheme)
        assertEquals("${app.packageName}.carlink.files", intent.data!!.authority)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue("the file stays", file.exists())
    }

    @Test fun aFileOutsideTheUpdateFolderIsNotHandedOut() {
        val stray = File.createTempFile("stray", ".apk")
        assertFalse(PackageInstallerUpdater(app).openForManualInstall(stray))
        assertEquals(null, shadowOf(app).nextStartedActivity)
        stray.delete()
    }

    @Test fun theCacheFolderFallbackIsServedToo() {
        val dir = File(File(app.cacheDir, "updates"), UpdateFiles.FOLDER_NAME).apply { mkdirs() }
        val file = File(dir, "CarLink-CS75Plus2023-v1.1.apk").apply { writeText("apk") }
        assertTrue(PackageInstallerUpdater(app).openForManualInstall(file))
    }
}
