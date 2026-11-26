package com.carlink.app.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.carlink.app.R
import com.carlink.app.switcher.FceBarAccessibilityService
import com.carlink.app.ui.premium.IonButton
import com.carlink.app.ui.theme.CarLinkTokens
import com.carlink.core.settings.CarBrand
import com.carlink.core.settings.HideCarStatusBarMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CS75-only switches, shown as the first Settings category when the driver has chosen
 * the CS75 Plus brand: hide-mode radios, the accessibility prompt, and the app picker.
 * Rendered in the same scrollable content pane as the other categories.
 */
@Composable
internal fun ChanganSettingsSection(ui: SettingsUi, actions: SettingsActions) {
    if (ui.carBrand?.isChanganCs75Plus != true) return
    val u = ui.unified
    val context = LocalContext.current
    var accessibilityOn by remember { mutableStateOf(FceBarAccessibilityService.isEnabled(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        accessibilityOn = FceBarAccessibilityService.isEnabled(context)
        onPauseOrDispose { }
    }
    val mode = u.hideCarStatusBarMode
    GlassGroup {
        AccessibilityNotice(enabled = accessibilityOn, onOpenSettings = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        GroupDivider()
        SwitchRow(
            title = stringResource(R.string.carlink_settings_hide_bar_all),
            checked = mode == HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER,
            onCheckedChange = { on -> actions.update { it.copy(hideCarStatusBarMode = if (on) HideCarStatusBarMode.ALL_EXCEPT_LAUNCHER else HideCarStatusBarMode.OFF) } },
            description = stringResource(R.string.carlink_settings_hide_bar_all_detail),
        )
        GroupDivider()
        SwitchRow(
            title = stringResource(R.string.carlink_settings_hide_bar_selected),
            checked = mode == HideCarStatusBarMode.SELECTED_APPS,
            onCheckedChange = { on -> actions.update { it.copy(hideCarStatusBarMode = if (on) HideCarStatusBarMode.SELECTED_APPS else HideCarStatusBarMode.OFF) } },
            description = stringResource(R.string.carlink_settings_hide_bar_selected_detail),
        )
        if (mode == HideCarStatusBarMode.SELECTED_APPS) {
            GroupDivider()
            AppPickerList(
                selected = u.hideCarStatusBarPackages,
                ownPackage = context.packageName,
                onToggle = { pkg, on ->
                    actions.update { settings ->
                        val next = settings.hideCarStatusBarPackages.toMutableSet()
                        if (on) next.add(pkg) else next.remove(pkg)
                        settings.copy(hideCarStatusBarPackages = next)
                    }
                },
            )
        }
    }
}

@Composable
private fun AccessibilityNotice(enabled: Boolean, onOpenSettings: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = CarLinkTokens.colors
    Column(Modifier.padding(horizontal = rowPadding, vertical = if (isCar) 18.dp else 12.dp)) {
        Text(
            stringResource(R.string.carlink_settings_hide_bar_accessibility_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) scheme.onSurface else colors.warning,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(
                if (enabled) R.string.carlink_settings_hide_bar_accessibility_on
                else R.string.carlink_settings_hide_bar_accessibility_off,
            ),
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) scheme.primary else colors.warning,
        )
        Spacer(Modifier.height(12.dp))
        IonButton(
            stringResource(R.string.carlink_settings_hide_bar_accessibility_button),
            onOpenSettings,
            Modifier,
            icon = R.drawable.carlink_ic_open_in_new,
        )
    }
}

@Composable
private fun AppPickerList(
    selected: Set<String>,
    ownPackage: String,
    onToggle: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LaunchableApps.App>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            LaunchableApps.sorted(LaunchableApps.list(context.packageManager), ownPackage, selected)
        }
    }
    Text(
        stringResource(R.string.carlink_settings_hide_bar_apps_heading),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = rowPadding, vertical = 8.dp),
    )
    if (apps.size >= 8) {
        val colors = CarLinkTokens.colors
        val scheme = MaterialTheme.colorScheme
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = rowPadding, vertical = 4.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.carlink_settings_hide_bar_apps_search)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.glassFill, unfocusedContainerColor = colors.glassFill,
                focusedBorderColor = scheme.primary, unfocusedBorderColor = colors.glassStrokeTop,
                focusedLabelColor = scheme.primary, cursorColor = scheme.primary,
            ),
        )
    }
    val visible = if (query.isBlank()) apps else apps.filter { LaunchableApps.matchesSearch(it, query) }
    visible.forEach { app ->
        val isOwn = app.packageName == ownPackage
        val checked = isOwn || app.packageName in selected
        SettingsRow(
            title = app.label,
            description = if (isOwn) stringResource(R.string.carlink_settings_hide_bar_apps_carlink) else app.packageName,
            enabled = !isOwn,
            onClick = if (isOwn) null else ({ onToggle(app.packageName, !checked) }),
            role = Role.Switch,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.icon != null) {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(if (isCar) 40.dp else 32.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    IonSwitch(checked = checked, enabled = !isOwn)
                }
            },
        )
        GroupDivider()
    }
}
