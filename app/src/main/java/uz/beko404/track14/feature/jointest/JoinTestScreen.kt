package uz.beko404.track14.feature.jointest

import uz.beko404.track14.feature.auth.AuthViewModel
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.core.designsystem.SecondaryButton
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.BackButton
import uz.beko404.track14.core.designsystem.AppTile
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.feature.messageRes

@Composable
fun JoinTestRoute(
    onBack: () -> Unit,
    onStarted: () -> Unit,
    viewModel: JoinTestViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val groupUrl = stringResource(R.string.testers_group_url)
    val clipboard = LocalClipboardManager.current

    LifecycleResumeEffect(Unit) {
        viewModel.refreshUsageAccess()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.steps.startedTestId) {
        if (state.steps.startedTestId != null) onStarted()
    }

    JoinTestScreen(
        state = state,
        email = (auth.session as? Session.SignedIn)?.email.orEmpty(),
        onBack = onBack,
        onCopyGroup = { clipboard.setText(AnnotatedString(GROUP_EMAIL)) },
        onOpenGroup = { if (ExternalLinks.openTrustedUrl(context, groupUrl)) viewModel.onGroupOpened() },
        onOpenOptIn = { url -> if (ExternalLinks.openOptIn(context, url)) viewModel.onOptInOpened() },
        onOpenStore = { pkg -> ExternalLinks.openStorePage(context, pkg) },
        onConfirmInstalled = viewModel::onStoreOpened,
        onOpenUsageSettings = { ExternalLinks.openUsageAccessSettings(context) },
        onStart = {
            viewModel.start()
            state.app?.packageName?.let { ExternalLinks.openApp(context, it) }
        },
        onGroupDone = viewModel::onGroupOpened,
        onOptInDone = viewModel::onOptInOpened,
    )
}

private const val GROUP_EMAIL = "track14-testers@googlegroups.com"

/**
 * The canvas wizard: 4 step bars, then one page per step (Guruh, Opt-in, O'rnatish, Usage Access
 * when not granted yet, Birinchi ochish). Each page has the icon / label / title / body block,
 * its own card and the two bottom buttons.
 */
@Composable
fun JoinTestScreen(
    state: JoinTestUiState,
    email: String,
    onBack: () -> Unit,
    onCopyGroup: () -> Unit,
    onOpenGroup: () -> Unit,
    onOpenOptIn: (String) -> Unit,
    onOpenStore: (String) -> Unit,
    onConfirmInstalled: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onStart: () -> Unit,
    onGroupDone: () -> Unit,
    onOptInDone: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val app = state.app
    val goBack = { if (page > 0) page -= 1 else onBack() }
    androidx.activity.compose.BackHandler(enabled = page > 0) { page -= 1 }

    Column(Modifier.fillMaxSize()) {
        if (page == PAGE_USAGE) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(goBack)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.join_usage_once), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = T14Theme.colors.textBody, modifier = Modifier.padding(end = 12.dp))
            }
        } else {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackButton(goBack)
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(stringResource(R.string.join_header), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                    if (app != null) Text(app.name, fontSize = 13.sp, color = T14Theme.colors.textSecondary)
                }
            }
            StepBars(current = if (page >= PAGE_USAGE) 3 else page)
        }
        when {
            state.loading -> SkeletonList(count = 2, modifier = Modifier.padding(20.dp))
            app == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.detail_not_found), color = T14Theme.colors.textSecondary)
            }
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = if (page == PAGE_USAGE) 12.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    when (page) {
                        0 -> GroupPage(onCopyGroup)
                        1 -> OptInPage(email)
                        2 -> InstallPage(app.id, app.name, app.category, app.packageName, state.steps.installed)
                        PAGE_USAGE -> UsagePage()
                        else -> OpenPage(state)
                    }
                }
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.steps.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    when (page) {
                        0 -> {
                            SecondaryButton(stringResource(R.string.join_step_group_action) + "  ↗", onClick = onOpenGroup)
                            PrimaryButton(stringResource(R.string.join_group_done), onClick = { onGroupDone(); page = 1 }, height = 56.dp)
                        }
                        1 -> {
                            SecondaryButton(stringResource(R.string.join_optin_open) + "  ↗", onClick = { onOpenOptIn(app.optInUrl) }, enabled = app.optInUrl.isNotEmpty())
                            PrimaryButton(stringResource(R.string.join_optin_done), onClick = { onOptInDone(); page = 2 }, height = 56.dp)
                        }
                        2 -> if (state.steps.installed) {
                            PrimaryButton(stringResource(R.string.join_continue), onClick = { page = if (state.steps.usageGranted) PAGE_OPEN else PAGE_USAGE }, height = 56.dp)
                        } else {
                            SecondaryButton(stringResource(R.string.join_step_install_action) + "  ↗", onClick = { onOpenStore(app.packageName) }, enabled = app.packageName.isNotEmpty())
                            PrimaryButton(stringResource(R.string.join_check), onClick = onConfirmInstalled, height = 56.dp)
                        }
                        PAGE_USAGE -> {
                            PrimaryButton(stringResource(R.string.join_usage_open_settings), onClick = onOpenUsageSettings, height = 56.dp)
                            Box(
                                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable { page = PAGE_OPEN },
                                contentAlignment = Alignment.Center,
                            ) { Text(stringResource(if (state.steps.usageGranted) R.string.join_continue else R.string.join_later), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = T14Theme.colors.textBody) }
                        }
                        else -> PrimaryButton(
                            stringResource(R.string.join_open_app, app.name) + "  ↗",
                            onClick = onStart,
                            enabled = state.canStart,
                            height = 56.dp,
                        )
                    }
                }
            }
        }
    }
}

private const val PAGE_USAGE = 3
private const val PAGE_OPEN = 4

/** Four bars with labels: done and current in blue, upcoming grey. */
@Composable
private fun StepBars(current: Int) {
    val c = T14Theme.colors
    val names = listOf(R.string.join_bar_group, R.string.join_bar_optin, R.string.join_bar_install, R.string.join_bar_open)
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { i, name ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(if (i <= current) Track14Colors.Blue600 else if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0)))
                Text(
                    stringResource(name), fontSize = 12.sp,
                    fontWeight = if (i == current) FontWeight.Bold else FontWeight.SemiBold,
                    color = when { i == current -> MaterialTheme.colorScheme.primary; i < current -> MaterialTheme.colorScheme.onSurface; else -> c.textTertiary },
                )
            }
        }
    }
}

@Composable
private fun StepIntro(label: String, title: String, body: String, iconPath: String? = null) {
    val c = T14Theme.colors
    if (iconPath != null) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(c.blueSoft), contentAlignment = Alignment.Center) {
            T14Icon(iconPath, size = 36.dp, tint = Track14Colors.Blue600, strokeWidth = 1.9f)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.4.sp)
        Text(title, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp)
        Text(body, fontSize = 15.sp, lineHeight = 23.sp, color = c.textBody)
    }
}

@Composable
private fun CheckLine(text: String, ok: Boolean) {
    val c = T14Theme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ok) T14Icon("M5 12l5 5 9-10", size = 20.dp, tint = c.successText, strokeWidth = 2.4f)
        else T14Icon("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 8v5M12 16.5v.5", size = 20.dp, tint = Color(0xFFB45309), strokeWidth = 2.2f)
        Text(text, fontSize = 14.sp, color = c.textBody)
    }
}

@Composable
private fun GroupPage(onCopy: () -> Unit) {
    val c = T14Theme.colors
    StepIntro(stringResource(R.string.join_label_1), stringResource(R.string.join_group_title), stringResource(R.string.join_group_body), T14Paths.Users)
    T14Card(radius = 18.dp, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Google Group", fontSize = 12.sp, color = c.textSecondary)
                Text(GROUP_EMAIL, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            val label = stringResource(R.string.join_copy)
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (c.dark) Color(0xFF1E293B) else Color(0xFFF1F5F9)).clickable(onClick = onCopy).semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) { T14Icon("M10.5 8h7A2.5 2.5 0 0 1 20 10.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 8 17.5v-7A2.5 2.5 0 0 1 10.5 8z M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2", size = 20.dp, tint = c.textBody) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CheckLine(stringResource(R.string.join_group_hidden), ok = true)
        CheckLine(stringResource(R.string.join_group_no_mail), ok = true)
        CheckLine(stringResource(R.string.join_group_leave_warning), ok = false)
    }
}

@Composable
private fun OptInPage(email: String) {
    val c = T14Theme.colors
    StepIntro(stringResource(R.string.join_label_2), stringResource(R.string.join_optin_title), stringResource(R.string.join_optin_body))
    T14Card(radius = 20.dp, padding = PaddingValues(16.dp), spacing = 16.dp) {
        listOf(
            R.string.join_optin_step1_t to R.string.join_optin_step1_s,
            R.string.join_optin_step2_t to R.string.join_optin_step2_s,
            R.string.join_optin_step3_t to R.string.join_optin_step3_s,
        ).forEachIndexed { i, (t, sub) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(Track14Colors.Blue600), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(t), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(sub), fontSize = 13.sp, lineHeight = 19.sp, color = c.textSecondary)
                }
            }
        }
    }
    if (email.isNotBlank()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.blueSoft).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Track14Colors.Blue600), contentAlignment = Alignment.Center) {
                Text(email.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.join_optin_account), fontSize = 12.sp, color = c.blueSoftText)
                Text(email, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (c.dark) Color.White else Color(0xFF172554))
            }
        }
    }
    Notice(stringResource(R.string.join_optin_warning), amber = true)
}

@Composable
private fun InstallPage(id: String, name: String, category: String, packageName: String, installed: Boolean) {
    val c = T14Theme.colors
    StepIntro(stringResource(R.string.join_label_3), stringResource(R.string.join_install_title), stringResource(R.string.join_install_body))
    T14Card(radius = 20.dp, padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppTile(id, name, size = 56.dp, category = category)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(packageName, fontSize = 13.sp, color = c.textSecondary, maxLines = 1)
            }
            if (installed) {
                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(c.successSoft).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    T14Icon("M5 12l5 5 9-10", size = 16.dp, tint = c.successText, strokeWidth = 2.6f)
                    Text(stringResource(R.string.join_installed), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.successText)
                }
            } else {
                Text(
                    stringResource(R.string.join_waiting), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.textBody,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (c.dark) Color(0xFF1E293B) else Color(0xFFF1F5F9)).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
    if (installed) Notice(stringResource(R.string.join_install_found), success = true)
    else Notice(stringResource(R.string.join_install_hint))
}

@Composable
private fun UsagePage() {
    val c = T14Theme.colors
    // Picture of the system settings row the user will switch on.
    T14Card(radius = 22.dp, padding = PaddingValues(18.dp)) {
        Text(stringResource(R.string.join_usage_path), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.blueSoft).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))), contentAlignment = Alignment.Center) {
                Text("14", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Track14", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.join_usage_granted), fontSize = 12.sp, color = c.blueSoftText)
            }
            Box(Modifier.size(width = 48.dp, height = 28.dp).clip(RoundedCornerShape(999.dp)).background(Track14Colors.Blue600), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.padding(end = 3.dp).size(22.dp).clip(CircleShape).background(Color.White))
            }
        }
        Row(Modifier.padding(horizontal = 12.dp).alpha(0.55f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0)))
            Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0)))
            Box(Modifier.size(width = 48.dp, height = 28.dp).clip(RoundedCornerShape(999.dp)).background(if (c.dark) Color(0xFF334155) else Color(0xFFCBD5E1)))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.join_usage_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp)
        Text(stringResource(R.string.join_usage_body), fontSize = 15.sp, lineHeight = 23.sp, color = c.textBody)
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        UsagePoint(stringResource(R.string.join_usage_point1), ok = true)
        UsagePoint(stringResource(R.string.join_usage_point2), ok = true)
        UsagePoint(stringResource(R.string.join_usage_point3), ok = false)
    }
}

@Composable
private fun UsagePoint(text: String, ok: Boolean) {
    val c = T14Theme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (ok) c.successSoft else c.amberSoft), contentAlignment = Alignment.Center) {
            T14Icon(if (ok) "M5 12l5 5 9-10" else "M12 7v6M12 17v.5", size = 16.dp, tint = if (ok) c.successText else Color(0xFFB45309), strokeWidth = 2.6f)
        }
        Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = c.textBody, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun OpenPage(state: JoinTestUiState) {
    val c = T14Theme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            val track = if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0)
            Canvas(Modifier.size(200.dp)) {
                val stroke = 14.dp.toPx()
                drawCircle(track, radius = size.minDimension / 2 - 20.dp.toPx(), style = Stroke(stroke))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("30", fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp)
                Text(stringResource(R.string.join_open_seconds), fontSize = 13.sp, color = c.textSecondary)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.join_label_4), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.4.sp)
            Text(stringResource(R.string.join_open_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp, textAlign = TextAlign.Center)
            Text(stringResource(R.string.join_open_body), fontSize = 15.sp, lineHeight = 23.sp, color = c.textBody, textAlign = TextAlign.Center)
        }
        if (!state.steps.joinedGroup || !state.steps.optedIn || !state.steps.installed) {
            Notice(stringResource(R.string.join_open_missing), amber = true)
        }
    }
}

@Composable
private fun Notice(text: String, amber: Boolean = false, success: Boolean = false) {
    val c = T14Theme.colors
    val (bg, border, fg, icon) = when {
        success -> listOf(if (c.dark) Color(0xFF0F2E1E) else Color(0xFFF0FDF4), if (c.dark) Color(0xFF14532D) else Color(0xFFBBF7D0), if (c.dark) Color(0xFF86EFAC) else Color(0xFF14532D), "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M8 12l3 3 5-6")
        amber -> listOf(c.amberSoft, c.amberBorder, if (c.dark) Color(0xFFFCD34D) else Color(0xFF78350F), "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 8v5M12 16.5v.5")
        else -> listOf(MaterialTheme.colorScheme.surface, if (c.dark) Color(0xFF334155) else Color(0xFFCBD5E1), c.textBody, "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 7v5l3 2")
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg as Color).border(1.dp, border as Color, RoundedCornerShape(16.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        T14Icon(icon as String, size = 22.dp, tint = if (amber) Color(0xFFB45309) else fg as Color)
        Text(text, fontSize = 14.sp, lineHeight = 21.sp, color = fg as Color)
    }
}
