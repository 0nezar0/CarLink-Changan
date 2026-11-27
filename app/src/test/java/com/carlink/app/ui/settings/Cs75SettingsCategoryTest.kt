package com.carlink.app.ui.settings

import com.carlink.core.settings.SettingsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Cs75SettingsCategoryTest {
    @Test fun brandSpecificCategoryFollowsGeneral() {
        val withCs75 = categoryOrder(includeCs75 = true)
        val without = categoryOrder(includeCs75 = false)
        assertEquals(SettingsCategory.CONNECTION, withCs75.first())
        assertEquals(SettingsCategory.GENERAL, withCs75[withCs75.lastIndex - 1])
        assertEquals(SettingsCategory.CHANGAN_CS75, withCs75.last())
        assertFalse(SettingsCategory.ADDED_FEATURES in withCs75)
        assertEquals(SettingsCategory.CONNECTION, without.first())
        assertEquals(SettingsCategory.GENERAL, without[without.lastIndex - 1])
        assertEquals(SettingsCategory.ADDED_FEATURES, without.last())
        assertFalse(SettingsCategory.CHANGAN_CS75 in without)
        assertEquals(withCs75.dropLast(1), without.dropLast(1))
    }

    @Test fun arabicAddedFeaturesTitleIsExact() {
        val xml = File("src/main/res/values-ar/carlink_strings_settings.xml").takeIf { it.isFile }
            ?: File("../src/main/res/values-ar/carlink_strings_settings.xml")
        val match = Regex("""<string name="carlink_settings_cat_added_features">([^<]+)</string>""").find(xml.readText())
            ?: error("missing carlink_settings_cat_added_features")
        assertEquals("مزايا إضافية", match.groupValues[1])
    }

    @Test fun arabicCs75TitlesIsolateTheLatinSegment() {
        val xml = File("src/main/res/values-ar/carlink_strings_settings.xml").takeIf { it.isFile }
            ?: File("../src/main/res/values-ar/carlink_strings_settings.xml")
        val text = xml.readText()
        // XML source uses the \u2068 escape; either the escape or the real isolate is fine.
        val fsi = "\\u2068"
        val pdi = "\\u2069"
        val isolated = fsi + "CS75 Plus 2023-2022" + pdi
        val isolatedChar = "⁨CS75 Plus 2023-2022⁩"
        for (name in listOf(
            "carlink_settings_group_changan",
            "carlink_settings_car_brand_cs75",
        )) {
            val match = Regex("""<string name="$name">([^<]+)</string>""").find(text)
                ?: error("missing $name")
            val value = match.groupValues[1]
            assertTrue(
                "$name must wrap the Latin model in FSI/PDI: $value",
                value.contains(isolated) || value.contains(isolatedChar),
            )
        }
    }

    @Test fun arabicFirstLaunchQuestionIsExact() {
        val xml = File("src/main/res/values-ar/carlink_strings_settings.xml").takeIf { it.isFile }
            ?: File("../src/main/res/values-ar/carlink_strings_settings.xml")
        val match = Regex("""<string name="carlink_cs75_first_launch_title">([^<]+)</string>""").find(xml.readText())
            ?: error("missing carlink_cs75_first_launch_title")
        // The Latin model is isolated (FSI/PDI) so the question mark stays on the Arabic side.
        assertEquals("هل سيارتك شانجان \\u2068CS75 Plus 2023\\u2069؟", match.groupValues[1])
    }

    @Test fun arabicExperimentalTagIsExact() {
        val xml = File("src/main/res/values-ar/carlink_strings_settings.xml").takeIf { it.isFile }
            ?: File("../src/main/res/values-ar/carlink_strings_settings.xml")
        val match = Regex("""<string name="carlink_settings_experimental_tag">([^<]+)</string>""").find(xml.readText())
            ?: error("missing carlink_settings_experimental_tag")
        assertEquals("تجريبي", match.groupValues[1])
    }
}
