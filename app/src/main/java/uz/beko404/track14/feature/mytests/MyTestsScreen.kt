package uz.beko404.track14.feature.mytests

import uz.beko404.track14.feature.common.HeaderBalancePill
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.core.designsystem.accentFor
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.ScreenTitle
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.Manrope
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.CoinAmount
import uz.beko404.track14.core.designsystem.DayDots
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.core.notifications.Channels
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.model.TesterTest
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.feature.auth.AuthViewModel
import uz.beko404.track14.feature.auth.rememberGoogleIdTokenClient
import uz.beko404.track14.feature.messageRes

@Composable
fun MyTestsRoute(
    onFeedback: (String) -> Unit,
    onOpenTest: (String) -> Unit = {},
    onOpenWallet: () -> Unit = {},
    viewModel: MyTestsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tokenClient = rememberGoogleIdTokenClient()
    val snackbar = remember { SnackbarHostState() }
    var leaving by remember { mutableStateOf<TesterTest?>(null) }

    // Ask once for notification permission when there is something to remind about (Android 13+).
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) {
        val hasTests = (state as? MyTestsUiState.Content)?.active?.isNotEmpty() == true
        if (hasTests && !asked && Build.VERSION.SDK_INT >= 33 && !Channels.canPost(context)) {
            asked = true
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LifecycleResumeEffect(state is MyTestsUiState.Content) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    val res = context.resources
    LaunchedEffect(Unit) {
        viewModel.messages.collect { event ->
            val text = when (event) {
                is MyTestsEvent.DayPassed -> if (event.completed) {
                    res.getString(R.string.mytests_completed, event.appName)
                } else {
                    res.getString(R.string.mytests_day_passed, event.appName, event.day)
                }
                is MyTestsEvent.NotEnough -> res.getString(R.string.mytests_not_enough, event.appName, event.seconds)
                is MyTestsEvent.Failed -> res.getString(event.error.messageRes())
                is MyTestsEvent.Left -> res.getString(R.string.mytests_left, event.appName)
            }
            snackbar.showSnackbar(text)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MyTestsScreen(
            state = state,
            isSigningIn = auth.isSigningIn,
            signInError = auth.error,
            onSignIn = { authViewModel.signIn { tokenClient.getIdToken() } },
            onOpenApp = { test ->
                viewModel.onOpenApp(test)
                if (!ExternalLinks.openApp(context, test.packageName)) ExternalLinks.openStorePage(context, test.packageName)
            },
            onGrantUsage = { ExternalLinks.openUsageAccessSettings(context) },
            onLeave = { leaving = it },
            onFeedback = { onFeedback(it.id) },
            onOpenTest = { onOpenTest(it.id) },
            header = { HeaderBalancePill(onWallet = onOpenWallet, onSignIn = { authViewModel.signIn { tokenClient.getIdToken() } }) },
        )
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    leaving?.let { test ->
        AlertDialog(
            onDismissRequest = { leaving = null },
            title = { Text(stringResource(R.string.mytests_leave_title)) },
            text = { Text(stringResource(R.string.mytests_leave_body, test.appName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.leave(test); leaving = null }) {
                    Text(stringResource(R.string.mytests_leave_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { leaving = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun MyTestsScreen(
    state: MyTestsUiState,
    isSigningIn: Boolean,
    onSignIn: () -> Unit,
    onOpenApp: (TesterTest) -> Unit,
    onGrantUsage: () -> Unit,
    onLeave: (TesterTest) -> Unit,
    onFeedback: (TesterTest) -> Unit,
    onOpenTest: (TesterTest) -> Unit = {},
    signInError: DomainError? = null,
    header: @Composable () -> Unit = {},
) {
    when (state) {
        MyTestsUiState.Loading -> Column {
            ScreenTitle(stringResource(R.string.tab_my_tests)) { header() }
            SkeletonList(count = 3, withDays = true, modifier = Modifier.padding(20.dp))
        }
        MyTestsUiState.SignedOut -> Column {
            ScreenTitle(stringResource(R.string.tab_my_tests)) { header() }
            SignedOutCard(isSigningIn, signInError, onSignIn)
        }
        is MyTestsUiState.Error -> Column {
            ScreenTitle(stringResource(R.string.tab_my_tests)) { header() }
            Centered { Text(stringResource(state.error.messageRes()), color = T14Theme.colors.textSecondary) }
        }
        is MyTestsUiState.Content -> {
            var showFinished by rememberSaveable { mutableStateOf(false) }
            val todayTests = state.active.filter { it.dayNumber(state.nowMillis) in 1..14 }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
                item { ScreenTitle(stringResource(R.string.tab_my_tests)) { header() } }
                if (state.active.isEmpty() && state.finished.isEmpty()) {
                    item { EmptyCard() }
                }
                if (todayTests.isNotEmpty()) {
                    item { TodayCard(todayTests, state.nowMillis) }
                }
                if (!state.usageGranted && state.active.isNotEmpty()) {
                    item { UsageBanner(onGrantUsage) }
                }
                if (todayTests.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.mytests_today_section),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
                        )
                    }
                    items(todayTests, key = { "task_" + it.id }) { test ->
                        TaskRow(test, state.nowMillis, onOpenApp, Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
                    }
                }
                if (state.active.isNotEmpty()) {
                    item { ActiveHeader() }
                    items(state.active, key = { it.id }) { test ->
                        TestCard(test, state.nowMillis, onOpenTest, Modifier.animateItem().padding(horizontal = 20.dp, vertical = 5.dp))
                    }
                }
                if (state.finished.isNotEmpty()) {
                    item {
                        DashedButton(
                            stringResource(R.string.mytests_finished_count, state.finished.size),
                            onClick = { showFinished = !showFinished },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                    if (showFinished) {
                        items(state.finished, key = { it.id }) { test ->
                            TestCard(test, state.nowMillis, onOpenTest, Modifier.animateItem().padding(horizontal = 20.dp, vertical = 5.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Blue "Bugungi vazifalar" card: done / total, one bar per test, coins still to earn, time left today. */
@Composable
private fun TodayCard(tests: List<TesterTest>, nowMillis: Long) {
    val done = tests.count { it.isDoneToday(nowMillis) }
    val left = tests.filterNot { it.isDoneToday(nowMillis) }.sumOf { it.rewardPerDay.value }
    val soft = Color(0xFFDBEAFE)
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)).background(Track14Colors.Blue600).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.mytests_today), color = soft, fontSize = 13.sp)
                Text("$done / ${tests.size}", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
            }
            Text(
                if (left > 0) stringResource(R.string.mytests_left_coins, left.toInt()) else stringResource(R.string.mytests_all_done),
                color = soft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tests.forEach { test ->
                Box(
                    Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp))
                        .background(if (test.isDoneToday(nowMillis)) Color.White else Color.White.copy(alpha = 0.3f)),
                )
            }
        }
        val (h, m) = timeLeftToday(nowMillis)
        Text(stringResource(R.string.mytests_time_left, h, m), color = soft, fontSize = 13.sp)
    }
}

/** Hours and minutes until local midnight. */
private fun timeLeftToday(nowMillis: Long): Pair<Int, Int> {
    val zone = java.time.ZoneId.systemDefault()
    val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone)
    val minutes = java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(zone)).toMinutes().toInt()
    return minutes / 60 to minutes % 60
}

/** "Bugun" row: letter tile, name, reward or result, and "Ochish" or a green tick. */
@Composable
private fun TaskRow(test: TesterTest, nowMillis: Long, onOpenApp: (TesterTest) -> Unit, modifier: Modifier) {
    val c = T14Theme.colors
    val done = test.isDoneToday(nowMillis)
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth()
            .shadow(if (c.dark) 0.dp else 6.dp, shape, ambientColor = Color(0x0D0F172A), spotColor = Color(0x140F172A))
            .clip(shape).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (done) c.successSoft else if (c.dark) c.cardBorder else Color(0xFFEDF2F7), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LetterTile(test.appId.ifEmpty { test.id }, test.appName, 44.dp, 12.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(test.appName, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (done) stringResource(R.string.mytests_task_done, test.rewardPerDay.value.toInt())
                else stringResource(R.string.mytests_task_pending, test.rewardPerDay.value.toInt()),
                fontSize = 13.sp,
                color = if (done) c.successText else c.textSecondary,
            )
        }
        if (done) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.successSoft), contentAlignment = Alignment.Center) {
                T14Icon("M5 12l5 5 9-10", size = 20.dp, tint = c.successText, strokeWidth = 2.8f)
            }
        } else {
            Box(
                Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(Track14Colors.Blue600)
                    .clickable(enabled = test.packageName.isNotEmpty()) { onOpenApp(test) }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.mytests_open), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun ActiveHeader() {
    val c = T14Theme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.mytests_active), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Legend(Track14Colors.Success, stringResource(R.string.legend_done))
            Legend(Track14Colors.Danger, stringResource(R.string.legend_missed))
            Legend(Color(0xFFF59E0B), stringResource(R.string.legend_today))
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, fontSize = 12.sp, color = T14Theme.colors.textBody)
    }
}

/** Active test card: small tile, "Kun d / 14", coins earned, 14 day cells (today in amber). */
@Composable
private fun TestCard(test: TesterTest, nowMillis: Long, onOpen: (TesterTest) -> Unit, modifier: Modifier = Modifier) {
    val c = T14Theme.colors
    val today = test.dayNumber(nowMillis)?.takeIf { test.status == TestStatus.ACTIVE }
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth()
            .shadow(if (c.dark) 0.dp else 6.dp, shape, ambientColor = Color(0x0D0F172A), spotColor = Color(0x140F172A))
            .clip(shape).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (c.dark) c.cardBorder else Color(0xFFEDF2F7), shape)
            .clickable { onOpen(test) }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LetterTile(test.appId.ifEmpty { test.id }, test.appName, 36.dp, 10.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(test.appName, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(statusLine(test, today, nowMillis), fontSize = 12.sp, color = statusColor(test, nowMillis))
            }
            Text("+${test.daysPassed * test.rewardPerDay.value}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.coinText)
        }
        val desc = stringResource(R.string.mytests_days_description, test.daysPassed)
        Row(Modifier.fillMaxWidth().semantics { contentDescription = desc }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            test.days.forEachIndexed { i, status ->
                val color = when {
                    i + 1 == today && status == DayStatus.PENDING -> Color(0xFFF59E0B)
                    status == DayStatus.PASSED -> Track14Colors.Success
                    status == DayStatus.MISSED -> Track14Colors.Danger
                    else -> if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0)
                }
                Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(5.dp)).background(color))
            }
        }
    }
}

/** Solid letter tile used where the canvas shows the app's initial (Testlarim). */
@Composable
private fun LetterTile(id: String, name: String, size: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(accentFor(id)), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.44f).sp)
    }
}

@Composable
private fun UsageBanner(onGrantUsage: () -> Unit) {
    val c = T14Theme.colors
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(c.amberSoft).border(1.dp, c.amberBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onGrantUsage).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        T14Icon(T14Paths.Info, size = 22.dp, tint = c.amberText)
        Text(stringResource(R.string.mytests_usage_hint), fontSize = 13.sp, color = c.amberText, modifier = Modifier.weight(1f))
        T14Icon(T14Paths.Chevron, size = 18.dp, tint = c.amberText)
    }
}

@Composable
private fun SignedOutCard(isSigningIn: Boolean, error: DomainError?, onSignIn: () -> Unit) {
    T14Card(Modifier.padding(20.dp)) {
        Text(stringResource(R.string.mytests_signed_out), style = MaterialTheme.typography.bodyMedium, color = T14Theme.colors.textBody)
        error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PrimaryButton(stringResource(R.string.sign_in_google), onClick = onSignIn, enabled = !isSigningIn)
    }
}

@Composable
private fun EmptyCard() {
    T14Card(Modifier.padding(20.dp)) {
        Text(stringResource(R.string.mytests_empty), style = MaterialTheme.typography.bodyMedium, color = T14Theme.colors.textBody)
    }
}

/** 48dp outline button with a dashed border, as for "Tugagan testlar". */
@Composable
private fun DashedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = T14Theme.colors
    val line = if (c.dark) Color(0xFF334155) else Color(0xFFCBD5E1)
    Box(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
            .drawBehind {
                drawRoundRect(
                    color = line,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.textBody) }
}

@Composable
private fun statusLine(test: TesterTest, today: Int?, nowMillis: Long): String = when (test.status) {
    TestStatus.COMPLETED -> stringResource(R.string.mytests_status_completed)
    TestStatus.FAILED -> stringResource(R.string.mytests_status_failed)
    TestStatus.LEFT -> stringResource(R.string.mytests_status_left)
    else -> if (today == null) stringResource(R.string.mytests_status_starting) else stringResource(R.string.testdetail_day_of, today)
}

@Composable
private fun statusColor(test: TesterTest, nowMillis: Long): Color = when (test.status) {
    TestStatus.FAILED -> MaterialTheme.colorScheme.error
    TestStatus.COMPLETED -> T14Theme.colors.successText
    else -> T14Theme.colors.textSecondary
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { content() }
}
