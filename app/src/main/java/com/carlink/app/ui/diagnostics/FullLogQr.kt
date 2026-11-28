package com.carlink.app.ui.diagnostics

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carlink.app.R
import com.carlink.app.diagnostics.FullLogOnPhone
import com.carlink.app.ui.CarLinkViewModel
import com.carlink.app.ui.components.CarSheet
import com.carlink.app.ui.components.QrCode
import com.carlink.app.ui.components.ltr
import com.carlink.app.ui.premium.GlassButton
import com.carlink.app.ui.premium.IonButton
import com.carlink.app.ui.settings.isCar
import com.carlink.app.ui.theme.CarLinkTokens
import com.shilapi.xcertplay.backup.SettingsBackupServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the QR-for-the-full-log sheet shows ([FullLogQrSheet]). */
internal sealed interface FullLogQrUi {
    data object Preparing : FullLogQrUi
    data class Ready(val endpoint: FullLogOnPhone.Endpoint, val codeEntryOpen: Boolean = true) : FullLogQrUi
    data object NoNetwork : FullLogQrUi
    data object Failed : FullLogQrUi
}

/** The page's words in the stereo's language ([FullLogOnPhone.PageTexts]). */
internal fun fullLogPageTexts(context: Context): FullLogOnPhone.PageTexts {
    val locale = context.resources.configuration.locales[0]
    return FullLogOnPhone.PageTexts(
        lang = locale.language.ifEmpty { "en" },
        rtl = android.text.TextUtils.getLayoutDirectionFromLocale(locale) == android.view.View.LAYOUT_DIRECTION_RTL,
        title = context.getString(R.string.carlink_fulllog_title),
        hint = context.getString(R.string.carlink_fulllog_page_hint),
        copy = context.getString(R.string.carlink_fulllog_page_copy),
        copied = context.getString(R.string.carlink_fulllog_page_copied),
        copyFailed = context.getString(R.string.carlink_fulllog_page_copy_failed),
        download = context.getString(R.string.carlink_fulllog_page_download),
        loading = context.getString(R.string.carlink_fulllog_page_loading),
        loadFailed = context.getString(R.string.carlink_fulllog_page_load_failed),
        codePrompt = context.getString(R.string.carlink_fulllog_page_code_prompt),
        codeOpen = context.getString(R.string.carlink_fulllog_page_code_open),
        codeWrong = context.getString(R.string.carlink_fulllog_page_code_wrong),
        codeLocked = context.getString(R.string.carlink_fulllog_page_code_locked),
    )
}

/**
 * Diagnostics › QR for the full log: exports the full log (cleaned, on an IO thread), starts the
 * phone page ([FullLogOnPhone]) and shows its sheet. The page stops when the sheet closes, when the
 * screen leaves composition, or after ten idle minutes (the sheet then closes too).
 * [onReport] is the short GitHub QR code, the fallback without a network.
 */
@Composable
internal fun rememberFullLogOnPhone(vm: CarLinkViewModel, onReport: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf<FullLogQrUi?>(null) }
    var session by remember { mutableStateOf<FullLogOnPhone.Session?>(null) }
    var attempt by remember { mutableStateOf(0) }

    fun close() {
        attempt++
        session?.stop()
        session = null
        ui = null
    }

    fun open() {
        session?.stop()
        session = null
        val mine = ++attempt
        ui = FullLogQrUi.Preparing
        val texts = fullLogPageTexts(context)
        scope.launch {
            val started = withContext(Dispatchers.IO) {
                runCatching {
                    val text = exportCleanLog(context, vm::savedSecrets)
                    FullLogOnPhone.start(text, texts, SettingsBackupServer.addressCandidates(),
                        onIdle = { scope.launch(Dispatchers.Main) { if (attempt == mine) close() } },
                        onCodeEntryClosed = {
                            scope.launch(Dispatchers.Main) {
                                val now = ui
                                if (attempt == mine && now is FullLogQrUi.Ready) ui = now.copy(codeEntryOpen = false)
                            }
                        })
                }.getOrDefault(FullLogOnPhone.Start.Failed)
            }
            if (attempt != mine) {
                // Closed while the log was being prepared.
                (started as? FullLogOnPhone.Start.Started)?.session?.stop()
                return@launch
            }
            ui = when (started) {
                is FullLogOnPhone.Start.Started -> { session = started.session; FullLogQrUi.Ready(started.session.endpoint) }
                FullLogOnPhone.Start.NoNetwork -> FullLogQrUi.NoNetwork
                FullLogOnPhone.Start.Failed -> FullLogQrUi.Failed
            }
        }
    }

    DisposableEffect(Unit) { onDispose { session?.stop() } }

    ui?.let { shown ->
        FullLogQrSheet(shown,
            onRetry = { open() },
            onReport = { close(); onReport() },
            onCopyLog = { scope.launch { copyConnectionLog(context, vm::savedSecrets) } },
            onDone = { close() })
    }
    return remember(vm) { { if (ui == null) open() } }
}

/**
 * The QR code of the stereo's own page with the full log, the link written out, the address and
 * six-digit code to type, and what to do when the phone cannot reach this screen.
 */
@Composable
internal fun FullLogQrSheet(ui: FullLogQrUi, onRetry: () -> Unit, onReport: () -> Unit, onCopyLog: () -> Unit, onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dimens = CarLinkTokens.dimens
    val gap = if (isCar) 20.dp else 14.dp
    // Every string is read here, before the sheet's dialog window: that window has its own Compose root,
    // which takes the context of the activity.
    val ready = ui as? FullLogQrUi.Ready
    val preparing = stringResource(R.string.carlink_fulllog_preparing)
    val body = stringResource(R.string.carlink_fulllog_body)
    val a11y = stringResource(R.string.carlink_fulllog_qr_a11y)
    val typed = ready?.let { stringResource(R.string.carlink_fulllog_type, ltr(it.endpoint.typedAddress), ltr(FullLogOnPhone.spacedCode(it.endpoint.code))) }
    val codeClosed = stringResource(R.string.carlink_fulllog_code_closed)
    val others = ready?.endpoint?.otherUrls?.takeIf { it.isNotEmpty() }
        ?.let { urls -> stringResource(R.string.carlink_fulllog_other, urls.joinToString("  ·  ") { ltr(it) }) }
    val privacy = stringResource(R.string.carlink_fulllog_privacy)
    val stops = stringResource(R.string.carlink_fulllog_stops)
    val noNetwork = stringResource(R.string.carlink_fulllog_no_network)
    val failed = stringResource(R.string.carlink_fulllog_failed)
    val retry = stringResource(R.string.carlink_fulllog_retry)
    val report = stringResource(R.string.carlink_report_action)
    val copy = stringResource(R.string.carlink_diag_log_copy)
    val done = stringResource(R.string.carlink_report_done)
    CarSheet(stringResource(R.string.carlink_fulllog_title), onDismiss = onDone) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val qr = minOf(maxWidth, if (isCar) 360.dp else 300.dp)
            Column(Modifier.fillMaxWidth()) {
                when (ui) {
                    FullLogQrUi.Preparing ->
                        Text(preparing, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    is FullLogQrUi.Ready -> {
                        val e = ui.endpoint
                        Text(body, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, modifier = Modifier.widthIn(max = 720.dp))
                        Spacer(Modifier.height(gap))
                        QrCode(e.url, qr, a11y, Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(10.dp))
                        Text(ltr(e.url), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(gap))
                        if (ui.codeEntryOpen && typed != null) Note(R.drawable.carlink_ic_smartphone, typed)
                        else Note(R.drawable.carlink_ic_warning, codeClosed)
                        if (others != null) {
                            Spacer(Modifier.height(10.dp))
                            Note(R.drawable.carlink_ic_wifi, others)
                        }
                        Spacer(Modifier.height(10.dp))
                        Note(R.drawable.carlink_ic_lock, privacy)
                        Spacer(Modifier.height(10.dp))
                        Note(R.drawable.carlink_ic_info, stops)
                    }
                    FullLogQrUi.NoNetwork -> Note(R.drawable.carlink_ic_wifi_off, noNetwork)
                    FullLogQrUi.Failed -> Note(R.drawable.carlink_ic_warning, failed)
                }
                Spacer(Modifier.height(gap))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.targetGap), verticalArrangement = Arrangement.spacedBy(dimens.targetGap)) {
                    when (ui) {
                        FullLogQrUi.NoNetwork, FullLogQrUi.Failed -> {
                            IonButton(retry, onRetry, icon = R.drawable.carlink_ic_refresh)
                            GlassButton(report, onReport, icon = R.drawable.carlink_ic_qr_code_2)
                            GlassButton(copy, onCopyLog, icon = R.drawable.carlink_ic_content_copy)
                        }
                        else -> {
                            IonButton(done, onDone, icon = R.drawable.carlink_ic_check)
                            GlassButton(copy, onCopyLog, icon = R.drawable.carlink_ic_content_copy)
                        }
                    }
                }
            }
        }
    }
}

/** A running page for previews and the screenshot gallery: the car hotspot address and a second one on Wi-Fi. */
internal fun previewFullLogReady(codeEntryOpen: Boolean = true) = FullLogQrUi.Ready(
    FullLogOnPhone.Endpoint(
        url = "http://192.168.43.1:8888/b/3f9a1c7e5d2b8a40/",
        typedAddress = "192.168.43.1:8888",
        code = "482913",
        otherUrls = listOf("http://192.168.1.37:8888/b/3f9a1c7e5d2b8a40/"),
    ),
    codeEntryOpen,
)
