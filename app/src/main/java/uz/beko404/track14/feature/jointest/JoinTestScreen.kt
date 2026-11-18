package uz.beko404.track14.feature.jointest

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
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val groupUrl = stringResource(R.string.testers_group_url)

    LifecycleResumeEffect(Unit) {
        viewModel.refreshUsageAccess()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.steps.startedTestId) {
        if (state.steps.startedTestId != null) onStarted()
    }

    JoinTestScreen(
        state = state,
        onBack = onBack,
        onOpenGroup = { if (ExternalLinks.openTrustedUrl(context, groupUrl)) viewModel.onGroupOpened() },
        onOpenOptIn = { url -> if (ExternalLinks.openOptIn(context, url)) viewModel.onOptInOpened() },
        onOpenStore = { pkg -> if (ExternalLinks.openStorePage(context, pkg)) viewModel.onStoreOpened() },
        onOpenUsageSettings = { ExternalLinks.openUsageAccessSettings(context) },
        onStart = viewModel::start,
    )
}

@Composable
fun JoinTestScreen(
    state: JoinTestUiState,
    onBack: () -> Unit,
    onOpenGroup: () -> Unit,
    onOpenOptIn: (String) -> Unit,
    onOpenStore: (String) -> Unit,
    onOpenUsageSettings: () -> Unit,
    onStart: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(stringResource(R.string.join_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        val app = state.app
        when {
            state.loading -> Centered { CircularProgressIndicator() }
            app == null -> Centered { Text(stringResource(R.string.detail_not_found)) }
            else -> {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.join_subtitle, app.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StepCard(1, R.string.join_step_group_title, R.string.join_step_group_body, state.steps.joinedGroup, R.string.join_step_group_action, onOpenGroup)
                    StepCard(
                        2, R.string.join_step_optin_title, R.string.join_step_optin_body, state.steps.optedIn,
                        R.string.join_step_optin_action, enabled = app.optInUrl.isNotEmpty(),
                    ) { onOpenOptIn(app.optInUrl) }
                    StepCard(
                        3, R.string.join_step_install_title, R.string.join_step_install_body, state.steps.installed,
                        R.string.join_step_install_action, enabled = app.packageName.isNotEmpty(),
                    ) { onOpenStore(app.packageName) }
                    StepCard(4, R.string.join_step_usage_title, R.string.join_step_usage_body, state.steps.usageGranted, R.string.join_step_usage_action, onOpenUsageSettings)
                }
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.steps.error?.let {
                        Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = onStart,
                        enabled = state.canStart,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (state.steps.isStarting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.detail_start))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: Int,
    body: Int,
    done: Boolean,
    action: Int,
    onAction: () -> Unit,
) = StepCard(number, title, body, done, action, enabled = true, onAction = onAction)

@Composable
private fun StepCard(
    number: Int,
    title: Int,
    body: Int,
    done: Boolean,
    action: Int,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    val badge by animateColorAsState(if (done) Track14Colors.Success else MaterialTheme.colorScheme.primary, label = "step")
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(badge),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("$number", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onAction, enabled = enabled, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(if (done) R.string.join_step_again else action))
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}
