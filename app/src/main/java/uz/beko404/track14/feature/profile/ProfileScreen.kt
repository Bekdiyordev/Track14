package uz.beko404.track14.feature.profile

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
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val balance by profileViewModel.balance.collectAsStateWithLifecycle()
    val stats by profileViewModel.stats.collectAsStateWithLifecycle()
    val tokenClient = rememberGoogleIdTokenClient()

    ProfileContent(
        auth = auth,
        balance = balance,
        onSignIn = { authViewModel.signIn { tokenClient.getIdToken() } },
        onSignOut = authViewModel::signOut,
        stats = stats,
        onOpenWallet = onOpenWallet,
        onOpenNotifications = onOpenNotifications,
        onOpenSupport = onOpenSupport,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun ProfileContent(
    auth: AuthUiState,
    balance: Coins?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    stats: TesterStats,
    onOpenWallet: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val session = auth.session) {
            null -> CircularProgressIndicator()
            Session.SignedOut -> SignedOut(auth, onSignIn)
            is Session.SignedIn -> SignedIn(session, balance, stats, onSignOut, onOpenWallet, onOpenNotifications)
        }
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings_title)) }
        TextButton(onClick = onOpenSupport) { Text(stringResource(R.string.support_title)) }
    }
}

@Composable
private fun SignedOut(auth: AuthUiState, onSignIn: () -> Unit) {
    Text(stringResource(R.string.profile_signed_out_title), style = MaterialTheme.typography.headlineSmall)
    Text(
        stringResource(R.string.profile_signed_out_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    auth.error?.let { error ->
        Text(
            stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (auth.isSigningIn) {
        CircularProgressIndicator()
    } else {
        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sign_in_google))
        }
    }
}

@Composable
private fun SignedIn(
    session: Session.SignedIn,
    balance: Coins?,
    stats: TesterStats,
    onSignOut: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    Text(session.displayName ?: session.email.orEmpty(), style = MaterialTheme.typography.headlineSmall)
    Text(
        stringResource(R.string.profile_balance),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("${balance?.value ?: 0} Coin14", style = MaterialTheme.typography.headlineMedium)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(stats.testsCompleted.toString(), stringResource(R.string.profile_stat_completed), Modifier.weight(1f))
        StatTile(stats.daysPassed.toString(), stringResource(R.string.profile_stat_days), Modifier.weight(1f))
        StatTile(stats.reliability?.let { "$it%" } ?: "—", stringResource(R.string.profile_stat_reliability), Modifier.weight(1f))
    }
    Button(onClick = onOpenWallet, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.wallet_title))
    }
    OutlinedButton(onClick = onOpenNotifications, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.notifications_title))
    }
    OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.sign_out))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
