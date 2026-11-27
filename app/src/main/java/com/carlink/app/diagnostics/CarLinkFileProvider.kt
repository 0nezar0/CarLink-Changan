package com.carlink.app.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * CarLink's own FileProvider (authority `<applicationId>.carlink.files`), so shared reports
 * don't depend on either engine's provider. It serves `cache/shared/` (reports) and the update
 * folder Downloads/CarLink (a verified update APK handed to Android's installer by hand).
 */
class CarLinkFileProvider : FileProvider() {
    companion object {
        const val SHARED_DIR = "shared"

        fun authority(context: Context) = "${context.packageName}.carlink.files"

        /** content:// URI for [file]; throws IllegalArgumentException outside the folders in carlink_file_paths.xml. */
        fun uriFor(context: Context, file: File): android.net.Uri = getUriForFile(context, authority(context), file)

        /** Opens the system share sheet for a text file in `cache/shared/`. */
        fun shareText(context: Context, file: File, subject: String, chooserTitle: String) {
            val uri = getUriForFile(context, authority(context), file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            send.clipData = ClipData.newRawUri(subject, uri)
            val chooser = Intent.createChooser(send, chooserTitle)
            if (context !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(chooser) }
        }
    }
}
