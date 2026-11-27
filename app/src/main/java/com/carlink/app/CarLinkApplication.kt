package com.carlink.app

import android.util.Log
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.HostIntegration
import com.carlink.app.entry.HomeLauncher
import com.carlink.app.ui.HomeActivity
import com.carlink.app.ui.home.CarPlayWaitingContent
import com.carlink.core.connection.CarLinkConfig
import com.carlink.core.connection.CarLinkCore
import com.carlink.core.settings.AppLanguage
import com.shilapi.xcertplay.CarPlayHostIntegration

/**
 * One Application class for every process of the APK. It must be DiAuto's [App] because the
 * Android Auto engine casts the application context to it in ~100 places; the engine's own
 * startup only runs in the ":aa" process.
 */
class CarLinkApplication : App() {

    private val isAndroidAutoProcess: Boolean by lazy { CarLinkCore.isAndroidAutoProcess(this) }

    override fun initializesAndroidAutoEngine(): Boolean = isAndroidAutoProcess

    override fun onCreate() {
        // Settings › General › Language, in this process's resources before any text is read.
        installLanguage()
        // Changan CS75 Plus build: CarPlay's Efficient video (HEVC) starts on. A stored choice wins,
        // and without a hardware HEVC decoder the session asks for H.264 (DiPlay HevcSupport; under
        // Automatic picture PicturePolicy also stores it off).
        com.shilapi.xcertplay.AirPlayPersistence.hevcDefault = true
        // Before App.onCreate: it creates the engine's notification channels from these names.
        brandEngines()
        super.onCreate()
        // The connection backend (:carlink-core): engine hooks, USB watching, the session log.
        CarLinkCore.install(this, CarLinkConfig(
            accessoryName = getString(R.string.app_name),
            host = HomeLauncher,
            waitingView = CarPlayWaitingContent::create,
        ))
        if (isAndroidAutoProcess) {
            // Tells the main process's floating switcher when the Android Auto picture is in front.
            com.carlink.app.switcher.ProjectionVisibility.installInEngineProcess(this)
        } else {
            // v1.6.5: once, dark theme and Bluetooth audio if those old defaults were never
            // changed. Then drop leftover Top margin keys so an earlier on-value cannot apply
            // a 48 px inset after that setting was removed.
            com.carlink.app.ui.DarkThemeDefaultMigration.apply(this)
            com.carlink.core.settings.BluetoothAudioDefaultMigration.apply(this)
            com.carlink.core.settings.UnifiedSettingsStore(this).dropRemovedMarginPrefs()
            // Settings > Display > Screen type reaches detection before any session (CarPlay's turning canvas).
            com.carlink.app.ui.applyScreenType(com.carlink.app.ui.UiPreferences(this).uiProfileMode)
            // The floating switcher over CarPlay / Android Auto (and the USB switch prompt).
            com.carlink.app.switcher.SwitcherController.install(this)
        }
    }

    /**
     * The driver sees CarLink, never the engines' names: notification titles and channels, media
     * sessions, and the screens the engines open on their own (notification taps, boot launches,
     * Wi-Fi Direct returns, the projection's home button). Engine toasts are logged and dropped:
     * CarLink's Home shows the connection status itself. A toast the engine forces past its own
     * "toasts off" setting (a dead end with no other explanation) still shows.
     */
    private fun brandEngines() {
        val name = getString(R.string.app_name)
        HostIntegration.homeActivity = HomeActivity::class.java
        // CarLink: engine support QR/link must not open upstream DiAuto issues.
        HostIntegration.reportIssuesUrl = com.carlink.app.diagnostics.GitHubIssueReport.NEW_ISSUE
        HostIntegration.appName = name
        HostIntegration.serviceChannelName = getString(R.string.carlink_notification_channel_android_auto)
        HostIntegration.mediaSessionTag = "CarLink Android Auto"
        HostIntegration.mediaSnapshotFile = java.io.File(
            noBackupFilesDir, com.carlink.core.diagnostics.AaMediaSnapshotStore.FILE_NAME)
        HostIntegration.toastHandler = { _, text, forced ->
            Log.i(TOAST_TAG, "Android Auto engine toast forced=$forced: $text")
            !forced
        }
        CarPlayHostIntegration.homeActivity = HomeActivity::class.java
        CarPlayHostIntegration.appName = name
        CarPlayHostIntegration.mediaSessionTag = "CarLink CarPlay"
        CarPlayHostIntegration.toastHandler = { _, text ->
            Log.i(TOAST_TAG, "CarPlay engine toast: $text")
            true
        }
        // The car tile in Android Auto / CarPlay opens CarLink's Settings, as a car shows its own
        // screen; the phone session keeps running and comes back with the projection.
        val openCarSettings = {
            runCatching { startActivity(HomeActivity.carSettingsIntent(this)) }
                .onFailure { Log.w("CarLink", "Settings from the car tile could not open: $it") }
            Unit
        }
        HostIntegration.openCarScreen = openCarSettings
        // CarPlay's language button opens CarLink's Settings, where the one language choice lives.
        CarPlayHostIntegration.openLanguageSettings = { openCarSettings() }
        com.shilapi.xcertplay.orchestration.CarPlayController.openCarScreen = openCarSettings
        // The projections' own chrome (waiting screens, session menus, panels) in CarLink's look.
        com.carlink.app.engine.EngineChrome.install(this)
    }

    /**
     * CarLink's language for every screen of this process: CarLink's own (HomeActivity), CarPlay's
     * (DiPlay's activities ask [CarPlayHostIntegration.appLocale]) and Android Auto's in ":aa"
     * ([HostIntegration.appLocale]). The main process refreshes the copy all processes read.
     */
    private fun installLanguage() {
        if (!isAndroidAutoProcess) {
            runCatching {
                AppLanguage.mirror(this, com.carlink.core.settings.UnifiedSettingsStore(this).appLanguage(), platform = false)
            }
        }
        val chosen: (android.content.Context) -> java.util.Locale = { context ->
            AppLanguage.effectiveLocale(context).also { AppLanguage.applyToApplication(context, it) }
        }
        CarPlayHostIntegration.appLocale = chosen
        HostIntegration.appLocale = chosen
        AppLanguage.applyToApplication(this)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // A system change (night mode, the car's language) resets the application's locale.
        AppLanguage.applyToApplication(this)
    }

    private companion object {
        const val TOAST_TAG = "CarLink-EngineToast"
    }
}
