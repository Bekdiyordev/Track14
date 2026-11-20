package uz.beko404.track14.feature.mytests

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
) {
    when (state) {
        MyTestsUiState.Loading -> SkeletonList(count = 3, withDays = true, modifier = Modifier.padding(20.dp))
        MyTestsUiState.SignedOut -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.mytests_signed_out), style = MaterialTheme.typography.bodyLarge)
                signInError?.let { error ->
                    Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onSignIn, enabled = !isSigningIn) { Text(stringResource(R.string.sign_in_google)) }
            }
        }
        is MyTestsUiState.Error -> Centered { Text(stringResource(state.error.messageRes())) }
        is MyTestsUiState.Content -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.tab_my_tests), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            }
            if (state.active.isEmpty() && state.finished.isEmpty()) {
                item { Centered { Text(stringResource(R.string.mytests_empty)) } }
            }
            if (state.active.isNotEmpty()) {
                item { TodayCard(state) }
                if (!state.usageGranted) item { UsageBanner(onGrantUsage) }
                items(state.active, key = { it.id }) { test ->
                    TestCard(test, state.nowMillis, onOpenApp, onLeave, onFeedback, onOpenTest, Modifier.animateItem())
                }
            }
            if (state.finished.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.mytests_finished), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
                items(state.finished, key = { it.id }) { test ->
                    TestCard(test, state.nowMillis, onOpenApp, onLeave, onFeedback, onOpenTest, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun TodayCard(state: MyTestsUiState.Content) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Track14Colors.Blue500, Track14Colors.Blue700)))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.mytests_today), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        Text(
            "${state.doneToday} / ${state.active.size}",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.active.forEach { test ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (test.isDoneToday(state.nowMillis)) Color.White else Color.White.copy(alpha = 0.28f)),
                )
            }
        }
    }
}

@Composable
private fun UsageBanner(onGrantUsage: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.mytests_usage_hint), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onGrantUsage) { Text(stringResource(R.string.join_step_usage_action)) }
        }
    }
}

@Composable
private fun TestCard(
    test: TesterTest,
    nowMillis: Long,
    onOpenApp: (TesterTest) -> Unit,
    onLeave: (TesterTest) -> Unit,
    onFeedback: (TesterTest) -> Unit,
    onOpen: (TesterTest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = test.dayNumber(nowMillis)
    val active = test.status == TestStatus.ACTIVE
    Card(
        onClick = { onOpen(test) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTile(test.appId.ifEmpty { test.id }, test.appName, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(test.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(statusLine(test, today, nowMillis), style = MaterialTheme.typography.bodySmall, color = statusColor(test, nowMillis))
                }
                CoinAmount(stringResource(R.string.reward_per_day, test.rewardPerDay.value.toInt()))
            }
            DayDots(
                days = test.days,
                today = today?.takeIf { active },
                description = stringResource(R.string.mytests_days_description, test.daysPassed),
                modifier = Modifier.fillMaxWidth(),
            )
            if (active || test.status == TestStatus.COMPLETED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onFeedback(test) }) {
                        Text(stringResource(if (test.feedbackRewarded) R.string.mytests_feedback else R.string.mytests_feedback_reward))
                    }
                }
            }
            if (active) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (test.needsCheckToday(nowMillis)) {
                        Button(onClick = { onOpenApp(test) }, shape = RoundedCornerShape(12.dp), enabled = test.packageName.isNotEmpty()) {
                            Text(stringResource(R.string.mytests_open))
                        }
                    }
                    TextButton(onClick = { onLeave(test) }) {
                        Text(stringResource(R.string.mytests_leave), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLine(test: TesterTest, today: Int?, nowMillis: Long): String = when (test.status) {
    TestStatus.COMPLETED -> stringResource(R.string.mytests_status_completed)
    TestStatus.FAILED -> stringResource(R.string.mytests_status_failed)
    TestStatus.LEFT -> stringResource(R.string.mytests_status_left)
    else -> when {
        today == null -> stringResource(R.string.mytests_status_starting)
        test.isDoneToday(nowMillis) -> stringResource(R.string.mytests_status_done_today, today)
        else -> stringResource(R.string.mytests_status_open_today, today)
    }
}

@Composable
private fun statusColor(test: TesterTest, nowMillis: Long): Color = when {
    test.status == TestStatus.FAILED -> MaterialTheme.colorScheme.error
    test.status == TestStatus.COMPLETED || test.isDoneToday(nowMillis) -> Track14Colors.Success
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { content() }
}
