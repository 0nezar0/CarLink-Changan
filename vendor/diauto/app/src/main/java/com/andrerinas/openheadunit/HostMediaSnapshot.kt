package com.andrerinas.openheadunit

import java.io.File

/**
 * CarLink: writes live Android Auto decoder figures to [HostIntegration.mediaSnapshotFile]
 * for Settings › Diagnostics › Connection status in the main process.
 * The file format matches com.carlink.core.diagnostics.AaMediaSnapshotStore.
 */
object HostMediaSnapshot {
    @Volatile private var lastWriteNs = 0L

    fun write(
        width: Int,
        height: Int,
        configuredFps: Int,
        codec: String?,
        decoderName: String?,
        received: Long,
        rendered: Long,
        dropped: Long,
        late: Long,
        bytes: Long,
        nowNanos: Long = System.nanoTime(),
    ) {
        val file = HostIntegration.mediaSnapshotFile ?: return
        if (nowNanos - lastWriteNs < 200_000_000L) return
        lastWriteNs = nowNanos
        val hardware = decoderName?.let { name ->
            val n = name.lowercase()
            !(n.startsWith("omx.google.") || n.startsWith("c2.android.") || n.startsWith("omx.ffmpeg.") ||
                n.contains(".sw.") || n.contains("software") || n.contains("ffmpeg"))
        }
        val text = buildString {
            appendLine("at=$nowNanos")
            if (width > 0) appendLine("w=$width")
            if (height > 0) appendLine("h=$height")
            if (configuredFps > 0) appendLine("fps=$configuredFps")
            if (!codec.isNullOrBlank()) appendLine("codec=$codec")
            if (!decoderName.isNullOrBlank()) appendLine("decoder=$decoderName")
            if (hardware != null) appendLine("hw=${if (hardware) 1 else 0}")
            appendLine("received=$received")
            appendLine("rendered=$rendered")
            appendLine("decodeNanos=0")
            appendLine("decodeSamples=0")
            appendLine("dropped=$dropped")
            appendLine("late=$late")
            appendLine("bytes=$bytes")
        }
        runCatching {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) parent.mkdirs()
            val tmp = File(parent, "${file.name}.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) {
                file.writeText(text)
                tmp.delete()
            }
        }
    }
}
