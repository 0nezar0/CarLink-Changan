package com.carlink.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test fun offersSystemDefaultFirstAndTheChosenLanguages() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.SUPPORTED.first())
        assertEquals(listOf("en", "ar", "ur", "hi", "fr", "tr", "es", "de", "ru", "zh-CN", "in", "pt-BR"), AppLanguage.SUPPORTED.drop(1))
    }

    @Test fun unknownOrMissingTagsFollowTheDevice() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.sanitize(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.sanitize("ja"))
        assertEquals("ur", AppLanguage.sanitize("ur"))
        assertNull(AppLanguage.locale(AppLanguage.SYSTEM))
    }

    @Test fun localesMatchTheResourceFolders() {
        assertEquals("zh", AppLanguage.locale("zh-CN")!!.language)
        assertEquals("CN", AppLanguage.locale("zh-CN")!!.country)
        assertEquals("BR", AppLanguage.locale("pt-BR")!!.country)
        // values-in: Android's legacy code for Indonesian.
        assertTrue(AppLanguage.locale("in")!!.language in setOf("in", "id"))
        assertEquals("ur", AppLanguage.locale("ur")!!.language)
    }

    @Test fun namesAreInTheirOwnScript() {
        assertEquals("العربية", AppLanguage.nativeName("ar"))
        assertEquals("اردو", AppLanguage.nativeName("ur"))
        assertEquals("हिन्दी", AppLanguage.nativeName("hi"))
        assertEquals("Türkçe", AppLanguage.nativeName("tr"))
        assertEquals("Русский", AppLanguage.nativeName("ru"))
        for (tag in AppLanguage.SUPPORTED.drop(1)) assertTrue(tag, AppLanguage.nativeName(tag) != tag)
    }

    @Test fun aChoiceInTheDevicesLanguageKeepsTheDevicesRegionAndNumbers() {
        val device = Locale.forLanguageTag("ar-SA-u-nu-latn")
        assertEquals(device, AppLanguage.resolve(AppLanguage.locale("ar"), device))
        assertEquals("latn", AppLanguage.resolve(AppLanguage.locale("ar"), device).getUnicodeLocaleType("nu"))
        assertEquals(device, AppLanguage.resolve(null, device))
        // Another language, or the same one in another named region, is the choice itself.
        assertEquals(Locale("en"), AppLanguage.resolve(AppLanguage.locale("en"), device))
        val portugal = Locale.forLanguageTag("pt-PT")
        assertEquals(Locale("pt", "BR"), AppLanguage.resolve(AppLanguage.locale("pt-BR"), portugal))
        val brazil = Locale.forLanguageTag("pt-BR-u-nu-latn")
        assertEquals(brazil, AppLanguage.resolve(AppLanguage.locale("pt-BR"), brazil))
        // Indonesian: the resources' "in" and a device's "id" are one language.
        val indonesia = Locale.forLanguageTag("id-ID")
        assertEquals(indonesia, AppLanguage.resolve(AppLanguage.locale("in"), indonesia))
    }
}
