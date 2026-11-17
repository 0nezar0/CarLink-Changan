package com.carlink.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
