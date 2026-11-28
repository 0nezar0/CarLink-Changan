package com.carlink.app.diagnostics

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullLogOnPhoneTest {
    private val texts = FullLogOnPhone.PageTexts(
        lang = "ar", rtl = true, title = "السجل <الكامل>", hint = "hint", copy = "نسخ", copied = "copied \"ok\"",
        copyFailed = "fail</script><script>alert(1)</script>", download = "dl", loading = "loading", loadFailed = "lf",
        codePrompt = "prompt", codeOpen = "open", codeWrong = "wrong {left}", codeLocked = "locked",
    )
    private val log = "CarLink full log\n2026-11-28 17:00:00.000  CP E  فشل الاتصال\n"
    private var session: FullLogOnPhone.Session? = null

    @After
    fun stop() {
        session?.stop()
    }

    private fun start(addresses: List<String> = listOf("127.0.0.1"), onIdle: () -> Unit = {}, idle: Int = FullLogOnPhone.IDLE_TIMEOUT_MILLIS):
        FullLogOnPhone.Session {
        val started = FullLogOnPhone.start(log, texts, addresses, onIdle = onIdle, idleTimeoutMillis = idle, typedPort = 0)
        assertTrue("$started", started is FullLogOnPhone.Start.Started)
        return (started as FullLogOnPhone.Start.Started).session.also { session = it }
    }

    private class Answer(val status: Int, val type: String?, val body: String)

    private fun request(url: String, method: String = "GET", body: String? = null): Answer {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        if (body != null) {
            val bytes = body.toByteArray()
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
        }
        val status = connection.responseCode
        val stream = if (status < 400) connection.inputStream else connection.errorStream
        val text = stream?.readBytes()?.toString(Charsets.UTF_8).orEmpty()
        val type = connection.contentType
        connection.disconnect()
        return Answer(status, type, text)
    }

    @Test
    fun noAddressMeansNoNetwork() {
        assertEquals(FullLogOnPhone.Start.NoNetwork, FullLogOnPhone.start(log, texts, emptyList(), typedPort = 0))
    }

    @Test
    fun endpointUsesTheBestAddressAndListsTheOthers() {
        val e = start(listOf("127.0.0.1", "10.0.0.9")).endpoint
        assertTrue(e.url, e.url.matches(Regex("http://127\\.0\\.0\\.1:\\d+/b/[0-9a-f]{16}/")))
        assertTrue(e.typedAddress, e.typedAddress.matches(Regex("127\\.0\\.0\\.1:\\d+")))
        assertTrue(e.code, e.code.matches(Regex("\\d{6}")))
        assertEquals(1, e.otherUrls.size)
        assertTrue(e.otherUrls[0].startsWith("http://10.0.0.9:"))
    }

    @Test
    fun servesTheLogAsUtf8PlainText() {
        val e = start().endpoint
        val page = request(e.url)
        assertEquals(200, page.status)
        assertTrue(page.body.contains("السجل &lt;الكامل&gt;"))
        assertTrue(page.body.contains("dir=\"rtl\""))
        val report = request(e.url + "report")
        assertEquals(200, report.status)
        assertEquals("text/plain; charset=utf-8", report.type)
        assertEquals(log, report.body)
    }

    @Test
    fun aWrongTokenGetsNothing() {
        val e = start().endpoint
        val wrong = e.url.replace(Regex("/b/[0-9a-f]{16}/"), "/b/0000000000000000/") + "report"
        assertEquals(404, request(wrong).status)
        // The settings routes of DiPlay's server give nothing away.
        assertEquals("{}", request(e.url + "export").body)
    }

    @Test
    fun theTypedCodeLeadsToTheLogPage() {
        val e = start().endpoint
        val root = "http://${e.typedAddress}/"
        val codePage = request(root)
        assertEquals(200, codePage.status)
        assertTrue(codePage.body.contains("prompt"))
        val wrongCode = if (e.code == "000000") "111111" else "000000"
        val wrong = request(root + "code", "POST", wrongCode)
        assertEquals(403, wrong.status)
        val right = request(root + "code", "POST", FullLogOnPhone.spacedCode(e.code))
        assertEquals(200, right.status)
        val path = Regex("\"path\":\"([^\"]+)\"").find(right.body)!!.groupValues[1]
        assertTrue(e.url.endsWith(path))
    }

    @Test
    fun stopEndsThePage() {
        val s = start()
        assertEquals(200, request(s.endpoint.url + "report").status)
        s.stop()
        val after = runCatching { request(s.endpoint.url + "report").status }.getOrNull()
        assertTrue("still answered: $after", after == null || after == 404)
        s.stop() // twice is fine
    }

    @Test
    fun stopsByItselfWhenIdle() {
        val idle = CountDownLatch(1)
        start(onIdle = { idle.countDown() }, idle = 300)
        assertTrue(idle.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun pageTextsCannotBreakOutOfTheScript() {
        val page = String(FullLogOnPhone.pageHtml(texts), Charsets.UTF_8)
        assertFalse(page.contains("<script>alert(1)"))
        assertTrue(page.contains("\\u003c/script\\u003e"))
        assertEquals("\"a\\\"b\\\\c\\n\\u003c\"", FullLogOnPhone.js("a\"b\\c\n<"))
        assertEquals("&lt;b&gt; &amp; &quot;x&quot; &#39;", FullLogOnPhone.html("<b> & \"x\" '"))
    }

    @Test
    fun codeIsSpacedInThrees() {
        assertEquals("482 913", FullLogOnPhone.spacedCode("482913"))
        // On screen: left-to-right marks keep the groups in order inside Arabic text.
        assertEquals("\u200E482\u200E\u00A0913", com.carlink.app.ui.diagnostics.codeForDisplay("482913"))
    }

    /** What both Copy full log and the phone page get: saved passwords and addresses hidden. */
    @Test
    fun theServedLogIsCleaned() {
        val raw = "Same Wi-Fi joined, key Sup3rSecret!, peer aa:bb:cc:dd:ee:ff, password=hunter22 Ayush's S25 Ultra"
        val clean = GitHubIssueReport.clean(raw, listOf("Sup3rSecret!", ""))
        assertFalse(clean, clean.contains("Sup3rSecret!"))
        assertFalse(clean, clean.contains("hunter22"))
        assertFalse(clean, clean.contains("aa:bb:cc:dd:ee:ff"))
        assertFalse(clean, clean.contains("Ayush"))
        assertTrue(clean, clean.contains("S25 Ultra"))
    }
}
