package uz.beko404.track14.feature.myapps

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
    viewModel: MyAppsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val tokenClient = rememberGoogleIdTokenClient()

    when (val s = state) {
        MyAppsUiState.Loading -> SkeletonList(count = 3, modifier = Modifier.padding(20.dp))
        MyAppsUiState.SignedOut -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.myapps_signed_out), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { authViewModel.signIn { tokenClient.getIdToken() } }, enabled = !auth.isSigningIn) {
                    Text(stringResource(R.string.sign_in_google))
                }
            }
        }
        is MyAppsUiState.Error -> Centered { Text(stringResource(s.error.messageRes())) }
        is MyAppsUiState.Content -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.tab_my_apps),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = onAddApp, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.myapps_add))
                    }
                }
            }
            if (s.apps.isEmpty()) {
                item { Centered { Text(stringResource(R.string.myapps_empty), style = MaterialTheme.typography.bodyMedium) } }
            }
            items(s.apps, key = { it.id }) { app ->
                OwnerAppCard(
                    app = app,
                    onClick = { if (app.status == AppStatus.DRAFT) onOpenDraft(app.id) else onOpenDashboard(app.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun OwnerAppCard(app: OwnerApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTile(app.id, app.name, size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(app.status)
            }
            val c = app.campaign
            if (c == null) {
                Text(stringResource(R.string.myapps_draft_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AnimatedProgress(if (c.needed > 0) c.joined.toFloat() / c.needed else 1f, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.myapps_stats, c.joined, c.needed, c.completed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { content() }
}
