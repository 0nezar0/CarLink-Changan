package com.carlink.app.ui.settings

import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carlink.app.R
import com.carlink.core.settings.AdvancedSetting
import com.carlink.core.settings.Applies
import com.carlink.core.settings.ConnectionMethod
import com.carlink.core.settings.Control
import com.carlink.core.settings.DayNight
import com.carlink.core.settings.SettingsCategory
import com.carlink.core.settings.Tier
import com.carlink.app.ui.Route
import com.carlink.app.ui.premium.GlassCard
import com.carlink.app.ui.premium.IonButton
import com.carlink.app.ui.theme.CarLinkTokens

/** How many taps on Version unlock Lab. */
internal const val LAB_TAPS = 7

/**
 * One category: its Essentials, then (with Show advanced) its engine settings grouped by
 * engine, then (once unlocked) its Lab settings in an amber frame.
 */
@Composable
internal fun CategoryPage(
    category: SettingsCategory,
    ui: SettingsUi,
    actions: SettingsActions,
    openSheet: (String) -> Unit,
    versionTaps: Int,
    onVersionTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CarLinkTokens.dimens.gutter)) {
        when (category) {
            SettingsCategory.CONNECTION -> ConnectionEssentials(ui, actions, openSheet)
            SettingsCategory.DISPLAY -> DisplayEssentials(ui, actions, openSheet)
            SettingsCategory.SOUND -> SoundEssentials(ui, actions, openSheet)
            SettingsCategory.NAVIGATION -> NavigationEssentials(ui, actions)
            SettingsCategory.CAR -> CarEssentials(ui, openSheet)
            SettingsCategory.GENERAL -> GeneralEssentials(ui, actions, openSheet, versionTaps, onVersionTap)
        }
        if (ui.unified.showAdvanced) EngineSection(category, Tier.ADVANCED, ui, actions, openSheet)
        if (ui.unified.labUnlocked) EngineSection(category, Tier.LAB, ui, actions, openSheet)
    }
}

// ---- Essentials -----------------------------------------------------------------------------

@Composable
private fun ConnectionEssentials(ui: SettingsUi, actions: SettingsActions, openSheet: (String) -> Unit) {
    val c = ui.connection
    val method = ui.unified.connectionMethod
    if (c.autoConnectPaused) PausedCard(actions::resumeAutoConnect)
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_start)) }) {
        SwitchRow(stringResource(R.string.carlink_settings_auto_bluetooth), c.autoConnectBluetooth, actions::setAutoConnectBluetooth,
            description = stringResource(R.string.carlink_settings_auto_bluetooth_detail), icon = R.drawable.carlink_ic_bluetooth)
        GroupDivider()
        SwitchRow(stringResource(R.string.carlink_settings_auto_usb), c.autoConnectUsb, actions::setAutoConnectUsb,
            description = stringResource(R.string.carlink_settings_auto_usb_detail), icon = R.drawable.carlink_ic_usb)
        GroupDivider()
        SwitchRow(stringResource(R.string.carlink_settings_start_boot), c.startOnBoot, actions::setStartOnBoot,
            description = stringResource(R.string.carlink_settings_start_boot_detail), icon = R.drawable.carlink_ic_power_settings_new)
    }
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_wireless)) }) {
        ChoiceRow(stringResource(R.string.carlink_settings_method), connectionMethodLabel(method),
            { openSheet(SheetKey.of(EssentialSheet.CONNECTION_METHOD)) },
            description = stringResource(R.string.carlink_settings_method_detail), icon = methodIcon(method))
        // Network details only where the chosen method can use them.
        if (method == ConnectionMethod.AUTOMATIC || method == ConnectionMethod.CAR_HOTSPOT) {
            GroupDivider()
            ChoiceRow(stringResource(R.string.carlink_settings_hotspot),
                ui.unified.carHotspot.ssid.ifEmpty { stringResource(R.string.carlink_settings_not_set) },
                { openSheet(SheetKey.of(EssentialSheet.CAR_HOTSPOT)) },
                description = stringResource(R.string.carlink_settings_hotspot_detail), icon = R.drawable.carlink_ic_wifi_tethering)
        }
        if (method == ConnectionMethod.AUTOMATIC || method == ConnectionMethod.SAME_WIFI) {
            GroupDivider()
            ChoiceRow(stringResource(R.string.carlink_settings_same_wifi),
                ui.unified.homeWifi.ssid.ifEmpty { stringResource(R.string.carlink_settings_not_set) },
                { openSheet(SheetKey.of(EssentialSheet.SAME_WIFI)) },
                description = stringResource(R.string.carlink_settings_same_wifi_detail), icon = R.drawable.carlink_ic_wifi)
        }
        if (method == ConnectionMethod.HEADUNIT_SERVER) {
            GroupDivider()
            ChoiceRow(stringResource(R.string.carlink_settings_headunit_server_address),
                ui.unified.headunitServerAddress.ifEmpty { stringResource(R.string.carlink_settings_headunit_server_address_auto) },
                { openSheet(SheetKey.of(EssentialSheet.HEADUNIT_SERVER_ADDRESS)) },
                description = stringResource(R.string.carlink_settings_headunit_server_address_detail), icon = R.drawable.carlink_ic_android)
        }
        // Advanced, and only where the probe found an external Bluetooth module.
        if (ui.unified.showAdvanced && ui.stereoBluetoothExternal) {
            GroupDivider()
            val detail = stringResource(R.string.carlink_settings_external_bt_detail)
            SwitchRow(stringResource(R.string.carlink_settings_external_bt), c.wirelessOnExternalBluetooth,
                actions::setWirelessOnExternalBluetooth,
                description = if (ui.stereoBluetoothFyt) detail + "\n" + stringResource(R.string.carlink_settings_external_bt_fyt_note) else detail,
                icon = R.drawable.carlink_ic_bluetooth)
        }
        if (ui.unified.showAdvanced) {
            GroupDivider()
            SwitchRow(stringResource(R.string.carlink_settings_root), c.rootMode, actions::setRootMode,
                description = stringResource(R.string.carlink_settings_root_detail), icon = R.drawable.carlink_ic_lock)
            GroupDivider()
            SwitchRow(stringResource(R.string.carlink_settings_auto_hotspot), c.autoCarHotspot, actions::setAutoCarHotspot,
                description = stringResource(R.string.carlink_settings_auto_hotspot_detail), icon = R.drawable.carlink_ic_wifi)
        }
        // Advanced, and only where Android Auto itself is installed (Self Mode dials its head unit server).
        if (ui.unified.showAdvanced && c.androidAutoInstalled) {
            GroupDivider()
            SwitchRow(stringResource(R.string.carlink_settings_aa_on_screen), c.androidAutoOnScreen && c.androidAutoCanRunCar,
                actions::setAndroidAutoOnScreen, enabled = c.androidAutoCanRunCar,
                description = stringResource(if (c.androidAutoCanRunCar) R.string.carlink_settings_aa_on_screen_detail
                    else R.string.carlink_settings_aa_on_screen_unsupported), icon = R.drawable.carlink_ic_android)
        }
    }
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_phones)) }) {
        LinkRow(stringResource(R.string.carlink_settings_bluetooth), actions::openBluetoothSettings,
            stringResource(R.string.carlink_settings_bluetooth_detail), R.drawable.carlink_ic_bluetooth_searching, external = true)
        GroupDivider()
        LinkRow(stringResource(R.string.carlink_settings_permissions), actions::openAppSettings,
            stringResource(R.string.carlink_settings_permissions_detail), R.drawable.carlink_ic_lock, external = true)
        if (ui.phones.isNotEmpty()) {
            var repairing by rememberSaveable { mutableStateOf(false) }
            GroupDivider()
            LinkRow(stringResource(R.string.carlink_repair_action), { repairing = true },
                stringResource(R.string.carlink_repair_settings_detail), R.drawable.carlink_ic_bluetooth_searching)
            if (repairing) com.carlink.app.ui.home.ForgetRepairDialog(ui.phones, null,
                { repairing = false; actions.forgetAndRepair(it) }, { repairing = false })
        }
    }
}

/** Shown while the driver's disconnect keeps automatic starts paused. */
@Composable
private fun PausedCard(onResume: () -> Unit) {
    val colors = CarLinkTokens.colors
    GlassCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(rowPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(if (isCar) 14.dp else 10.dp).clip(CircleShape).background(colors.warning))
            Spacer(Modifier.width(if (isCar) 20.dp else 14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.carlink_settings_paused_title), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.carlink_settings_paused_body), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(CarLinkTokens.dimens.gutter))
            IonButton(stringResource(R.string.carlink_settings_resume), onResume, icon = R.drawable.carlink_ic_play_arrow_filled)
        }
    }
}

@Composable
private fun DisplayEssentials(ui: SettingsUi, actions: SettingsActions, openSheet: (String) -> Unit) {
    val u = ui.unified
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_projection)) }) {
        ChoiceRow(stringResource(R.string.carlink_settings_picture), pictureLabel(u.picture), { openSheet(SheetKey.of(EssentialSheet.PICTURE)) },
            description = stringResource(R.string.carlink_settings_picture_detail), icon = R.drawable.carlink_ic_aspect_ratio)
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_interface_size), interfaceSizeLabel(u.interfaceSize),
            { openSheet(SheetKey.of(EssentialSheet.INTERFACE_SIZE)) },
            description = stringResource(R.string.carlink_settings_interface_size_detail), icon = R.drawable.carlink_ic_text_fields)
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_daynight), dayNightLabel(u.dayNight), { openSheet(SheetKey.of(EssentialSheet.DAY_NIGHT)) },
            description = stringResource(R.string.carlink_settings_daynight_detail), icon = dayNightIcon(u.dayNight))
        if (u.dayNight == DayNight.SCHEDULE) {
            GroupDivider()
            ChoiceRow(stringResource(R.string.carlink_settings_night_start), clockText(u.nightStartMinute),
                { openSheet(SheetKey.of(EssentialSheet.NIGHT_START)) }, icon = R.drawable.carlink_ic_dark_mode)
            GroupDivider()
            ChoiceRow(stringResource(R.string.carlink_settings_night_end), clockText(u.nightEndMinute),
                { openSheet(SheetKey.of(EssentialSheet.NIGHT_END)) }, icon = R.drawable.carlink_ic_light_mode)
        }
    }
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_margins)) }) {
        ChoiceRow(stringResource(R.string.carlink_settings_margin_app), marginLabel(u.marginTopPx),
            { openSheet(SheetKey.of(EssentialSheet.MARGIN_TOP)) },
            description = stringResource(R.string.carlink_settings_margin_app_detail),
            icon = R.drawable.carlink_ic_aspect_ratio)
    }
    // Each projection keeps its own set: CarPlay and Android Auto draw their bars differently.
    ProjectionMargins(R.string.carlink_settings_group_margins_carplay, carPlay = true, u.carPlayMargins, openSheet)
    ProjectionMargins(R.string.carlink_settings_group_margins_aa, carPlay = false, u.androidAutoMargins, openSheet)
    // Back to the Changan CS75 Plus defaults (48 px at the top) for all three; lit only when something differs.
    val changan = com.carlink.core.settings.ScreenMargins.CHANGAN
    val atChangan = u.marginTopPx == changan.top && u.carPlayMargins == changan && u.androidAutoMargins == changan
    GlassGroup {
        SettingsRow(stringResource(R.string.carlink_settings_margin_reset_changan),
            description = stringResource(R.string.carlink_settings_margin_reset_changan_detail),
            icon = R.drawable.carlink_ic_restart_alt, enabled = !atChangan, role = androidx.compose.ui.semantics.Role.Button,
            onClick = { actions.update { it.copy(marginTopPx = changan.top, carPlayMargins = changan, androidAutoMargins = changan) } },
            value = if (atChangan) stringResource(R.string.carlink_settings_margin_reset_changan_done) else null)
    }
}

@Composable
private fun ProjectionMargins(header: Int, carPlay: Boolean, margins: com.carlink.core.settings.ScreenMargins, openSheet: (String) -> Unit) {
    GlassGroup(header = { GroupHeader(stringResource(header)) }) {
        MarginEdge.entries.forEachIndexed { index, edge ->
            if (index > 0) GroupDivider()
            val (title, detail) = edge.texts()
            ChoiceRow(stringResource(title), marginLabel(margins.get(edge)),
                { openSheet(SheetKey.of(marginSheet(carPlay, edge))) },
                description = stringResource(detail),
                icon = if (index == 0) R.drawable.carlink_ic_aspect_ratio else null, tags = { NextConnectionTag() })
        }
    }
}

@Composable
private fun SoundEssentials(ui: SettingsUi, actions: SettingsActions, openSheet: (String) -> Unit) {
    val u = ui.unified
    val bluetooth = u.soundThroughCarBluetooth
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_sound)) }) {
        ChoiceRow(stringResource(R.string.carlink_settings_sound),
            stringResource(if (bluetooth) R.string.carlink_settings_sound_bluetooth else R.string.carlink_settings_sound_carlink),
            { openSheet(SheetKey.of(EssentialSheet.SOUND)) }, description = stringResource(R.string.carlink_settings_sound_detail),
            icon = if (bluetooth) R.drawable.carlink_ic_bluetooth else R.drawable.carlink_ic_volume_up)
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_audio_delay), audioDelayLabel(ui.unified.audioDelay),
            { openSheet(SheetKey.of(EssentialSheet.AUDIO_DELAY)) }, description = stringResource(R.string.carlink_settings_audio_delay_detail),
            icon = R.drawable.carlink_ic_graphic_eq, tags = { NextConnectionTag() })
        GroupDivider()
        SwitchRow(stringResource(R.string.carlink_settings_resume_music), u.resumeMusic,
            { on -> actions.update { it.copy(resumeMusic = on) } },
            description = stringResource(R.string.carlink_settings_resume_music_detail), icon = R.drawable.carlink_ic_play_arrow)
    }
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_levels)) }) {
        ChoiceRow(stringResource(R.string.carlink_settings_level_media), levelLabel(u.mediaLevelDb),
            { openSheet(SheetKey.of(EssentialSheet.MEDIA_LEVEL)) }, description = stringResource(R.string.carlink_settings_level_media_detail),
            icon = R.drawable.carlink_ic_volume_up, tags = { NextConnectionTag() })
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_level_navigation), levelLabel(u.navigationLevelDb),
            { openSheet(SheetKey.of(EssentialSheet.NAVIGATION_LEVEL)) },
            description = stringResource(R.string.carlink_settings_level_navigation_detail),
            icon = R.drawable.carlink_ic_navigation, tags = { NextConnectionTag() })
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_level_calls), levelLabel(u.callLevelDb),
            { openSheet(SheetKey.of(EssentialSheet.CALL_LEVEL)) }, description = stringResource(R.string.carlink_settings_level_calls_detail),
            icon = R.drawable.carlink_ic_call, tags = { NextConnectionTag() })
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_call_stream),
            stringResource(if (u.callAudioAsMedia) R.string.carlink_settings_call_stream_media else R.string.carlink_settings_call_stream_phone),
            { openSheet(SheetKey.of(EssentialSheet.CALL_STREAM)) }, description = stringResource(R.string.carlink_settings_call_stream_detail),
            icon = R.drawable.carlink_ic_phone_iphone, tags = { NextConnectionTag() })
    }
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_microphone)) }) {
        ChoiceRow(stringResource(R.string.carlink_settings_mic_gain), levelLabel(u.micGainDb),
            { openSheet(SheetKey.of(EssentialSheet.MIC_GAIN)) }, description = stringResource(R.string.carlink_settings_mic_gain_detail),
            icon = R.drawable.carlink_ic_mic, tags = { NextConnectionTag() })
    }
}

/** "Normal" for 0 dB, else a signed dB value. */
@Composable
internal fun levelLabel(db: Int): String =
    if (db == 0) stringResource(R.string.carlink_settings_level_normal)
    else stringResource(R.string.carlink_settings_level_db, if (db > 0) "+$db" else "−${-db}")

@Composable
private fun NavigationEssentials(ui: SettingsUi, actions: SettingsActions) {
    val u = ui.unified
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_location)) }) {
        SwitchRow(stringResource(R.string.carlink_settings_location), u.sendCarLocation,
            { on -> actions.update { it.copy(sendCarLocation = on) } },
            description = stringResource(R.string.carlink_settings_location_detail), icon = R.drawable.carlink_ic_location_on)
        if (ui.isByd) {
            GroupDivider()
            SwitchRow(stringResource(R.string.carlink_settings_byd), u.bydDirections,
                { on -> actions.update { it.copy(bydDirections = on) } },
                description = stringResource(R.string.carlink_settings_byd_detail), icon = R.drawable.carlink_ic_navigation)
        }
    }
}

@Composable
private fun CarEssentials(ui: SettingsUi, openSheet: (String) -> Unit) {
    val right = ui.unified.rightHandDrive
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_car)) }) {
        ChoiceRow(stringResource(R.string.carlink_settings_driving_side),
            stringResource(if (right) R.string.carlink_settings_drive_right else R.string.carlink_settings_drive_left),
            { openSheet(SheetKey.of(EssentialSheet.DRIVING_SIDE)) },
            description = stringResource(R.string.carlink_settings_driving_side_detail), icon = R.drawable.carlink_ic_directions_car)
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_car_brand), carBrandLabel(ui.carBrand ?: ui.detectedBrand),
            { openSheet(SheetKey.of(EssentialSheet.CAR_BRAND)) },
            description = stringResource(R.string.carlink_settings_car_brand_detail) + "\n" + detectedCarLabel(ui.detectedBrand),
            icon = brandLogo(ui.carBrand ?: ui.detectedBrand) ?: R.drawable.carlink_ic_directions_car_filled, tags = { NextConnectionTag() })
        GroupDivider()
        val learned = ui.unified.wheelKeys.size
        ChoiceRow(stringResource(R.string.carlink_wheel_keys),
            if (learned == 0) stringResource(R.string.carlink_settings_not_set)
            else pluralStringResource(R.plurals.carlink_wheel_keys_value, learned, learned),
            { openSheet(SheetKey.of(EssentialSheet.WHEEL_KEYS)) },
            description = stringResource(R.string.carlink_wheel_keys_detail), icon = R.drawable.carlink_ic_search_hands_free,
            tags = { NextConnectionTag() })
    }
}

@Composable
private fun GeneralEssentials(ui: SettingsUi, actions: SettingsActions, openSheet: (String) -> Unit, versionTaps: Int,
                              onVersionTap: () -> Unit) {
    val d = ui.display
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_carlink)) }) {
        ChoiceRow(stringResource(R.string.carlink_settings_language), languageLabel(ui.unified.appLanguage),
            { openSheet(SheetKey.of(EssentialSheet.LANGUAGE)) },
            description = stringResource(R.string.carlink_settings_language_detail), icon = R.drawable.carlink_ic_translate)
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_appearance), themeLabel(d.themeMode),
            { openSheet(SheetKey.of(EssentialSheet.APPEARANCE)) },
            description = stringResource(R.string.carlink_settings_appearance_detail), icon = themeIcon(d.themeMode))
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_layout), layoutLabel(d.uiProfileMode),
            { openSheet(SheetKey.of(EssentialSheet.LAYOUT)) },
            description = stringResource(R.string.carlink_settings_layout_detail), icon = layoutIcon(d.uiProfileMode))
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_carlink_size),
            "${carLinkScaleLabel(d.interfaceScale)} · ${percentText(nearestScale(d.interfaceScale))}",
            { openSheet(SheetKey.of(EssentialSheet.CARLINK_SIZE)) },
            description = stringResource(R.string.carlink_settings_carlink_size_detail), icon = R.drawable.carlink_ic_text_fields)
        GroupDivider()
        ChoiceRow(stringResource(R.string.carlink_settings_rendering), renderingValue(d),
            { openSheet(SheetKey.of(EssentialSheet.RENDERING)) },
            description = stringResource(R.string.carlink_settings_rendering_detail), icon = R.drawable.carlink_ic_dashboard)
        GroupDivider()
        SwitchRow(stringResource(R.string.carlink_settings_floating_switcher), ui.connection.floatingSwitcher,
            actions::setFloatingSwitcher, description = stringResource(R.string.carlink_settings_floating_switcher_detail),
            icon = R.drawable.carlink_ic_layers)
        if (ui.connection.floatingSwitcher && !ui.overlayAllowed) {
            GroupDivider()
            LinkRow(stringResource(R.string.carlink_settings_floating_switcher_permission), actions::openOverlayPermission,
                stringResource(R.string.carlink_settings_floating_switcher_permission_detail), R.drawable.carlink_ic_lock,
                external = true)
        }
    }
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_help)) }) {
        LinkRow(stringResource(R.string.carlink_settings_diagnostics), { actions.navigate(Route.DIAGNOSTICS) },
            stringResource(R.string.carlink_settings_diagnostics_detail), R.drawable.carlink_ic_monitor_heart)
        GroupDivider()
        ErrorLogRows()
        GroupDivider()
        LinkRow(stringResource(R.string.carlink_settings_setup_again), { actions.navigate(Route.SETUP) },
            stringResource(R.string.carlink_settings_setup_again_detail), R.drawable.carlink_ic_restart_alt)
        GroupDivider()
        LinkRow(stringResource(R.string.carlink_backup_action), actions::backupSettings,
            stringResource(R.string.carlink_backup_detail), R.drawable.carlink_ic_ios_share)
        GroupDivider()
        LinkRow(stringResource(R.string.carlink_restore_action), actions::restoreSettings,
            stringResource(R.string.carlink_restore_detail), R.drawable.carlink_ic_download)
    }
    GlassGroup(header = { GroupHeader(stringResource(R.string.carlink_settings_group_about)) }) {
        val remaining = LAB_TAPS - versionTaps
        val versionHint = when {
            ui.unified.labUnlocked -> null
            versionTaps >= 2 -> pluralStringResource(R.plurals.carlink_settings_lab_taps, remaining, remaining)
            else -> null
        }
        SettingsRow(stringResource(R.string.carlink_settings_version), description = versionHint, icon = R.drawable.carlink_ic_info,
            onClick = onVersionTap, value = ui.versionName)
        if (ui.unified.labUnlocked) {
            GroupDivider()
            SwitchRow(stringResource(R.string.carlink_settings_lab_toggle), true, { on -> actions.update { it.copy(labUnlocked = on) } },
                description = stringResource(R.string.carlink_settings_lab_toggle_detail), icon = R.drawable.carlink_ic_science)
        }
        GroupDivider()
        LinkRow(stringResource(R.string.carlink_settings_about_open), { actions.navigate(Route.ABOUT) },
            stringResource(R.string.carlink_settings_about_summary), R.drawable.carlink_ic_gavel)
    }
}

// ---- Advanced and Lab ----------------------------------------------------------------------

/** The category's engine settings of one tier, one glass group per engine. */
@Composable
private fun EngineSection(category: SettingsCategory, tier: Tier, ui: SettingsUi, actions: SettingsActions, openSheet: (String) -> Unit) {
    val groups = engineRows(category, tier)
    if (groups.isEmpty()) return
    val colors = CarLinkTokens.colors
    val lab = tier == Tier.LAB
    val accent = if (lab) colors.warning else colors.onSurfaceTertiary
    Column(Modifier.padding(top = CarLinkTokens.dimens.gutter / 2, start = 8.dp, end = 8.dp)) {
        GroupHeader(stringResource(if (lab) R.string.carlink_settings_lab else R.string.carlink_settings_advanced), color = accent,
            icon = if (lab) R.drawable.carlink_ic_science_filled else R.drawable.carlink_ic_tune)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(if (lab) R.string.carlink_settings_lab_detail else R.string.carlink_settings_advanced_detail),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    groups.forEach { (engine, rows) ->
        GlassGroup(frame = if (lab) colors.warning else null, header = { EngineHeader(engine, if (lab) colors.warning else null) }) {
            rows.forEachIndexed { index, setting ->
                if (index > 0) GroupDivider()
                EngineSettingRow(setting, ui.advancedValues, actions, openSheet)
            }
        }
    }
}

@Composable
internal fun EngineSettingRow(setting: AdvancedSetting, values: Map<String, String>?, actions: SettingsActions, openSheet: (String) -> Unit) {
    val value = values?.get(setting.id)
    val loaded = value != null
    val tags: (@Composable () -> Unit)? = if (setting.applies == Applies.NEXT_CONNECTION) ({ NextConnectionTag() }) else null
    when (setting.control) {
        is Control.Toggle -> SwitchRow(setting.title, value == "true", { on -> actions.setAdvanced(setting, on.toString()) },
            description = setting.description, enabled = loaded, tags = tags)
        is Control.Choice -> ChoiceRow(setting.title,
            advancedValueLabel(setting, value) ?: stringResource(R.string.carlink_settings_loading),
            { openSheet(SheetKey.of(setting)) }, description = setting.description, enabled = loaded, tags = tags)
    }
}

// ---- Icons ----------------------------------------------------------------------------------

internal fun methodIcon(method: ConnectionMethod): Int = when (method) {
    ConnectionMethod.AUTOMATIC -> R.drawable.carlink_ic_sync
    ConnectionMethod.SAME_WIFI -> R.drawable.carlink_ic_wifi
    ConnectionMethod.CAR_HOTSPOT -> R.drawable.carlink_ic_wifi_tethering
    ConnectionMethod.WIFI_DIRECT -> R.drawable.carlink_ic_p2p
    ConnectionMethod.HEADUNIT_SERVER -> R.drawable.carlink_ic_android
}

internal fun dayNightIcon(mode: DayNight): Int = when (mode) {
    DayNight.AUTOMATIC -> R.drawable.carlink_ic_brightness_auto
    DayNight.DAY -> R.drawable.carlink_ic_light_mode
    DayNight.NIGHT -> R.drawable.carlink_ic_dark_mode
    DayNight.SCHEDULE -> R.drawable.carlink_ic_schedule
}

internal fun themeIcon(mode: com.carlink.app.ui.theme.ThemeMode): Int = when (mode) {
    com.carlink.app.ui.theme.ThemeMode.FOLLOW_SYSTEM -> R.drawable.carlink_ic_brightness_auto
    com.carlink.app.ui.theme.ThemeMode.DARK -> R.drawable.carlink_ic_dark_mode
    com.carlink.app.ui.theme.ThemeMode.LIGHT -> R.drawable.carlink_ic_light_mode
}

internal fun layoutIcon(mode: com.carlink.app.ui.theme.UiProfileMode): Int = when (mode) {
    com.carlink.app.ui.theme.UiProfileMode.AUTO -> R.drawable.carlink_ic_devices
    com.carlink.app.ui.theme.UiProfileMode.CAR -> R.drawable.carlink_ic_directions_car
    com.carlink.app.ui.theme.UiProfileMode.HANDHELD -> R.drawable.carlink_ic_tablet_android
}

/**
 * Settings › General › Help: the last connection attempts' log (CarLink's own log, both engines'
 * logs and the system log of CarLink's processes) saved to Downloads or shared, all offline.
 */
@Composable
private fun ErrorLogRows() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val save = { scope.launch { com.carlink.app.ui.diagnostics.saveConnectionLog(context) }; Unit }
    // Android 9 and older write the public Download folder only with storage access; denied still saves (CarLink's own folder).
    val askStorage = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { save() }
    LinkRow(stringResource(R.string.carlink_settings_save_log), {
        val needsPermission = android.os.Build.VERSION.SDK_INT < 29 && androidx.core.content.ContextCompat.checkSelfPermission(context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) runCatching { askStorage.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) }.onFailure { save() }
        else save()
    }, stringResource(R.string.carlink_settings_save_log_detail), R.drawable.carlink_ic_download)
    GroupDivider()
    LinkRow(stringResource(R.string.carlink_settings_share_log), {
        scope.launch { com.carlink.app.ui.diagnostics.shareConnectionLog(context) }
    }, stringResource(R.string.carlink_settings_share_log_detail), R.drawable.carlink_ic_ios_share)
}

/** A language's name in its own script; "System default" in the current language. */
@Composable
internal fun languageLabel(tag: String): String =
    if (tag == com.carlink.core.settings.AppLanguage.SYSTEM) stringResource(R.string.carlink_settings_language_system)
    else com.carlink.app.ui.components.ltrIfLatin(com.carlink.core.settings.AppLanguage.nativeName(tag))
