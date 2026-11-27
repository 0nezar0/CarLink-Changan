package com.carlink.app.update

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import com.carlink.app.diagnostics.CarLinkFileProvider
import android.util.Log
import java.io.File
import java.io.IOException

/** Installs a verified APK. The outcome arrives later through [UpdateController.onInstallStatus]. */
interface UpdateInstaller {
    /** Android lets CarLink install apps ("Install unknown apps" allowed; always true below API 26). */
    fun canInstall(): Boolean

    /** Opens Android's "Install unknown apps" page for CarLink. */
    fun openInstallPermissionSettings()

    /** Writes [file] into a PackageInstaller session and commits it. Blocking; throws [IOException]. */
    fun install(file: File)

    /**
     * Manual fallback: hands the already verified [file] to Android's installer (or a file manager)
     * with an ACTION_VIEW intent on a content:// URI. False when nothing could be started.
     */
    fun openForManualInstall(file: File): Boolean

    /** Last resort of the manual fallback: opens the folder that holds [file]. False when no app can show it. */
    fun openContainingFolder(file: File): Boolean
}

/** What the PackageInstaller session reported. */
sealed interface InstallStatus {
    /** Android shows its confirmation screen. */
    data object WaitingForUser : InstallStatus
    data object Success : InstallStatus
    data object Aborted : InstallStatus
    data class Failure(val message: String?) : InstallStatus
}

private const val APK_MIME = "application/vnd.android.package-archive"

/** [UpdateInstaller] on [PackageInstaller] sessions. */
class PackageInstallerUpdater(context: Context) : UpdateInstaller {
    private val app = context.applicationContext

    override fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || app.packageManager.canRequestPackageInstalls()

    override fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { app.startActivity(intent) }.onFailure {
            // Some head-unit builds lack the per-app page; the general "unknown apps" list then.
            runCatching { app.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    override fun openForManualInstall(file: File): Boolean {
        val uri = runCatching { CarLinkFileProvider.uriFor(app, file) }.getOrNull() ?: return false
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { app.startActivity(view) }.isSuccess
    }

    override fun openContainingFolder(file: File): Boolean {
        val folder = file.parentFile ?: return false
        // A file manager that understands a folder; the folder is shown by its content:// URI.
        val uri = runCatching { CarLinkFileProvider.uriFor(app, folder) }.getOrNull()
        val intents = buildList {
            if (uri != null) add(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "resource/folder"))
            if (uri != null) add(Intent(Intent.ACTION_VIEW).setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR))
            add(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        }
        return intents.any { intent ->
            val launch = intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { app.startActivity(launch) }.isSuccess
        }
    }

    override fun install(file: File) {
        val installer = app.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            setSize(file.length())
            // When Android allows it (CarLink installed the current version itself), update without
            // a second confirmation; otherwise Android asks as usual.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("CarLink.apk", 0, file.length()).use { output ->
                        input.copyTo(output, 64 * 1024)
                        session.fsync(output)
                    }
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                val status = PendingIntent.getBroadcast(app, sessionId,
                    Intent(app, UpdateInstallReceiver::class.java).setAction(UpdateInstallReceiver.ACTION), flags)
                session.commit(status.intentSender)
            }
        } catch (failure: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw failure as? IOException ?: IOException(failure.message, failure)
        }
    }
}

/** Receives the PackageInstaller session's status and hands it to the [UpdateController]. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.i(TAG, "install status $status ${message.orEmpty()}")
        val result = when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm == null) {
                    InstallStatus.Failure(message)
                } else {
                    runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .fold({ InstallStatus.WaitingForUser }, { InstallStatus.Failure(it.message) })
                }
            }
            PackageInstaller.STATUS_SUCCESS -> InstallStatus.Success
            PackageInstaller.STATUS_FAILURE_ABORTED -> InstallStatus.Aborted
            else -> InstallStatus.Failure(message)
        }
        Updates.get(context).onInstallStatus(result)
    }

    companion object {
        const val ACTION = "com.carlink.app.update.INSTALL_STATUS"
        private const val TAG = "CarLink-Update"
    }
}
