package com.carlink.core.diagnostics

import android.content.Context
import java.io.File

/**
 * A small properties file the Android Auto process (`:aa`) writes and the main process reads,
 * so Diagnostics can show live decoder figures across the process boundary.
 */
object AaMediaSnapshotStore {
    const val FILE_NAME = "carlink_aa_media_snapshot.properties"
    private const val STALE_NS = 3_000_000_000L

    fun file(context: Context): File = File(context.applicationContext.noBackupFilesDir, FILE_NAME)

    fun write(context: Context, counters: MediaLinkCounters) {
        write(file(context), counters)
    }

    fun write(file: File, counters: MediaLinkCounters) {
        val text = buildString {
            appendLine("at=${counters.atNanos}")
            counters.width?.let { appendLine("w=$it") }
            counters.height?.let { appendLine("h=$it") }
            counters.configuredFps?.let { appendLine("fps=$it") }
            counters.codec?.let { appendLine("codec=$it") }
            counters.decoderName?.let { appendLine("decoder=$it") }
            counters.hardwareDecoder?.let { appendLine("hw=${if (it) 1 else 0}") }
            appendLine("received=${counters.received}")
            appendLine("rendered=${counters.rendered}")
            appendLine("decodeNanos=${counters.decodeNanos}")
            appendLine("decodeSamples=${counters.decodeSamples}")
            appendLine("dropped=${counters.dropped}")
            appendLine("late=${counters.late}")
            appendLine("bytes=${counters.bytes}")
            if (counters.streams.isNotEmpty()) {
                appendLine("streams=${counters.streams.joinToString(",") { it.name.lowercase() }}")
            }
            counters.sampleRateHz?.let { appendLine("rate=$it") }
            counters.bufferFillMs?.let { appendLine("buf=$it") }
        }
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    fun read(context: Context, nowNanos: Long = System.nanoTime()): MediaLinkCounters? =
        read(file(context), nowNanos)

    fun read(file: File, nowNanos: Long): MediaLinkCounters? {
        if (!file.isFile) return null
        val map = runCatching {
            file.readLines().mapNotNull { line ->
                val i = line.indexOf('=')
                if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
            }.toMap()
        }.getOrNull() ?: return null
        val at = map["at"]?.toLongOrNull() ?: return null
        if (nowNanos > 0 && at > 0 && nowNanos - at > STALE_NS) return null
        return MediaLinkCounters(
            width = map["w"]?.toIntOrNull(),
            height = map["h"]?.toIntOrNull(),
            configuredFps = map["fps"]?.toIntOrNull(),
            codec = map["codec"],
            decoderName = map["decoder"],
            hardwareDecoder = map["hw"]?.let { it == "1" },
            received = map["received"]?.toLongOrNull() ?: 0,
            rendered = map["rendered"]?.toLongOrNull() ?: 0,
            decodeNanos = map["decodeNanos"]?.toLongOrNull() ?: 0,
            decodeSamples = map["decodeSamples"]?.toLongOrNull() ?: 0,
            dropped = map["dropped"]?.toLongOrNull() ?: 0,
            late = map["late"]?.toLongOrNull() ?: 0,
            bytes = map["bytes"]?.toLongOrNull() ?: 0,
            streams = map["streams"].orEmpty().split(',').mapNotNull { raw ->
                AudioStreamKind.entries.find { it.name.equals(raw.trim(), ignoreCase = true) }
            }.toSet(),
            sampleRateHz = map["rate"]?.toIntOrNull(),
            bufferFillMs = map["buf"]?.toIntOrNull(),
            atNanos = at,
        )
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}
