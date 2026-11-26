package com.carlink.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carlink.app.R
import com.carlink.app.ui.premium.GlassButton
import com.carlink.app.ui.premium.IonButton
import java.text.DateFormat
import java.util.Date

/** Back up: Wi-Fi passwords stay out unless the driver includes them (the file is readable by whoever has it). */
@Composable
internal fun BackupPasswordsDialog(onChoose: (withPasswords: Boolean) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.carlink_backup_passwords_title)) },
        text = { Text(stringResource(R.string.carlink_backup_passwords_body), color = scheme.onSurfaceVariant) },
        confirmButton = {
            IonButton(stringResource(R.string.carlink_backup_without_passwords), { onChoose(false) }, icon = R.drawable.carlink_ic_check)
        },
        dismissButton = {
            GlassButton(stringResource(R.string.carlink_backup_with_passwords), { onChoose(true) }, icon = R.drawable.carlink_ic_lock)
        },
        containerColor = scheme.surfaceContainerHigh,
    )
}

/** Restore: what the file holds and what it replaces, asked before anything is written. */
@Composable
internal fun RestoreConfirmDialog(backup: SettingsBackup.Backup, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val count = pluralStringResource(R.plurals.carlink_restore_count, backup.count, backup.count)
    val body = listOfNotNull(
        backup.createdMillis?.let { stringResource(R.string.carlink_restore_body_dated, count,
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))) }
            ?: stringResource(R.string.carlink_restore_body, count),
        if (backup.withPasswords) null else stringResource(R.string.carlink_restore_passwords_kept),
    ).joinToString("\n\n")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.carlink_restore_title)) },
        text = { Text(body, color = scheme.onSurfaceVariant) },
        confirmButton = { IonButton(stringResource(R.string.carlink_restore_confirm), onConfirm, icon = R.drawable.carlink_ic_restart_alt) },
        dismissButton = { GlassButton(stringResource(R.string.carlink_home_cancel), onDismiss) },
        containerColor = scheme.surfaceContainerHigh,
    )
}

/**
 * Shown after the driver picks the CS75 Plus entry (brand list, setup, or Yes on the first-launch
 * car question). Enable turns on status-bar hide in every app except the car's home screen.
 * Skip or Back leaves it off. The switch stays in the CS75 Settings category. The 48 px top
 * margin is not changed here; it stays off until they turn it on in Settings.
 */
@Composable
internal fun Cs75HideBarDialog(onAnswer: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        title = { Text(stringResource(R.string.carlink_cs75_hide_bar_prompt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.carlink_cs75_hide_bar_prompt_body), color = scheme.onSurface)
                Text(stringResource(R.string.carlink_cs75_hide_bar_prompt_hint), color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { IonButton(stringResource(R.string.carlink_cs75_hide_bar_prompt_enable), { onAnswer(true) }) },
        dismissButton = { GlassButton(stringResource(R.string.carlink_cs75_hide_bar_prompt_skip), { onAnswer(false) }) },
        containerColor = scheme.surfaceContainerHigh,
    )
}

/** A file that is not a backup, or one that was changed: nothing was written. */
@Composable
internal fun RestoreErrorDialog(reason: String?, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.carlink_restore_error_title)) },
        text = { Text(stringResource(R.string.carlink_restore_error_body, reason ?: "?"), color = scheme.onSurfaceVariant) },
        confirmButton = { IonButton(stringResource(R.string.carlink_restore_error_ok), onDismiss) },
        containerColor = scheme.surfaceContainerHigh,
    )
}

/**
 * First launch after a fresh install only. Yes selects the CS75 Plus brand and applies its
 * projection defaults, then the status-bar hide prompt is shown. No leaves the normal flow.
 * Back counts as No. Shown once.
 */
@Composable
internal fun Cs75FirstLaunchDialog(onAnswer: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        title = { Text(stringResource(R.string.carlink_cs75_first_launch_title)) },
        confirmButton = { IonButton(stringResource(R.string.carlink_cs75_first_launch_yes), { onAnswer(true) }) },
        dismissButton = { GlassButton(stringResource(R.string.carlink_cs75_first_launch_no), { onAnswer(false) }) },
        containerColor = scheme.surfaceContainerHigh,
    )
}
