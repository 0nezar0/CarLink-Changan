package com.carlink.app.update

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Where a downloaded update is kept: Downloads/CarLink, next to the full reports
 * (ui/diagnostics/ReportProblem). On Android 9 and older that is the public Download folder when
 * CarLink has storage access (asked when the driver taps "Download & install"). Without it, or on
 * Android 10 and newer (no direct file access to shared storage), CarLink's own Downloads/CarLink
 * (Android/data/<package>/files), which needs no permission. The installer reads the file itself
 * into a PackageInstaller session, so no content URI is needed.
 */
object UpdateFiles {
    /** The folder's name inside Downloads: the same Downloads/CarLink as the full reports. */
    const val FOLDER_NAME = "CarLink"

    /** The one place the update folder is chosen: Downloads/CarLink. */
    fun folder(context: Context): File {
        val public = Build.VERSION.SDK_INT < 29 &&
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        val dir = if (public) File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER_NAME)
        else File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.cacheDir, "updates"), FOLDER_NAME)
        if (!dir.isDirectory) dir.mkdirs()
        return dir
    }

    /** The public folder needs asking on Android 9 and older. */
    fun needsStoragePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 29 &&
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

    /**
     * Update APKs to delete in [dir]: this build's update files (`CarLink-CS75Plus2023-v*.apk`) older
     * than the installed [current] version. The installed version's own APK stays, so the folder
     * keeps one copy to reinstall from. Reports and any other file are never touched.
     */
    fun stale(dir: File, current: AppVersion): List<File> =
        dir.listFiles().orEmpty().filter { file ->
            val version = ReleaseRepo.apkVersion(file.name)
            file.isFile && version != null && version < current
        }
}
