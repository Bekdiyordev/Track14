package uz.beko404.track14.feature.appdetail

import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.Coin14Icon
import uz.beko404.track14.core.designsystem.BackButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AnimatedProgress
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.CoinAmount
import uz.beko404.track14.core.designsystem.ProBadge
import uz.beko404.track14.domain.repository.CatalogApp
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.feature.auth.AuthViewModel

@Composable
fun AppDetailRoute(
    onBack: () -> Unit,
    onStartTest: (String) -> Unit,
    onReport: (String) -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    var askSignIn by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val start = { (state as? AppDetailUiState.Content)?.let { onStartTest(it.app.id) }; Unit }

    AppDetailScreen(
        state = state,
        signedIn = auth.session is Session.SignedIn,
        isSigningIn = auth.isSigningIn,
        onBack = onBack,
        onSignIn = { askSignIn = true },
        onStart = start,
        onReport = { (state as? AppDetailUiState.Content)?.let { onReport(it.app.id) } },
    )
    uz.beko404.track14.feature.auth.SignInSheet(
        visible = askSignIn,
        onDismiss = { askSignIn = false },
        onSignedIn = start,
        authViewModel = authViewModel,
    )
}

@Composable
fun AppDetailScreen(
    state: AppDetailUiState,
    signedIn: Boolean,
    isSigningIn: Boolean,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onStart: () -> Unit,
    onReport: () -> Unit,
) {
    val c = T14Theme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            if (signedIn && state is AppDetailUiState.Content) {
                val label = stringResource(R.string.report_title)
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onReport).semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) { T14Icon(T14Paths.Flag, size = 22.dp, tint = c.textBody) }
            }
        }
        when (state) {
            AppDetailUiState.Loading -> SkeletonList(count = 3, modifier = Modifier.padding(20.dp))
            AppDetailUiState.NotFound -> Centered(stringResource(R.string.detail_not_found))
            is AppDetailUiState.Error -> Centered(stringResource(R.string.apps_error))
            is AppDetailUiState.Content -> {
                val app = state.app
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AppTile(app.id, app.name, size = 76.dp, category = app.category)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(app.name, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp)
                            Text(
                                listOf(app.ownerName, app.packageName).filter { it.isNotBlank() }.joinToString(" · "),
                                fontSize = 14.sp, color = c.textBody, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (app.isPro) {
                                    Text(
                                        "PRO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp,
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.listProBg).padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                                if (app.category.isNotBlank()) {
                                    Text(
                                        app.category, color = c.blueSoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.blueSoft).padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBox(stringResource(R.string.testers_progress_short, app.testersJoined, app.testersNeeded), stringResource(R.string.myapps_stat_testers), Modifier.weight(1f))
                        StatBox(stringResource(R.string.detail_days_value), stringResource(R.string.detail_days_label), Modifier.weight(1f))
                        val total = app.rewardPerDay.value * 14 + app.completionBonus.value
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.coinBg).padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(total.toString(), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c.coinText)
                            Text(stringResource(R.string.detail_total_coins), fontSize = 12.sp, color = c.coinText)
                        }
                    }
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.detail_about), style = MaterialTheme.typography.titleMedium)
                        Text(
                            app.shortDesc.ifBlank { stringResource(R.string.detail_no_desc) },
                            fontSize = 15.sp, lineHeight = 23.sp, color = c.textBody,
                        )
                    }
                    T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), radius = 18.dp, padding = PaddingValues(16.dp)) {
                        Text(stringResource(R.string.detail_how), style = MaterialTheme.typography.titleMedium)
                        listOf(R.string.detail_step_1, R.string.detail_step_2, R.string.detail_step_3).forEachIndexed { i, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(c.blueSoft), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", color = c.blueSoftText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(stringResource(step), fontSize = 15.sp, lineHeight = 22.sp, color = c.textBody)
                            }
                        }
                    }
                    if (app.ownerName.isNotBlank()) {
                        T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp), radius = 18.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val initials = app.ownerName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
                                Box(Modifier.size(40.dp).clip(CircleShape).background(if (c.dark) Color(0xFF14264D) else Color(0xFFDBEAFE)), contentAlignment = Alignment.Center) {
                                    Text(initials, color = c.blueSoftText, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(app.ownerName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.detail_owner_sub, app.ownerTests), fontSize = 13.sp, color = c.textSecondary)
                                }
                                app.ownerReliability?.let {
                                    Text(
                                        "$it%", color = c.successText, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.successSoft).padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(20.dp))
                    }
                }
                // Bottom bar: reward on the left, gradient CTA on the right.
                Row(
                    Modifier.fillMaxWidth().background(c.navBar)
                        .drawBehind { drawLine(c.navBorder, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx()) }
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Coin14Icon(18.dp)
                            Text(stringResource(R.string.reward_per_day_long, app.rewardPerDay.value.toInt()).removePrefix("+"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.coinText)
                        }
                        if (app.completionBonus.value > 0) {
                            Text(stringResource(R.string.detail_bonus_end, app.completionBonus.value.toInt()), fontSize = 12.sp, color = c.textSecondary)
                        }
                    }
                    PrimaryButton(
                        text = stringResource(if (signedIn) R.string.detail_start else R.string.detail_sign_in_to_start),
                        onClick = if (signedIn) onStart else onSignIn,
                        enabled = !isSigningIn,
                        height = 56.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier) {
    T14Card(modifier, radius = 16.dp, padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp), spacing = 4.dp) {
        Text(value, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1)
        Text(label, fontSize = 12.sp, color = T14Theme.colors.textSecondary, maxLines = 1)
    }
}

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = T14Theme.colors.textSecondary)
    }
}
