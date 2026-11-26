package com.carlink.core.settings

import android.content.Context
import com.shilapi.xcertplay.AirPlayPersistence

/**
 * The settings a driver sets once for both CarPlay and Android Auto (design/REBUILD_PLAN.md,
 * "Essentials"). Each value says what the driver wants; `connection/CarPlaySettings` and
 * `connection/aa/AaSessionSettings` translate it into each
 * engine's own storage. [Picture.CUSTOM] and [InterfaceSize.CUSTOM] mean an Advanced, per-engine
 * value was set and the unified choice must not overwrite it.
 */
data class UnifiedSettings(
    val connectionMethod: ConnectionMethod = ConnectionMethod.AUTOMATIC,
    val carHotspot: WifiDetails = WifiDetails(),
    val homeWifi: WifiDetails = WifiDetails(),
    val picture: Picture = Picture.AUTOMATIC,
    val interfaceSize: InterfaceSize = InterfaceSize.MEDIUM,
    val dayNight: DayNight = DayNight.AUTOMATIC,
    /** Minutes after midnight, for [DayNight.SCHEDULE]. */
    val nightStartMinute: Int = 19 * 60,
    val nightEndMinute: Int = 7 * 60,
    val rightHandDrive: Boolean = false,
    /** Where playback goes: through the car's Bluetooth (default) or through CarLink / USB. */
    val soundThroughCarBluetooth: Boolean = true,
    /** How far CarPlay music and video sound is buffered behind the picture ([AudioDelay]). */
    val audioDelay: AudioDelay = AudioDelay.NORMAL,
    val sendCarLocation: Boolean = true,
    val bydDirections: Boolean = true,
    val showAdvanced: Boolean = false,
    val labUnlocked: Boolean = false,
    /** Learned steering-wheel buttons ([WheelKeys]); applies from the next connection. */
    val wheelKeys: Map<WheelAction, Int> = emptyMap(),
    /** Sound levels in dB against the engines' own ([AudioLevels.PLAYBACK_DB]); from the next connection. */
    val mediaLevelDb: Int = 0,
    val navigationLevelDb: Int = 0,
    val callLevelDb: Int = 0,
    /** CarPlay calls play as media instead of as a phone call (GitHub #32); opt-in, may bring echo. */
    val callAudioAsMedia: Boolean = false,
    /** Press play once a session is back when the phone was playing music as the last one ended. */
    val resumeMusic: Boolean = false,
    /** CarPlay lowers its own music while a navigation prompt plays (GitHub #83). */
    val duckMusicForDirections: Boolean = true,
    /** CarPlay's Same Wi-Fi reaches the network over this screen's wired Ethernet, not its Wi-Fi (GitHub #87). */
    val sameWifiWired: Boolean = false,
    /** Software gain on the microphone sent to the phone ([AudioLevels.MIC_DB]). */
    val micGainDb: Int = 0,
    /**
     * Android Auto's size, percent of its Automatic density, under [InterfaceSize.CUSTOM] while the Advanced
     * DPI stays Automatic (set from the switcher's Custom size). CarPlay's custom size is DiPlay's own width.
     */
    val customSizePercent: Int = 100,
    /** The head unit's Back key goes back inside Android Auto (GitHub #72); off, it opens the session menu. */
    val androidAutoBack: Boolean = true,
    /**
     * [ConnectionMethod.HEADUNIT_SERVER] only: the phone's address typed by the driver, tried before
     * the gateway and the subnet scan. Empty: discovery only.
     */
    val headunitServerAddress: String = "",
    /**
     * Changan only: how the car's floating FceSystemUI status bar is hidden. Off by default;
     * the driver picks a mode in the Changan section at the top of Settings. [hideCarStatusBar]
     * is true when a hide mode is on (kept for backups of the earlier on/off switch).
     */
    val hideCarStatusBarMode: HideCarStatusBarMode = HideCarStatusBarMode.OFF,
    /** Packages ticked in [HideCarStatusBarMode.SELECTED_APPS]. CarLink itself is always included. */
    val hideCarStatusBarPackages: Set<String> = emptySet(),
    val hideCarStatusBar: Boolean = false,
    /** CarLink's language ([AppLanguage.SUPPORTED]); [AppLanguage.SYSTEM] follows the device. */
    val appLanguage: String = AppLanguage.SYSTEM,
    /** Lite mode for a weak CPU ([LiteMode]), off unless the driver turns it on: fewer frames, never a lower resolution; from the next connection. */
    val liteMode: LiteMode = LiteMode.OFF,
) {
}

data class WifiDetails(val ssid: String = "", val password: String = "")

/**
 * [HEADUNIT_SERVER] (Android Auto only, GitHub #70): CarLink dials the phone's own Android Auto head
 * unit server (developer settings, "Start head unit server", TCP 5277) over the Wi-Fi both are on,
 * the phone's hotspot or a shared router. CarPlay treats it as [AUTOMATIC].
 */
enum class ConnectionMethod { AUTOMATIC, SAME_WIFI, CAR_HOTSPOT, WIFI_DIRECT, HEADUNIT_SERVER }

/** Quality versus load; [CUSTOM] when Advanced picked exact per-engine values. */
enum class Picture { AUTOMATIC, BEST, BALANCED, LIGHT, CUSTOM }

/** [EXTRA_LARGE] for screens read from further away (GitHub #83); [CUSTOM] when Advanced set exact per-engine values. */
enum class InterfaceSize { SMALL, MEDIUM, LARGE, EXTRA_LARGE, CUSTOM }

enum class DayNight { AUTOMATIC, DAY, NIGHT, SCHEDULE }

/** CarPlay's music buffer: lower keeps sound in step with the picture, higher rides out Wi-Fi gaps. */
enum class AudioDelay { LOW, NORMAL, HIGH }

/** Main-process storage for [UnifiedSettings]. */
class UnifiedSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): UnifiedSettings {
        dropRemovedMarginPrefs()
        return read()
    }


    private fun read() = UnifiedSettings(
        connectionMethod = enumOr(prefs.getString("connection_method", null), ConnectionMethod.AUTOMATIC),
        carHotspot = WifiDetails(prefs.getString("car_hotspot_ssid", "")!!, prefs.getString("car_hotspot_password", "")!!),
        homeWifi = WifiDetails(prefs.getString("home_wifi_ssid", "")!!, prefs.getString("home_wifi_password", "")!!),
        picture = enumOr(prefs.getString("picture", null), Picture.AUTOMATIC),
        interfaceSize = enumOr(prefs.getString("interface_size", null), InterfaceSize.MEDIUM),
        dayNight = enumOr(prefs.getString("day_night", null), DayNight.AUTOMATIC),
        nightStartMinute = prefs.getInt("night_start", 19 * 60),
        nightEndMinute = prefs.getInt("night_end", 7 * 60),
        rightHandDrive = driveSide(prefs.getBoolean(KEY_DRIVE_SIDE_CHOSEN, false), prefs.getBoolean("right_hand_drive", false),
            if (prefs.contains(KEY_STEREO_RIGHT)) prefs.getBoolean(KEY_STEREO_RIGHT, false) else null),
        soundThroughCarBluetooth = prefs.getBoolean(KEY_SOUND_CAR_BLUETOOTH, true),
        // Until saved once, follow the Music buffer an earlier version set in DiPlay (500 ms or 1 s was High).
        audioDelay = enumOr(prefs.getString("audio_delay", null),
            if (AirPlayPersistence.loadMediaBufferMillis(appContext, wireless = true) >= 500) AudioDelay.HIGH else AudioDelay.NORMAL),
        sendCarLocation = prefs.getBoolean("send_location", true),
        bydDirections = prefs.getBoolean("byd_directions", true),
        showAdvanced = prefs.getBoolean("show_advanced", false),
        labUnlocked = prefs.getBoolean("lab_unlocked", false),
        wheelKeys = WheelKeys.decode(prefs.getString("wheel_keys", null)),
        mediaLevelDb = prefs.getInt("media_level_db", 0),
        navigationLevelDb = prefs.getInt("navigation_level_db", 0),
        callLevelDb = prefs.getInt("call_level_db", 0),
        callAudioAsMedia = prefs.getBoolean("call_audio_as_media", false),
        resumeMusic = prefs.getBoolean("resume_music", false),
        duckMusicForDirections = prefs.getBoolean("duck_music_directions", true),
        sameWifiWired = prefs.getBoolean("same_wifi_wired", false),
        micGainDb = prefs.getInt("mic_gain_db", 0),
        headunitServerAddress = prefs.getString("headunit_server_address", "")!!,
        hideCarStatusBarMode = hideCarStatusBarMode(),
        hideCarStatusBarPackages = hideCarStatusBarPackages(),
        hideCarStatusBar = hideCarStatusBarMode() != HideCarStatusBarMode.OFF,
        appLanguage = appLanguage(),
        liteMode = enumOr(prefs.getString("lite_mode", null), LiteMode.OFF),
        customSizePercent = prefs.getInt("custom_size_percent", 100),
        androidAutoBack = prefs.getBoolean("android_auto_back", true),
    )

    /** The stored language alone (cheap; read at startup to refresh [AppLanguage]'s copy). */
    fun appLanguage(): String = AppLanguage.sanitize(prefs.getString("app_language", AppLanguage.SYSTEM))

    private fun hideCarStatusBarMode(): HideCarStatusBarMode =
        enumOr(prefs.getString(KEY_HIDE_CAR_STATUS_BAR_MODE, null), HideCarStatusBarMode.OFF)

    private fun hideCarStatusBarPackages(): Set<String> {
        val raw = prefs.getString(KEY_HIDE_CAR_STATUS_BAR_PACKAGES, "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * Drops leftover Top margin keys from earlier versions. The setting was removed; an old
     * on-value must not apply a 48 px inset after an update. Safe to call repeatedly.
     */
    fun dropRemovedMarginPrefs() {
        if (REMOVED_MARGIN_KEYS.none { prefs.contains(it) }) return
        prefs.edit().also { edit -> REMOVED_MARGIN_KEYS.forEach { edit.remove(it) } }.apply()
    }

    fun save(settings: UnifiedSettings) {
        // The driving side counts as chosen once the driver changes it from what CarLink showed.
        val sideChosen = prefs.getBoolean(KEY_DRIVE_SIDE_CHOSEN, false) || settings.rightHandDrive != load().rightHandDrive
        prefs.edit()
            .putBoolean(KEY_DRIVE_SIDE_CHOSEN, sideChosen)
            .putString("connection_method", settings.connectionMethod.name)
            .putString("car_hotspot_ssid", settings.carHotspot.ssid)
            .putString("car_hotspot_password", settings.carHotspot.password)
            .putString("home_wifi_ssid", settings.homeWifi.ssid)
            .putString("home_wifi_password", settings.homeWifi.password)
            .putString("picture", settings.picture.name)
            .putString("interface_size", settings.interfaceSize.name)
            .putString("day_night", settings.dayNight.name)
            .putInt("night_start", settings.nightStartMinute)
            .putInt("night_end", settings.nightEndMinute)
            // Unchosen, the stored side stays as it was: writing the stereo's default would make it look chosen.
            .apply { if (sideChosen) putBoolean("right_hand_drive", settings.rightHandDrive) }
            .putBoolean(KEY_SOUND_CAR_BLUETOOTH, settings.soundThroughCarBluetooth)
            .putString("audio_delay", settings.audioDelay.name)
            .putBoolean("send_location", settings.sendCarLocation)
            .putBoolean("byd_directions", settings.bydDirections)
            .putBoolean("show_advanced", settings.showAdvanced)
            .putBoolean("lab_unlocked", settings.labUnlocked)
            .putString("wheel_keys", WheelKeys.encode(settings.wheelKeys))
            .putInt("media_level_db", settings.mediaLevelDb)
            .putInt("navigation_level_db", settings.navigationLevelDb)
            .putInt("call_level_db", settings.callLevelDb)
            .putBoolean("call_audio_as_media", settings.callAudioAsMedia)
            .putBoolean("resume_music", settings.resumeMusic)
            .putBoolean("duck_music_directions", settings.duckMusicForDirections)
            .putBoolean("same_wifi_wired", settings.sameWifiWired)
            .putInt("mic_gain_db", settings.micGainDb)
            .putString("headunit_server_address", settings.headunitServerAddress)
            .putString(KEY_HIDE_CAR_STATUS_BAR_MODE, settings.hideCarStatusBarMode.name)
            .putString(KEY_HIDE_CAR_STATUS_BAR_PACKAGES, settings.hideCarStatusBarPackages.sorted().joinToString(","))
            .putBoolean(KEY_HIDE_CAR_STATUS_BAR, settings.hideCarStatusBarMode != HideCarStatusBarMode.OFF)
            .also { edit -> REMOVED_MARGIN_KEYS.forEach { edit.remove(it) } }
            .putString("app_language", AppLanguage.sanitize(settings.appLanguage))
            .putString("lite_mode", settings.liteMode.name)
            .putInt("custom_size_percent", settings.customSizePercent)
            .putBoolean("android_auto_back", settings.androidAutoBack)
            .apply()
        AppLanguage.mirror(appContext, settings.appLanguage)
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    /** The driver side an FYT/Joying stereo reported (HeadUnitSignals); the default until the driver chooses. */
    fun saveStereoDriveSide(right: Boolean) {
        prefs.edit().putBoolean(KEY_STEREO_RIGHT, right).apply()
    }

    companion object {
        const val PREFS = "carlink_unified"
        const val KEY_SOUND_CAR_BLUETOOTH = "sound_car_bluetooth"
        const val KEY_HIDE_CAR_STATUS_BAR = "hide_car_status_bar"
        const val KEY_HIDE_CAR_STATUS_BAR_MODE = "hide_car_status_bar_mode"
        const val KEY_HIDE_CAR_STATUS_BAR_PACKAGES = "hide_car_status_bar_packages"
        /** Removed Top margin switch and earlier per-edge keys; dropped on load and save. */
        const val KEY_TOP_MARGIN = "top_margin"
        const val KEY_CARS_WITH_TOP_MARGIN = "cars_with_top_margin"
        const val KEY_CHANGAN_MARGINS = "changan_margins_adopted"
        val LEGACY_MARGIN_KEYS = listOf("", "cp_", "aa_").flatMap { prefix ->
            listOf("left", "top", "right", "bottom").map { "${prefix}margin_${it}_px" }
        } + KEY_CHANGAN_MARGINS
        val REMOVED_MARGIN_KEYS = (LEGACY_MARGIN_KEYS + KEY_TOP_MARGIN + KEY_CARS_WITH_TOP_MARGIN).distinct()
        private const val KEY_DRIVE_SIDE_CHOSEN = "drive_side_chosen"
        private const val KEY_STEREO_RIGHT = "stereo_right_hand_drive"

        /**
         * Right-hand drive: the driver's choice once made, or a stored right (before the marker existed,
         * right could only be the driver's choice), else the stereo's driver side, else left.
         */
        fun driveSide(chosen: Boolean, storedRight: Boolean, stereoRight: Boolean?): Boolean =
            if (chosen || storedRight) storedRight else stereoRight ?: false
    }
}
