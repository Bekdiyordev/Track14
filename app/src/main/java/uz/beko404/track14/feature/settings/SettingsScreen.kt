package uz.beko404.track14.feature.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.BackButton
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.core.device.UsageAccess
import uz.beko404.track14.core.settings.AppLanguage
import uz.beko404.track14.core.settings.ThemeMode
import uz.beko404.track14.feature.welcome.label

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val c = T14Theme.colors
    val privacyUrl = stringResource(R.string.privacy_url)
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("")
    }
    // Permissions can change in system settings while we are away: re-read them on every resume.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val usage = remember(resumes) { UsageAccess(context.applicationContext).hasAccess() }
    val notifications = remember(resumes) { NotificationManagerCompat.from(context).areNotificationsEnabled() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BackButton(onBack)
            Text(stringResource(R.string.settings_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
            Caps(stringResource(R.string.settings_language_caps), top = 14)
            val current = viewModel.language
            Segmented(
                options = AppLanguage.entries.map { stringResource(it.label()) to null },
                selected = AppLanguage.entries.indexOf(current),
                onSelect = { i -> AppLanguage.entries[i].takeIf { it != current }?.let(viewModel::setLanguage) },
            )

            Caps(stringResource(R.string.settings_theme_caps))
            Segmented(
                options = ThemeMode.entries.map { stringResource(it.label()) to it.icon() },
                selected = ThemeMode.entries.indexOf(settings.theme),
                onSelect = { i -> viewModel.setTheme(ThemeMode.entries[i]) },
            )

            Caps(stringResource(R.string.settings_notifications_caps))
            Group {
                ToggleRow(stringResource(R.string.settings_push_reminders), stringResource(R.string.settings_push_reminders_sub), settings.pushReminders, divider = true) {
                    viewModel.setPush(it, settings.pushOwner)
                }
                ToggleRow(stringResource(R.string.settings_push_owner), stringResource(R.string.settings_push_owner_sub), settings.pushOwner, divider = false) {
                    viewModel.setPush(settings.pushReminders, it)
                }
            }

            Caps(stringResource(R.string.settings_permissions_caps))
            Group {
                PermissionRow("Usage Access", usage, divider = true) { ExternalLinks.openUsageAccessSettings(context) }
                PermissionRow(stringResource(R.string.settings_notifications), notifications, divider = false) {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
                    }
                    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            }

            Caps(stringResource(R.string.settings_other_caps))
            Group {
                LinkRow(stringResource(R.string.settings_privacy), divider = true) { ExternalLinks.openTrustedUrl(context, privacyUrl) }
                if (signedIn) LinkRow(stringResource(R.string.settings_sign_out), divider = true, enabled = !account.deleting, onClick = viewModel::signOut)
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_version_label), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(version, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.textSecondary)
                }
            }

            if (signedIn) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, c.dangerBorder, RoundedCornerShape(16.dp)).clickable(enabled = !account.deleting) { confirmDelete = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(if (account.deleting) R.string.settings_deleting else R.string.settings_delete_account),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.dangerText,
                        )
                    }
                    Text(stringResource(R.string.settings_delete_note), fontSize = 12.sp, lineHeight = 18.sp, color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    account.error?.let { Text(stringResource(R.string.settings_delete_failed), fontSize = 13.sp, color = c.dangerText) }
                }
            }
            if (account.deleted) {
                Text(stringResource(R.string.settings_deleted), fontSize = 13.sp, color = c.textBody, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.settings_delete_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
            text = { Text(stringResource(R.string.settings_delete_body), fontSize = 15.sp, lineHeight = 22.sp, color = c.textBody) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteAccount() }) {
                    Text(stringResource(R.string.settings_delete_confirm), color = c.dangerText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.settings_delete_cancel), color = c.textSecondary) } },
        )
    }
}

@Composable
private fun Caps(text: String, top: Int = 22) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T14Theme.colors.textSecondary, letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = top.dp, bottom = 8.dp),
    )
}

/** Grey track with a white pill on the selected option (language and theme). */
@Composable
private fun Segmented(options: List<Pair<String, String?>>, selected: Int, onSelect: (Int) -> Unit) {
    val c = T14Theme.colors
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (c.dark) Color(0xFF1E293B) else Color(0xFFE2E8F0)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, (label, icon) ->
            val on = i == selected
            Row(
                Modifier.weight(1f).height(40.dp)
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(12.dp)) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) (if (c.dark) Color(0xFF334155) else Color.White) else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(i) },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fg = if (on) MaterialTheme.colorScheme.onSurface else c.textBody
                if (icon != null) T14Icon(icon, size = 16.dp, tint = fg, strokeWidth = 2.2f)
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
            }
        }
    }
}

private fun ThemeMode.icon(): String = when (this) {
    ThemeMode.LIGHT -> "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8 M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4"
    ThemeMode.DARK -> "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"
    ThemeMode.SYSTEM -> "M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z M8 21h8M12 17v4"
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    T14Card(Modifier.padding(horizontal = 20.dp), radius = 20.dp, padding = PaddingValues(0.dp), spacing = 0.dp, content = content)
}

private fun Modifier.bottomHairline(on: Boolean, color: Color): Modifier =
    if (!on) this else drawBehind { drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }

@Composable
private fun ToggleRow(title: String, sub: String, on: Boolean, divider: Boolean, onChange: (Boolean) -> Unit) {
    val c = T14Theme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 62.dp).bottomHairline(divider, c.divider)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, fontSize = 12.sp, color = c.textSecondary)
        }
        val track by animateColorAsState(if (on) Track14Colors.Blue600 else (if (c.dark) Color(0xFF334155) else Color(0xFFCBD5E1)), label = "track")
        val knob by animateDpAsState(if (on) 23.dp else 3.dp, label = "knob")
        Box(Modifier.size(width = 50.dp, height = 30.dp).clip(RoundedCornerShape(999.dp)).background(track)) {
            Box(Modifier.padding(start = knob, top = 3.dp).size(24.dp).shadow(1.dp, CircleShape).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, divider: Boolean, onClick: () -> Unit) {
    val c = T14Theme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).bottomHairline(divider, c.divider).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(
            stringResource(if (granted) R.string.settings_granted else R.string.settings_not_granted),
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (granted) c.successText else c.amberText,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (granted) c.successSoft else c.amberSoft).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LinkRow(title: String, divider: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).bottomHairline(divider, T14Theme.colors.divider).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}
