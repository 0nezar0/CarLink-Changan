package com.carlink.app.switcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The HEVC quick toggle in the switcher panel is translated in every locale, and says it applies next connection. */
class SwitcherHevcStringsTest {
    private val res = File("src/main/res")
    private val keys = listOf(
        "carlink_switcher_hevc", "carlink_switcher_hevc_on", "carlink_switcher_hevc_off",
        "carlink_switcher_hevc_toast_on", "carlink_switcher_hevc_toast_off",
    )

    private fun values(dir: File): Map<String, String> {
        val text = File(dir, "carlink_strings_switcher.xml").readText()
        return Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private val locales get() = res.listFiles { f -> f.isDirectory && f.name.matches(Regex("values(-[a-z]{2}(-r[A-Z]{2})?)?")) }!!

    @Test fun everyLocaleHasTheFiveStrings() {
        assertEquals(11, locales.size)
        for (dir in locales) {
            val v = values(dir)
            for (key in keys) assertTrue("${dir.name}/$key", !v[key].isNullOrBlank())
            assertTrue("${dir.name} on/off differ", v["carlink_switcher_hevc_on"] != v["carlink_switcher_hevc_off"])
            assertTrue("${dir.name} toasts differ", v["carlink_switcher_hevc_toast_on"] != v["carlink_switcher_hevc_toast_off"])
            assertFalse(dir.name, v.values.any { "الأصلي" in it })
        }
    }

    @Test fun englishToastsSayTheChangeAppliesFromTheNextConnection() {
        val v = values(File(res, "values"))
        assertTrue("next connection" in v.getValue("carlink_switcher_hevc_toast_on"))
        assertTrue("next connection" in v.getValue("carlink_switcher_hevc_toast_off"))
    }
}
