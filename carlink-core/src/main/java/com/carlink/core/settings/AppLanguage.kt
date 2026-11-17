package com.carlink.core.settings

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.io.File
import java.util.Locale

/**
 * CarLink's own language choice (Settings › General › Language), for every screen of the APK:
 * CarLink's, CarPlay's (main process) and Android Auto's (":aa" process).
 *
 * Android 13+ has per-app languages: the choice goes to the platform, which restarts the screens.
 * Older Android (the head units) has none, so each screen wraps its context in the chosen locale
 * and the application's resources follow it ([applyToApplication]). [UnifiedSettings.appLanguage]
 * is the stored choice; [mirror] copies it to a small file every process can read, because
 * [UnifiedSettingsStore] is main-process storage and SharedPreferences are cached per process.
 */
object AppLanguage {
    /** Follow the device; a device language CarLink has no text for shows English. */
    const val SYSTEM = ""

    /** The languages offered, as stored (BCP 47). Keep in step with localeFilters and xml/locales_config. */
    val SUPPORTED = listOf(SYSTEM, "en", "ar", "ur", "hi", "fr", "tr", "es", "de", "ru", "zh-CN", "in", "pt-BR")

    /** Each language's name in its own script; [SYSTEM] has a translated label in the app. */
    fun nativeName(tag: String): String = when (tag) {
        "en" -> "English"
        "ar" -> "العربية"
        "ur" -> "اردو"
        "hi" -> "हिन्दी"
        "fr" -> "Français"
        "tr" -> "Türkçe"
        "es" -> "Español"
        "de" -> "Deutsch"
        "ru" -> "Русский"
        "zh-CN" -> "中文（简体）"
        "in" -> "Bahasa Indonesia"
        "pt-BR" -> "Português (Brasil)"
        else -> tag
    }

    fun sanitize(tag: String?): String = tag?.takeIf { it in SUPPORTED } ?: SYSTEM

    /** The locale for [tag]; null for [SYSTEM]. */
    fun locale(tag: String): Locale? = when (tag) {
        SYSTEM -> null
        "zh-CN" -> Locale.SIMPLIFIED_CHINESE
        "pt-BR" -> Locale("pt", "BR")
        // Android's resources use the legacy code for Indonesian (values-in).
        "in" -> Locale("in")
        else -> Locale(tag)
    }

    /** The device's own language, whatever an earlier choice did to this process. */
    fun systemLocale(): Locale = Resources.getSystem().configuration.locales[0]

    private fun file(context: Context) = File(context.applicationContext.noBackupFilesDir, "carlink_language")

    /** The chosen language as every process sees it. */
    fun current(context: Context): String =
        sanitize(runCatching { file(context).takeIf { it.isFile }?.readText()?.trim() }.getOrNull())

    /** The locale screens should use before Android 13: the choice, or the device's for [SYSTEM]. */
    fun effectiveLocale(context: Context): Locale = resolve(locale(current(context)), systemLocale())

    /**
     * [chosen], or the device's own [system] locale when it is the same language (and region, when
     * the choice names one): "ar" on an ar-SA-u-nu-latn device keeps its region and number style.
     */
    fun resolve(chosen: Locale?, system: Locale): Locale {
        if (chosen == null) return system
        val sameLanguage = baseLanguage(chosen.language) == baseLanguage(system.language)
        val sameRegion = chosen.country.isEmpty() || chosen.country.equals(system.country, ignoreCase = true)
        return if (sameLanguage && sameRegion) system else chosen
    }

    /** Indonesian is "in" in Android's resources and "id" in newer locales; Hebrew and Yiddish alike. */
    private fun baseLanguage(language: String): String = when (language.lowercase(Locale.ROOT)) {
        "in" -> "id"
        "iw" -> "he"
        "ji" -> "yi"
        else -> language.lowercase(Locale.ROOT)
    }

    /** Before Android 13 the app supplies the language; from 13 the platform does. */
    val appSupplied: Boolean get() = Build.VERSION.SDK_INT < 33

    /**
     * Stores [tag] for all processes; on Android 13+ also hands it to the platform ([platform]
     * false at startup, so a language picked in Android's own app settings stays).
     */
    fun mirror(context: Context, tag: String, platform: Boolean = true) {
        val clean = sanitize(tag)
        val changed = current(context) != clean
        if (changed || !file(context).isFile) {
            runCatching { file(context).writeText(clean) }
        }
        if (platform && changed && Build.VERSION.SDK_INT >= 33) {
            runCatching {
                val manager = context.getSystemService(android.app.LocaleManager::class.java)
                val wanted = locale(clean)?.let { LocaleList(it) } ?: LocaleList.getEmptyLocaleList()
                if (manager.applicationLocales != wanted) manager.applicationLocales = wanted
            }
        }
    }

    /**
     * For an activity's attachBaseContext: [base] with the chosen locale and its layout direction
     * (right to left for Arabic and Urdu). Unchanged on Android 13+.
     */
    fun wrap(base: Context): Context {
        if (!appSupplied) return base
        val locale = effectiveLocale(base)
        applyToApplication(base, locale)
        if (base.resources.configuration.locales[0] == locale) return base
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }

    /**
     * A configuration that only sets the locale, for [Activity.applyOverrideConfiguration]: unlike
     * [wrap], night mode and size changes still reach the activity. Null on Android 13+.
     */
    fun overrideConfiguration(base: Context): Configuration? {
        if (!appSupplied) return null
        val locale = effectiveLocale(base)
        applyToApplication(base, locale)
        return Configuration().apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }

    /**
     * The application's resources (notifications, toasts, engine texts read through the app
     * context) in [locale]; also Locale.getDefault for formatting.
     */
    @Suppress("DEPRECATION")
    fun applyToApplication(context: Context, locale: Locale = effectiveLocale(context)) {
        if (!appSupplied) return
        runCatching {
            if (Locale.getDefault() != locale) Locale.setDefault(locale)
            val resources = context.applicationContext.resources
            if (resources.configuration.locales[0] != locale) {
                val configuration = Configuration(resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                resources.updateConfiguration(configuration, resources.displayMetrics)
            }
        }
    }
}
