package com.carlink.app.diagnostics

import com.shilapi.xcertplay.backup.SettingsBackupServer
import com.shilapi.xcertplay.backup.SettingsImportOutcome

/**
 * Diagnostics › QR for the full log (v1.7.11): the whole connection log ("Copy full log") on a phone
 * that shares a network with the stereo (car hotspot, the same Wi-Fi or an iPhone hotspot), no
 * internet needed. One QR code holds about 2.9 KB and the full log is up to ~0.6 MB, so the code
 * carries a link to a page this stereo serves itself.
 *
 * The server is DiPlay's [SettingsBackupServer] (vendor/diplay, unchanged): a random 64-bit token in
 * the link is the only key (no typed code: the page is started without a code page, so "/" and
 * "/code" answer 404), and it stops by itself after [IDLE_TIMEOUT_MILLIS] without a request. Typing
 * the link written under the QR code opens the log directly. Its settings export/import routes are
 * not used: export answers "{}" and import refuses everything.
 *
 * The log is exported and passed through [GitHubIssueReport.clean] (saved Wi-Fi passwords, owners'
 * names, serial numbers, addresses) once, before the server starts; the page shows that text.
 */
object FullLogOnPhone {
    /** Ten minutes without a request, as DiPlay's own phone page. */
    const val IDLE_TIMEOUT_MILLIS = SettingsBackupServer.IDLE_TIMEOUT_MILLIS

    /** What the sheet shows: the link in the QR code (also typed as is) and the same link on other addresses. */
    data class Endpoint(val url: String, val otherUrls: List<String> = emptyList())

    /** The page's words, in the stereo's language. */
    data class PageTexts(
        val lang: String,
        val rtl: Boolean,
        val title: String,
        val hint: String,
        val copy: String,
        val copied: String,
        val copyFailed: String,
        val download: String,
        val loading: String,
        val loadFailed: String,
    )

    sealed interface Start {
        data class Started(val session: Session) : Start
        /** No IPv4 address on Wi-Fi, a hotspot or Wi-Fi Direct: the phone cannot reach this stereo. */
        data object NoNetwork : Start
        data object Failed : Start
    }

    /** A running page; [stop] is safe to call more than once. */
    class Session internal constructor(private val server: SettingsBackupServer, val endpoint: Endpoint) {
        fun stop() = server.stop()
    }

    /**
     * Starts the page for [logText] (already cleaned) on [addresses] (best first, as
     * [SettingsBackupServer.addressCandidates]). [onIdle] runs on a server thread.
     */
    fun start(
        logText: String,
        texts: PageTexts,
        addresses: List<String>,
        onIdle: () -> Unit = {},
        idleTimeoutMillis: Int = IDLE_TIMEOUT_MILLIS,
    ): Start {
        if (addresses.isEmpty()) return Start.NoNetwork
        val report = logText.toByteArray(Charsets.UTF_8)
        val server = SettingsBackupServer(
            page = pageHtml(texts),
            export = { "{}".toByteArray(Charsets.UTF_8) },
            import = { SettingsImportOutcome(0, 0, invalid = true) },
            report = { report },
            // No code page: the token in the link is the only way in.
            codePage = null,
        )
        server.idleTimeoutMillis = idleTimeoutMillis
        server.onIdle = onIdle
        val started = runCatching { server.start() }.getOrNull() ?: run { runCatching { server.stop() }; return Start.Failed }
        return Start.Started(Session(server, Endpoint(started.url(addresses.first()), addresses.drop(1).map { started.url(it) })))
    }

    /** Escapes text for HTML element content and double-quoted attributes. */
    fun html(text: String): String = buildString(text.length) {
        for (c in text) when (c) {
            '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;"); '"' -> append("&quot;"); '\'' -> append("&#39;")
            else -> append(c)
        }
    }

    /** A JavaScript string literal (double-quoted) that is also safe inside a <script> element. */
    fun js(text: String): String = buildString(text.length + 2) {
        append('"')
        for (c in text) when (c) {
            '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n"); '\r' -> append("\\r")
            '<' -> append("\\u003c"); '>' -> append("\\u003e"); '&' -> append("\\u0026")
            '\u2028' -> append("\\u2028"); '\u2029' -> append("\\u2029")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }

    private const val STYLE = """
:root { color-scheme: light dark; }
body { margin:0; padding:20px 16px 40px; background:#101418; color:#e8eaed;
  font:16px/1.5 -apple-system,"Segoe UI",Roboto,"Noto Sans","Noto Sans Arabic",sans-serif; }
main { max-width:900px; margin:0 auto; }
h1 { font-size:22px; margin:0 0 6px; }
p { color:#9aa0a6; font-size:14px; margin:0 0 14px; }
.row { display:flex; gap:10px; flex-wrap:wrap; margin-bottom:12px; }
button { flex:1 1 140px; padding:13px 16px; border-radius:999px; border:0; font-size:16px; font-weight:600; cursor:pointer;
  background:#a8c7fa; color:#0b2540; }
button.alt { background:#2a323d; color:#e8eaed; }
button:disabled { opacity:.5; }
textarea { width:100%; box-sizing:border-box; height:60vh; padding:12px; border-radius:12px; border:1px solid #2a323d;
  background:#0b0f13; color:#e8eaed; font:12px/1.4 ui-monospace,Menlo,Consolas,monospace; direction:ltr; text-align:left; }
#status { min-height:20px; font-size:14px; color:#81c995; margin-bottom:10px; }
#status.bad { color:#f28b82; }
"""

    /** The page the QR link opens: the log in a text box, Copy and Download. */
    fun pageHtml(t: PageTexts): ByteArray = """<!DOCTYPE html>
<html lang="${html(t.lang)}" dir="${if (t.rtl) "rtl" else "ltr"}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>${html(t.title)}</title>
<style>$STYLE</style>
</head>
<body>
<main>
  <h1>${html(t.title)}</h1>
  <p>${html(t.hint)}</p>
  <div class="row">
    <button id="copy" disabled>${html(t.copy)}</button>
    <button id="download" class="alt" disabled>${html(t.download)}</button>
  </div>
  <div id="status">${html(t.loading)}</div>
  <textarea id="log" spellcheck="false" autocomplete="off"></textarea>
</main>
<script>
var T = {copied:${js(t.copied)}, copyFailed:${js(t.copyFailed)}, loadFailed:${js(t.loadFailed)}};
var $ = function(id) { return document.getElementById(id); };
var text = "";
function status(message, bad) { $("status").textContent = message; $("status").className = bad ? "bad" : ""; }
function fallbackCopy() {
  var box = $("log");
  box.focus(); box.select(); box.setSelectionRange(0, box.value.length);
  var ok = false;
  try { ok = document.execCommand("copy"); } catch (e) { ok = false; }
  status(ok ? T.copied : T.copyFailed, !ok);
}
$("copy").addEventListener("click", function() {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(function() { status(T.copied, false); }, fallbackCopy);
  } else fallbackCopy();
});
$("download").addEventListener("click", function() {
  var stamp = new Date().toISOString().slice(0, 19).replace(/[-:T]/g, "");
  var link = document.createElement("a");
  link.href = URL.createObjectURL(new Blob([text], {type: "text/plain;charset=utf-8"}));
  link.download = "carlink-full-log-" + stamp + ".txt";
  document.body.appendChild(link); link.click();
  setTimeout(function() { URL.revokeObjectURL(link.href); link.remove(); }, 1000);
});
fetch("report", {cache: "no-store"}).then(function(r) {
  if (!r.ok) throw new Error(r.status);
  return r.text();
}).then(function(body) {
  text = body; $("log").value = body; status("", false);
  $("copy").disabled = false; $("download").disabled = false;
}).catch(function() { status(T.loadFailed, true); });
</script>
</body>
</html>
""".toByteArray(Charsets.UTF_8)
}
