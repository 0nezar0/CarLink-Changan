package com.carlink.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test fun offersSystemDefaultAndEveryTranslatedLanguage() {
        assertEquals(
            listOf(
                AppLanguage.SYSTEM, "ar", "en",
                "es", "pt", "fr", "it", "de", "ms", "tr", "zh-CN", "hu",
            ),
            AppLanguage.SUPPORTED,
        )
    }

    @Test fun unknownOrMissingTagsFollowTheDevice() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.sanitize(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.sanitize("ur"))
        assertEquals("ar", AppLanguage.sanitize("ar"))
        assertNull(AppLanguage.locale(AppLanguage.SYSTEM))
        assertEquals("ar", AppLanguage.locale("ar")!!.language)
    }

    @Test fun namesAreInTheirOwnScript() {
        assertEquals("العربية", AppLanguage.nativeName("ar"))
        assertEquals("English", AppLanguage.nativeName("en"))
        assertEquals("Español", AppLanguage.nativeName("es"))
        assertEquals("Português", AppLanguage.nativeName("pt"))
        assertEquals("Français", AppLanguage.nativeName("fr"))
        assertEquals("Italiano", AppLanguage.nativeName("it"))
        assertEquals("Deutsch", AppLanguage.nativeName("de"))
        assertEquals("Bahasa Melayu", AppLanguage.nativeName("ms"))
        assertEquals("Türkçe", AppLanguage.nativeName("tr"))
        assertEquals("简体中文", AppLanguage.nativeName("zh-CN"))
        assertEquals("Magyar", AppLanguage.nativeName("hu"))
    }

    @Test fun aChoiceInTheDevicesLanguageKeepsTheDevicesRegionAndNumbers() {
        val device = Locale.forLanguageTag("ar-SA-u-nu-latn")
        assertEquals(device, AppLanguage.resolve(AppLanguage.locale("ar"), device))
        assertEquals("latn", AppLanguage.resolve(AppLanguage.locale("ar"), device).getUnicodeLocaleType("nu"))
        assertEquals(device, AppLanguage.resolve(null, device))
        // Another language is the choice itself.
        assertEquals(Locale.forLanguageTag("en"), AppLanguage.resolve(AppLanguage.locale("en"), device))
    }

    @Test fun aRegionalChoiceKeepsADifferentScriptFromFollowingTheDevice() {
        val taiwan = Locale.forLanguageTag("zh-TW")
        assertEquals(Locale.forLanguageTag("zh-CN"), AppLanguage.resolve(AppLanguage.locale("zh-CN"), taiwan))
        val mainland = Locale.forLanguageTag("zh-CN-u-nu-latn")
        assertEquals(mainland, AppLanguage.resolve(AppLanguage.locale("zh-CN"), mainland))
        assertEquals("CN", AppLanguage.locale("zh-CN")!!.country)
        assertEquals("zh-CN", AppLanguage.sanitize("zh-CN"))
    }
}
