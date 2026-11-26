package com.carlink.app.update

import android.content.Context
import android.util.Log
import com.carlink.app.BuildConfig
import com.carlink.app.ui.UiPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.MainScope

/** The app's one [UpdateController], wired to Android (network, PackageManager, PackageInstaller). */
object Updates {
    /**
     * The package of the APK installed in the car: the release build from the "Build CarLink"
     * workflow (`-Pcarlink.applicationId=com.carlink.app.hudtest`). The local debug build has the
     * same package but never checks or installs (BuildConfig.DEBUG).
     */
    const val RELEASE_PACKAGE = "com.carlink.app.hudtest"
    private const val TAG = "CarLink-Update"

    @Volatile private var instance: UpdateController? = null

    fun get(context: Context): UpdateController = instance ?: synchronized(this) {
        instance ?: create(context.applicationContext).also { instance = it }
    }

    /**
     * Only the car's release build updates itself: a release build of [RELEASE_PACKAGE] signed with
     * this build's key ([ReleaseKey]). A build signed with another key could not install the update anyway.
     */
    fun isSupported(context: Context): Boolean =
        !BuildConfig.DEBUG && context.packageName == RELEASE_PACKAGE &&
            AndroidApkInspector.ownSigners(context) == setOf(ReleaseKey.normalized)

    private fun create(app: Context): UpdateController {
        val current = AppVersion.parseVersionName(BuildConfig.VERSION_NAME) ?: AppVersion(0, 0, 0)
        val http = UrlConnectionHttp("CarLink/${BuildConfig.VERSION_NAME} (Android)")
        val supported = isSupported(app)
        val prefs = UiPreferences(app)
        Log.i(TAG, "updater: version $current, ${if (supported) "updates on" else "updates off (not the signed release build)"}")
        return UpdateController(
            scope = MainScope(),
            io = Dispatchers.IO,
            source = GitHubReleaseSource(http, current) { prefs.testVersions },
            http = http,
            verifier = UpdateVerifier(JavaSha256, AndroidApkInspector(app), app.packageName, BuildConfig.VERSION_CODE.toLong()),
            installer = PackageInstallerUpdater(app),
            prefs = prefs,
            current = current,
            supported = supported,
            downloadDir = { UpdateFiles.folder(app) },
            log = { Log.i(TAG, it) },
            awaitSessionQuiet = {
                val connection = com.carlink.core.connection.Connection.get(app)
                kotlinx.coroutines.flow.combine(connection.state, connection.pendingSwitch) { s, p -> UpdatePolicy.sessionQuiet(s, p) }
                    .first { quiet -> quiet.also { if (!it) Log.i(TAG, "update ready; installing once the session ends") } }
            },
        )
    }
}
