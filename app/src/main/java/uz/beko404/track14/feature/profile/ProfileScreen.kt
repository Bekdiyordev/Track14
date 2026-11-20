package uz.beko404.track14.feature.profile

import uz.beko404.track14.feature.common.formatCoins
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.core.designsystem.ScreenTitle
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.Manrope
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import uz.beko404.track14.domain.model.TesterStats
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.feature.auth.AuthUiState
import uz.beko404.track14.feature.auth.AuthViewModel
import uz.beko404.track14.feature.auth.rememberGoogleIdTokenClient
import uz.beko404.track14.feature.messageRes

@Composable
fun ProfileScreen(
    onOpenWallet: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPublished: () -> Unit = {},
    onOpenMyApps: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val balance by profileViewModel.balance.collectAsStateWithLifecycle()
    val stats by profileViewModel.stats.collectAsStateWithLifecycle()
    val extras by profileViewModel.extras.collectAsStateWithLifecycle()
    val telegram by profileViewModel.telegram.collectAsStateWithLifecycle()
    val tokenClient = rememberGoogleIdTokenClient()
    val context = LocalContext.current

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { ScreenTitle(stringResource(R.string.tab_profile)) }
        when (val session = auth.session) {
            null -> item { SkeletonList(count = 2, modifier = Modifier.padding(20.dp)) }
            Session.SignedOut -> item {
                T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
                    Text(stringResource(R.string.profile_signed_out_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.profile_signed_out_hint), style = MaterialTheme.typography.bodyMedium, color = T14Theme.colors.textBody)
                    auth.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    PrimaryButton(stringResource(R.string.sign_in_google), onClick = { authViewModel.signIn { tokenClient.getIdToken() } }, enabled = !auth.isSigningIn)
                }
            }
            is Session.SignedIn -> {
                item { IdentityCard(session, stats.reliability) }
                item {
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(stats.testsCompleted.toString(), stringResource(R.string.profile_stat_completed), "M5 12l5 5 9-10", Color(0xFF1D4ED8), Color(0xFFEFF6FF), Modifier.weight(1f))
                            StatCard(stats.daysPassed.toString(), stringResource(R.string.profile_stat_days), "M3 5h18v16H3zM8 3v4M16 3v4M3 10h18", Color(0xFF0F766E), Color(0xFFCCFBF1), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(formatCoins(extras.earnedTotal), stringResource(R.string.profile_stat_earned), "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 8v8M9.5 10.5h5", Color(0xFFB45309), Color(0xFFFEF3C7), Modifier.weight(1f))
                            StatCard(extras.appsCount.toString(), stringResource(R.string.profile_stat_apps), T14Paths.MyApps, Color(0xFF6D28D9), Color(0xFFEDE9FE), Modifier.weight(1f), onClick = onOpenMyApps)
                        }
                    }
                }
            }
        }
        item {
            val signedIn = auth.session is Session.SignedIn
            T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), padding = PaddingValues(vertical = 4.dp), spacing = 0.dp) {
                if (signedIn) {
                    MenuRow(stringResource(R.string.wallet_title), "M3 7h18v12H3zM16 13h2M3 7l3-3h12l3 3", Color(0xFFFEF3C7), Color(0xFFB45309), onOpenWallet) {
                        balance?.let { Badge(formatCoins(it.value), T14Theme.colors.coinText, T14Theme.colors.coinBg) }
                    }
                    MenuRow(stringResource(R.string.notifications_title), T14Paths.Bell, Color(0xFFEFF6FF), Color(0xFF1D4ED8), onOpenNotifications) {
                        if (extras.unread > 0) Badge(extras.unread.toString(), Color.White, Track14Colors.Danger)
                    }
                }
                MenuRow(stringResource(R.string.profile_menu_published), T14Paths.Star, Color(0xFFDCFCE7), Color(0xFF15803D), onOpenPublished)
                MenuRow(stringResource(R.string.settings_title), "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6zM4 12h2M18 12h2M12 4v2M12 18v2", Color(0xFFF1F5F9), Color(0xFF334155), onOpenSettings)
                MenuRow(stringResource(R.string.support_title), "M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.7M12 17v.5", Color(0xFFF1F5F9), Color(0xFF334155), onOpenSupport)
                telegram?.let { url ->
                    MenuRow(stringResource(R.string.profile_menu_telegram), "M21 4L3 11l6 2 2 6 3-4 5 4z", Color(0xFFE0F2FE), Color(0xFF0369A1), { ExternalLinks.openTrustedUrl(context, url) })
                }
            }
        }
        if (auth.session is Session.SignedIn) {
            item {
                Box(
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = authViewModel::signOut),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.profile_sign_out), color = T14Theme.colors.dangerText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** Blue identity card: reliability ring around the initials, name, e-mail, reliability pill. */
@Composable
private fun IdentityCard(session: Session.SignedIn, reliability: Int?) {
    val shape = RoundedCornerShape(24.dp)
    val name = session.displayName ?: session.email.orEmpty()
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "?" }
    Box(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = Color(0x472563EB), spotColor = Color(0x472563EB))
            .clip(shape)
            .background(Brush.linearGradient(0f to Color(0xFF3B82F6), 0.65f to Color(0xFF1D4ED8), 1f to Color(0xFF1E3A8A))),
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).size(150.dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                val fraction = (reliability ?: 0) / 100f
                Canvas(Modifier.size(76.dp)) {
                    val stroke = 5.dp.toPx() * (76f / 72f)
                    val inset = stroke / 2 + 4f * size.width / 72f
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    drawArc(Color.White, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Box(Modifier.size(56.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Text(initials, color = Color(0xFF1D4ED8), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                session.email?.let { Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                if (reliability != null) {
                    Text(
                        stringResource(R.string.profile_reliability_pill, reliability),
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, icon: String, fg: Color, bg: Color, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val c = T14Theme.colors
    T14Card(modifier, radius = 18.dp, padding = PaddingValues(14.dp), spacing = 4.dp, onClick = onClick) {
        Box(
            Modifier.padding(bottom = 4.dp).size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (c.dark) fg.copy(alpha = 0.18f) else bg),
            contentAlignment = Alignment.Center,
        ) { T14Icon(icon, size = 16.dp, tint = if (c.dark) androidx.compose.ui.graphics.lerp(fg, Color.White, 0.45f) else fg, strokeWidth = 2.2f) }
        Text(value, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text(label, fontSize = 12.sp, color = c.textSecondary)
    }
}

@Composable
private fun MenuRow(title: String, icon: String, iconBg: Color, iconFg: Color, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val c = T14Theme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (c.dark) iconFg.copy(alpha = 0.18f) else iconBg), contentAlignment = Alignment.Center) {
            T14Icon(icon, size = 18.dp, tint = if (c.dark) androidx.compose.ui.graphics.lerp(iconFg, Color.White, 0.45f) else iconFg)
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        trailing()
        T14Icon(T14Paths.Chevron, size = 18.dp, tint = Color(0xFF94A3B8), strokeWidth = 2.2f)
    }
}

@Composable
private fun Badge(text: String, fg: Color, bg: Color) {
    Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp))
}
