package uz.beko404.track14.feature.appdetail

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
import uz.beko404.track14.feature.auth.rememberGoogleIdTokenClient

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
    val tokenClient = rememberGoogleIdTokenClient()

    AppDetailScreen(
        state = state,
        signedIn = auth.session is Session.SignedIn,
        isSigningIn = auth.isSigningIn,
        onBack = onBack,
        onSignIn = { authViewModel.signIn { tokenClient.getIdToken() } },
        onStart = { (state as? AppDetailUiState.Content)?.let { onStartTest(it.app.id) } },
        onReport = { (state as? AppDetailUiState.Content)?.let { onReport(it.app.id) } },
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
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
        }
        when (state) {
            AppDetailUiState.Loading -> Centered { CircularProgressIndicator() }
            AppDetailUiState.NotFound -> Centered { Text(stringResource(R.string.detail_not_found)) }
            is AppDetailUiState.Error -> Centered { Text(stringResource(R.string.apps_error)) }
            is AppDetailUiState.Content -> {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Header(state.app)
                    Stats(state.app)
                    Section(stringResource(R.string.detail_about)) {
                        Text(
                            state.app.shortDesc.ifBlank { stringResource(R.string.detail_no_desc) },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Section(stringResource(R.string.detail_how)) {
                        listOf(R.string.detail_step_1, R.string.detail_step_2, R.string.detail_step_3)
                            .forEachIndexed { index, step ->
                                Text("${index + 1}. ${stringResource(step)}", style = MaterialTheme.typography.bodyMedium)
                            }
                    }
                    if (signedIn) {
                        TextButton(onClick = onReport) {
                            Text(stringResource(R.string.report_title), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = if (signedIn) onStart else onSignIn,
                        enabled = !isSigningIn,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(if (signedIn) R.string.detail_start else R.string.detail_sign_in_to_start))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(app: CatalogApp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AppTile(app.id, app.name, size = 72.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(app.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                if (app.isPro) ProBadge()
            }
            Text(app.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Stats(app: CatalogApp) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(stringResource(R.string.detail_testers), Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.testers_progress, app.testersJoined, app.testersNeeded),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Stat(stringResource(R.string.detail_reward), Modifier.weight(1f)) {
                    CoinAmount(stringResource(R.string.reward_per_day, app.rewardPerDay.value.toInt()))
                }
                Stat(stringResource(R.string.detail_duration), Modifier.weight(1f)) {
                    Text(stringResource(R.string.detail_duration_value), fontWeight = FontWeight.Bold)
                }
            }
            AnimatedProgress(app.testersProgress, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Stat(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        value()
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        content()
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}
