package com.carlink.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test fun offersSystemDefaultArabicAndEnglish() {
        assertEquals(listOf(AppLanguage.SYSTEM, "en", "ar"), AppLanguage.SUPPORTED)
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
    }

    @Test fun aChoiceInTheDevicesLanguageKeepsTheDevicesRegionAndNumbers() {
        val device = Locale.forLanguageTag("ar-SA-u-nu-latn")
        assertEquals(device, AppLanguage.resolve(AppLanguage.locale("ar"), device))
        assertEquals("latn", AppLanguage.resolve(AppLanguage.locale("ar"), device).getUnicodeLocaleType("nu"))
        assertEquals(device, AppLanguage.resolve(null, device))
        // Another language is the choice itself.
        assertEquals(Locale("en"), AppLanguage.resolve(AppLanguage.locale("en"), device))
    }
}
