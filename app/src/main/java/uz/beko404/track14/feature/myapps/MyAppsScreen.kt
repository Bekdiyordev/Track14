package uz.beko404.track14.feature.myapps

import uz.beko404.track14.core.designsystem.EmptyState
import uz.beko404.track14.feature.common.HeaderBalancePill
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.ScreenTitle
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.Manrope
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AnimatedProgress
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.StatusPill
import uz.beko404.track14.domain.model.AppStatus
import uz.beko404.track14.domain.model.OwnerApp
import uz.beko404.track14.feature.auth.AuthViewModel
import uz.beko404.track14.feature.auth.rememberGoogleIdTokenClient
import uz.beko404.track14.feature.messageRes

@Composable
fun MyAppsRoute(
    onAddApp: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onOpenDashboard: (String) -> Unit,
    onOpenWallet: () -> Unit = {},
    onAppeal: () -> Unit = {},
    viewModel: MyAppsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val tokenClient = rememberGoogleIdTokenClient()
    val signIn = { authViewModel.signIn { tokenClient.getIdToken() } }
    val header: @Composable () -> Unit = { HeaderBalancePill(onWallet = onOpenWallet, onSignIn = signIn) }

    when (val s = state) {
        MyAppsUiState.Loading -> Column {
            ScreenTitle(stringResource(R.string.tab_my_apps)) { header() }
            SkeletonList(count = 3, modifier = Modifier.padding(20.dp))
        }
        MyAppsUiState.SignedOut -> Column {
            ScreenTitle(stringResource(R.string.tab_my_apps)) { header() }
            T14Card(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.myapps_signed_out), style = MaterialTheme.typography.bodyMedium, color = T14Theme.colors.textBody)
                auth.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                PrimaryButton(stringResource(R.string.sign_in_google), onClick = signIn, enabled = !auth.isSigningIn)
            }
        }
        is MyAppsUiState.Error -> Column {
            ScreenTitle(stringResource(R.string.tab_my_apps)) { header() }
            Centered { Text(stringResource(s.error.messageRes()), color = T14Theme.colors.textSecondary) }
        }
        is MyAppsUiState.Content -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { ScreenTitle(stringResource(R.string.tab_my_apps)) { header() } }
            item { AddAppHero(onAddApp) }
            item {
                Text(
                    stringResource(R.string.myapps_list_title, s.apps.size),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
                )
            }
            if (s.apps.isEmpty()) {
                item {
                    EmptyState(
                        T14Paths.MyApps, stringResource(R.string.myapps_empty_title), stringResource(R.string.myapps_empty), Modifier.padding(horizontal = 20.dp),
                        action = stringResource(R.string.myapps_empty_action), onAction = onAddApp,
                    )
                }
            }
            items(s.apps, key = { it.id }) { app ->
                OwnerAppCard(
                    app = app,
                    onClick = { if (app.status == AppStatus.DRAFT) onOpenDraft(app.id) else onOpenDashboard(app.id) },
                    onAppeal = onAppeal,
                    modifier = Modifier.animateItem().padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Blue "Ilova qo'shish" card at the top of the tab. */
@Composable
private fun AddAppHero(onAddApp: () -> Unit) {
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)).background(Track14Colors.Blue600).clickable(onClick = onAddApp).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            T14Icon(T14Paths.Plus, size = 28.dp, tint = Color.White, strokeWidth = 2.2f)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.myapps_add), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(stringResource(R.string.myapps_add_sub), color = Color(0xFFDBEAFE), fontSize = 13.sp, lineHeight = 19.sp)
        }
        T14Icon(T14Paths.Chevron, size = 22.dp, tint = Color.White, strokeWidth = 2.2f)
    }
}

@Composable
private fun OwnerAppCard(app: OwnerApp, onClick: () -> Unit, onAppeal: () -> Unit, modifier: Modifier = Modifier) {
    val c = T14Theme.colors
    val shape = RoundedCornerShape(20.dp)
    val suspended = app.status == AppStatus.SUSPENDED
    val draft = app.status == AppStatus.DRAFT
    val borderColor = when {
        suspended -> c.dangerBorder
        c.dark -> c.cardBorder
        else -> Color(0xFFEDF2F7)
    }
    val dashLine = if (c.dark) Color(0xFF334155) else Color(0xFFCBD5E1)
    Column(
        modifier.fillMaxWidth()
            .shadow(if (c.dark) 0.dp else 6.dp, shape, ambientColor = Color(0x0D0F172A), spotColor = Color(0x140F172A))
            .clip(shape).background(MaterialTheme.colorScheme.surface)
            .then(
                if (draft) Modifier.drawBehind {
                    drawRoundRect(
                        color = dashLine,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                        ),
                    )
                } else Modifier.border(1.dp, borderColor, shape),
            )
            .clickable(enabled = !suspended, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (suspended) 12.dp else 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (draft) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (c.dark) Color(0xFF1E293B) else Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                    Text(app.name.firstOrNull()?.uppercase() ?: "?", color = c.textBody, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                }
            } else {
                AppTile(app.id, app.name, size = 48.dp, category = app.category, modifier = if (suspended) Modifier.alpha(0.55f) else Modifier)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle(app), fontSize = 13.sp, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusTag(app.status)
        }
        val campaign = app.campaign
        if (suspended) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.dangerSoft).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.myapps_suspended_reason, app.suspendedReason.ifBlank { stringResource(R.string.myapps_suspended_no_reason) }),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.dangerText,
                )
                Text(stringResource(R.string.myapps_suspended_body), fontSize = 12.sp, lineHeight = 18.sp, color = c.textBody)
            }
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)).clickable(onClick = onAppeal),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.myapps_appeal), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        } else if (campaign != null && (app.status == AppStatus.ACTIVE || app.status == AppStatus.PAUSED)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(campaign.joined.toString(), stringResource(R.string.myapps_stat_testers), Modifier.weight(1f))
                MiniStat("${campaign.completed} / ${campaign.needed}", stringResource(R.string.myapps_stat_done), Modifier.weight(1f))
                MiniStat(etaText(campaign.startedAtMillis), stringResource(R.string.myapps_stat_eta), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun subtitle(app: OwnerApp): String {
    val campaign = app.campaign
    return when (app.status) {
        AppStatus.DRAFT -> stringResource(R.string.myapps_draft_hint)
        AppStatus.SUSPENDED -> stringResource(R.string.myapps_suspended_sub)
        AppStatus.PUBLISHED -> stringResource(R.string.myapps_published_sub)
        AppStatus.ACTIVE, AppStatus.PAUSED -> {
            val day = campaign?.startedAtMillis?.let { ((System.currentTimeMillis() - it) / 86_400_000L + 1).toInt().coerceIn(1, 14) }
            if (day != null) stringResource(R.string.myapps_campaign_day, day) else stringResource(R.string.myapps_campaign_waiting)
        }
        AppStatus.REMOVED -> stringResource(R.string.status_removed)
    }
}

/** Rough "ready" date: 14 days after the campaign started (the first testers' last day). */
private fun etaText(startedAtMillis: Long?): String {
    val start = startedAtMillis ?: return "—"
    val date = java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.systemDefault()).toLocalDate().plusDays(14)
    return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    val c = T14Theme.colors
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (c.dark) Color(0xFF1B2436) else Color(0xFFF8FAFC)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1)
        Text(label, fontSize = 11.sp, color = c.textSecondary, maxLines = 1)
    }
}

/** Owner status tags exactly as the canvas: dot pill (active/paused), red stop, blue tick, grey draft. */
@Composable
private fun StatusTag(status: AppStatus) {
    val c = T14Theme.colors
    when (status) {
        AppStatus.ACTIVE, AppStatus.PAUSED -> StatusPill(status)
        AppStatus.SUSPENDED -> Row(
            Modifier.clip(RoundedCornerShape(999.dp)).background(if (c.dark) Color(0xFF3B1418) else Color(0xFFFEE2E2)).padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            T14Icon("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M5.6 5.6l12.8 12.8", size = 13.dp, tint = c.dangerText, strokeWidth = 3f)
            Text(stringResource(R.string.status_suspended), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.dangerText)
        }
        AppStatus.PUBLISHED -> Row(
            Modifier.clip(RoundedCornerShape(999.dp)).background(c.blueSoft).padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            T14Icon("M5 12l5 5 9-10", size = 14.dp, tint = c.blueSoftText, strokeWidth = 3f)
            Text(stringResource(R.string.status_published), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.blueSoftText)
        }
        AppStatus.DRAFT, AppStatus.REMOVED -> Text(
            stringResource(if (status == AppStatus.DRAFT) R.string.status_draft else R.string.status_removed),
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textBody,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (c.dark) Color(0xFF1E293B) else Color(0xFFF1F5F9)).padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
internal fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { content() }
}
