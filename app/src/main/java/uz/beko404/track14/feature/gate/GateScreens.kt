package uz.beko404.track14.feature.gate

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.domain.model.GateState
import uz.beko404.track14.domain.model.SupportConfig

/** Full-screen states that replace the whole app (no navigation behind them). */
@Composable
fun GateScreen(state: GateState, support: SupportConfig) {
    val context = LocalContext.current
    val openStore = { ExternalLinks.openStorePage(context, context.packageName) }
    when (state) {
        is GateState.UpdateRequired -> StateScreen(
            icon = Icons.Filled.Refresh,
            title = stringResource(R.string.gate_update_title),
            body = state.message.ifBlank { stringResource(R.string.gate_update_body) },
            primary = stringResource(R.string.gate_update_action) to { openStore() },
        )
        is GateState.Maintenance -> StateScreen(
            icon = Icons.Filled.Build,
            title = stringResource(R.string.gate_maintenance_title),
            body = state.message.ifBlank { stringResource(R.string.gate_maintenance_body) },
            support = support,
        )
        is GateState.Blocked -> StateScreen(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.gate_blocked_title),
            body = stringResource(R.string.gate_blocked_body) +
                (state.reason.takeIf { it.isNotBlank() }?.let { "\n\n" + stringResource(R.string.gate_blocked_reason, it) } ?: ""),
            support = support,
            accent = Track14Colors.Danger,
        )
        GateState.Loading, is GateState.Open -> Unit
    }
}

@Composable
fun OptionalUpdateDialog(onUpdate: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.gate_optional_title)) },
        text = { Text(stringResource(R.string.gate_optional_body)) },
        confirmButton = { TextButton(onClick = onUpdate) { Text(stringResource(R.string.gate_update_action)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.gate_later)) } },
    )
}

@Composable
private fun StateScreen(
    icon: ImageVector,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>? = null,
    support: SupportConfig? = null,
    accent: Color = Track14Colors.Blue600,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), accent)), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp)) }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        primary?.let { (label, action) ->
            Button(onClick = action, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text(label) }
        }
        support?.let { ContactButtons(it) }
    }
}

@Composable
fun ContactButtons(support: SupportConfig) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        support.safeTelegram?.let { url ->
            OutlinedButton(onClick = { ExternalLinks.openTrustedUrl(context, url) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Text(stringResource(R.string.support_telegram), modifier = Modifier.padding(start = 8.dp))
            }
        }
        support.safeEmail?.let { email ->
            OutlinedButton(onClick = { ExternalLinks.openEmail(context, email) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.Email, contentDescription = null)
                Text(email, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- help & contact

@Composable
fun SupportRoute(onBack: () -> Unit, viewModel: GateViewModel = hiltViewModel()) {
    val support by viewModel.support.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val faq = listOf(
        R.string.faq_q1 to R.string.faq_a1,
        R.string.faq_q2 to R.string.faq_a2,
        R.string.faq_q3 to R.string.faq_a3,
        R.string.faq_q4 to R.string.faq_a4,
    )
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(stringResource(R.string.support_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            faq.forEach { (q, a) ->
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(q), fontWeight = FontWeight.Bold)
                    Text(stringResource(a), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            support.safeFaqUrl?.let { url ->
                TextButton(onClick = { ExternalLinks.openTrustedUrl(context, url) }) { Text(stringResource(R.string.support_more_faq)) }
            }
            Text(stringResource(R.string.support_contact), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            ContactButtons(support)
            if (support.safeTelegram == null && support.safeEmail == null) {
                Text(stringResource(R.string.support_none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
