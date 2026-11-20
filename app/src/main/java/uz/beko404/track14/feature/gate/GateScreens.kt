@file:OptIn(ExperimentalMaterial3Api::class)

package uz.beko404.track14.feature.gate

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.BackButton
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.ScreenBackground
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.designsystem.Track14Logo
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.domain.model.GateState
import uz.beko404.track14.domain.model.SupportConfig

private const val GROUP_EMAIL = "track14-testers@googlegroups.com"

// ---------------------------------------------------------------- full-screen states

/** Full-screen states that replace the whole app (no navigation behind them). */
@Composable
fun GateScreen(state: GateState, support: SupportConfig, onSignOut: () -> Unit = {}, onRetry: () -> Unit = {}) {
    val context = LocalContext.current
    val openStore = { ExternalLinks.openStorePage(context, context.packageName) }
    val c = T14Theme.colors
    ScreenBackground {
        when (state) {
            is GateState.UpdateRequired -> StateLayout(
                art = { Track14Logo(104.dp, Modifier.shadow(24.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x402563EB), spotColor = Color(0x402563EB))) },
                title = stringResource(R.string.gate_update_title),
                body = stringResource(R.string.gate_update_body),
                extra = {
                    val version = remember { currentVersion(context) }
                    if (version.isNotEmpty()) {
                        Row(
                            Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(stringResource(R.string.gate_you_have), fontSize = 14.sp, color = c.textSecondary)
                            Text(version, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            T14Icon("M5 12h14M13 6l6 6-6 6", size = 18.dp, tint = c.textTertiary)
                            Text(stringResource(R.string.gate_new), fontSize = 14.sp, color = c.textSecondary)
                        }
                    }
                    if (state.message.isNotBlank()) WhatsNew(state.message)
                },
                bottom = {
                    PrimaryButton(stringResource(R.string.gate_update_store), onClick = { openStore() })
                    Text(stringResource(R.string.gate_update_cannot_skip), fontSize = 12.sp, color = c.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
            )
            is GateState.Maintenance -> StateLayout(
                art = { CircleArt(if (c.dark) Color(0xFF2A200A) else Color(0xFFFEF3C7)) { T14Icon(WRENCH, size = 46.dp, tint = Color(0xFFB45309)) } },
                title = stringResource(R.string.gate_maintenance_title),
                body = state.message.ifBlank { stringResource(R.string.gate_maintenance_body) },
                extra = {
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth()) { OutlineBlue(stringResource(R.string.gate_retry), onRetry) }
                },
            )
            is GateState.Blocked -> StateLayout(
                art = { CircleArt(if (c.dark) c.dangerSoft else Color(0xFFFEE2E2)) { T14Icon("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M5.6 5.6l12.8 12.8", size = 48.dp, tint = c.dangerText, strokeWidth = 2.2f) } },
                title = stringResource(R.string.gate_blocked_title),
                body = stringResource(R.string.gate_blocked_lead),
                extra = {
                    T14Card(radius = 16.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), spacing = 10.dp) {
                        if (state.reason.isNotBlank()) {
                            Row {
                                Text(stringResource(R.string.gate_reason), fontSize = 14.sp, color = c.textSecondary, modifier = Modifier.weight(1f))
                                Text(state.reason, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(2f))
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                        }
                        Text(stringResource(R.string.gate_blocked_body), fontSize = 13.sp, lineHeight = 19.sp, color = c.textBody)
                    }
                },
                bottom = {
                    val email = support.safeEmail
                    val telegram = support.safeTelegram
                    if (email != null) PrimaryButton(stringResource(R.string.gate_appeal), onClick = { ExternalLinks.openEmail(context, email) })
                    if (telegram != null) OutlineBlue(stringResource(R.string.gate_contact_support)) { ExternalLinks.openTrustedUrl(context, telegram) }
                    Text(
                        stringResource(R.string.settings_sign_out), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onSignOut).padding(vertical = 8.dp),
                    )
                },
            )
            GateState.Loading, is GateState.Open -> Unit
        }
    }
}

private const val WRENCH = "M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.6 2.6-2.4-.6-.6-2.4z"

private fun currentVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}.getOrDefault("")

/** Centered art, title and body, optional extra block; optional white bottom bar with the actions. */
@Composable
private fun StateLayout(
    art: @Composable () -> Unit,
    title: String,
    body: String,
    extra: @Composable ColumnScope.() -> Unit = {},
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = T14Theme.colors
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            art()
            Text(title, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Text(body, fontSize = 15.sp, lineHeight = 23.sp, color = c.textBody, textAlign = TextAlign.Center)
            extra()
        }
        if (bottom != null) {
            Column(
                Modifier.fillMaxWidth().background(c.navBar)
                    .drawBehind { drawLine(c.navBorder, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = bottom,
            )
        }
    }
}

@Composable
private fun CircleArt(bg: Color, icon: @Composable () -> Unit) {
    Box(Modifier.size(96.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { icon() }
}

@Composable
private fun WhatsNew(message: String) {
    val c = T14Theme.colors
    T14Card(radius = 16.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), spacing = 8.dp) {
        Text(stringResource(R.string.gate_whats_new), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.textSecondary, letterSpacing = 0.4.sp)
        message.lines().filter { it.isNotBlank() }.take(6).forEach { Text(it.trim(), fontSize = 14.sp, lineHeight = 20.sp, color = c.textBody) }
    }
}

@Composable
private fun OutlineBlue(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, Track14Colors.Blue600, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (T14Theme.colors.dark) Color(0xFF60A5FA) else Track14Colors.Blue600) }
}

// ---------------------------------------------------------------- optional update (bottom sheet)

@Composable
fun OptionalUpdateDialog(onUpdate: () -> Unit, onLater: () -> Unit, message: String = "") {
    val c = T14Theme.colors
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x8C111827),
        dragHandle = null,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Track14Logo(56.dp, cornerUnits = 16f)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.gate_optional_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    val v = remember { currentVersion(context) }
                    if (v.isNotEmpty()) Text(stringResource(R.string.gate_you_have) + " " + v, fontSize = 13.sp, color = c.textSecondary)
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.divider).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val lines = message.lines().filter { it.isNotBlank() }.take(5)
                if (lines.isEmpty()) {
                    Text(stringResource(R.string.gate_optional_body), fontSize = 14.sp, lineHeight = 20.sp, color = c.textBody)
                } else {
                    lines.forEach { Text(it.trim(), fontSize = 14.sp, lineHeight = 20.sp, color = c.textBody) }
                }
            }
            PrimaryButton(stringResource(R.string.gate_update_action), onClick = onUpdate)
            Box(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, c.fieldBorder, RoundedCornerShape(16.dp)).clickable(onClick = onLater),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.gate_later), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textBody) }
            Text(stringResource(R.string.gate_later_note), fontSize = 12.sp, color = c.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ContactButtons(support: SupportConfig) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        support.safeTelegram?.let { url -> OutlineBlue(stringResource(R.string.support_telegram)) { ExternalLinks.openTrustedUrl(context, url) } }
        support.safeEmail?.let { email -> OutlineBlue(email) { ExternalLinks.openEmail(context, email) } }
    }
}

// ---------------------------------------------------------------- help & contact

@Composable
fun SupportRoute(onBack: () -> Unit, viewModel: GateViewModel = hiltViewModel()) {
    val support by viewModel.support.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val c = T14Theme.colors
    val privacyUrl = stringResource(R.string.privacy_url)
    val faq = listOf(
        R.string.faq_q1 to R.string.faq_a1,
        R.string.faq_q2 to R.string.faq_a2,
        R.string.faq_q3 to R.string.faq_a3,
        R.string.faq_q4 to R.string.faq_a4,
    )
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("")
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BackButton(onBack)
            Text(stringResource(R.string.support_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
            Caps(stringResource(R.string.support_contact_caps), top = 14)
            Group {
                val tg = support.safeTelegram
                val email = support.safeEmail
                if (tg != null) {
                    ContactRow(
                        "M21 4L3 11l6 2 2 6 3-4 5 4z", if (c.dark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE), if (c.dark) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                        stringResource(R.string.support_telegram_row), "@" + tg.removePrefix("https://t.me/"), divider = email != null,
                    ) { ExternalLinks.openTrustedUrl(context, tg) }
                }
                if (email != null) {
                    ContactRow(
                        "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z M3 8l9 6 9-6", c.divider, c.textBody,
                        "Email", email, divider = false,
                    ) { ExternalLinks.openEmail(context, email) }
                }
                if (tg == null && email == null) {
                    Text(stringResource(R.string.support_none), fontSize = 14.sp, color = c.textSecondary, modifier = Modifier.padding(16.dp))
                }
            }

            Caps(stringResource(R.string.support_group_caps))
            T14Card(Modifier.padding(horizontal = 20.dp), radius = 20.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), spacing = 10.dp) {
                Text(stringResource(R.string.support_group_body), fontSize = 14.sp, lineHeight = 20.sp, color = c.textBody)
                val copy = stringResource(R.string.copy)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.divider)
                        .clickable { clipboard.setText(AnnotatedString(GROUP_EMAIL)) }.semantics { contentDescription = copy }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(GROUP_EMAIL, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    T14Icon("M10.5 8h7A2.5 2.5 0 0 1 20 10.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 8 17.5v-7A2.5 2.5 0 0 1 10.5 8zM16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2", size = 18.dp, tint = Track14Colors.Blue600)
                }
                support.safeGroupUrl?.let { url ->
                    Box(
                        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, Track14Colors.Blue600, RoundedCornerShape(16.dp)).clickable { ExternalLinks.openTrustedUrl(context, url) },
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.support_join_group), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Track14Colors.Blue600) }
                }
            }

            Caps(stringResource(R.string.support_faq_caps))
            Group {
                faq.forEachIndexed { i, (q, a) -> FaqRow(stringResource(q), stringResource(a), divider = i < faq.lastIndex || support.safeFaqUrl != null) }
                support.safeFaqUrl?.let { url ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { ExternalLinks.openTrustedUrl(context, url) }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(stringResource(R.string.support_more_faq), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Track14Colors.Blue600) }
                }
            }

            Caps(stringResource(R.string.support_app_caps))
            Group {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).drawBehind { drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.settings_version_label), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(version, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.textSecondary)
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).drawBehind { drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                        .clickable { ExternalLinks.openStorePage(context, context.packageName) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text(stringResource(R.string.support_check_update), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Track14Colors.Blue600) }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { ExternalLinks.openTrustedUrl(context, privacyUrl) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text(stringResource(R.string.settings_privacy), fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun Caps(text: String, top: Int = 20) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T14Theme.colors.textSecondary, letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = top.dp, bottom = 8.dp),
    )
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    T14Card(Modifier.padding(horizontal = 20.dp), radius = 20.dp, padding = PaddingValues(0.dp), spacing = 0.dp, content = content)
}

@Composable
private fun ContactRow(icon: String, bg: Color, fg: Color, title: String, sub: String, divider: Boolean, onClick: () -> Unit) {
    val c = T14Theme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .then(if (divider) Modifier.drawBehind { drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) } else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) { T14Icon(icon, size = 20.dp, tint = fg) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, fontSize = 12.sp, color = c.textSecondary)
        }
        T14Icon(T14Paths.Chevron, size = 18.dp, tint = c.textTertiary)
    }
}

/** Question row that opens its answer in place. */
@Composable
private fun FaqRow(question: String, answer: String, divider: Boolean) {
    val c = T14Theme.colors
    var open by rememberSaveable(question) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .then(if (divider) Modifier.drawBehind { drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) } else Modifier)
            .clickable { open = !open }.padding(horizontal = 16.dp, vertical = 14.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(question, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            T14Icon("M6 9l6 6 6-6", size = 18.dp, tint = c.textTertiary, modifier = Modifier.rotate(if (open) 180f else 0f))
        }
        if (open) Text(answer, fontSize = 13.sp, lineHeight = 19.sp, color = c.textBody)
    }
}
