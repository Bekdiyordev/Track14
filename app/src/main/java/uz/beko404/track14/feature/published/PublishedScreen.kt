package uz.beko404.track14.feature.published

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.domain.model.PublishedApp
import uz.beko404.track14.domain.model.PublishedTotals
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.PublishedRepository
import javax.inject.Inject

@HiltViewModel
class PublishedViewModel @Inject constructor(repository: PublishedRepository) : ViewModel() {
    val apps: StateFlow<List<PublishedApp>?> = repository.observeApps()
        .map { (it as? DomainResult.Success)?.value.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val totals: StateFlow<PublishedTotals> = repository.observeTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PublishedTotals())
}

@Composable
fun PublishedRoute(onBack: () -> Unit, viewModel: PublishedViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(stringResource(R.string.published_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { TotalsCard(totals) }
            val list = apps
            when {
                list == null -> item { SkeletonList(count = 4) }
                list.isEmpty() -> item {
                    Text(stringResource(R.string.published_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                else -> items(list, key = { it.id }) { app ->
                    PublishedCard(app, onOpenStore = { ExternalLinks.openStorePage(context, app.packageName) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(totals: PublishedTotals) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Track14Colors.Blue500, Track14Colors.Blue700, Color(0xFF1E3A8A))), RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Total(totals.count.toString(), stringResource(R.string.published_count), Modifier.weight(1f))
        Total(totals.testers.toString(), stringResource(R.string.published_testers), Modifier.weight(1f))
    }
}

@Composable
private fun Total(value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(value, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PublishedCard(app: PublishedApp, onOpenStore: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTile(app.id, app.name, size = 52.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(app.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            app.rating?.let { Text("★ %.1f".format(it), fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(app.testers.toString(), stringResource(R.string.published_stat_testers), Modifier.weight(1f))
            Stat(app.completed.toString(), stringResource(R.string.published_stat_completed), Modifier.weight(1f))
            Stat(app.feedbackCount.toString(), stringResource(R.string.published_stat_feedback), Modifier.weight(1f))
        }
        if (app.packageName.isNotEmpty()) {
            Button(
                onClick = onOpenStore,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Track14Colors.Navy, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.published_open_play), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(value, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Home banner: "N apps shipped through Track14". */
@Composable
fun PublishedBanner(totals: PublishedTotals, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Track14Colors.Navy, contentColor = Color.White),
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.published_banner, totals.count), fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.published_banner_sub), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}
