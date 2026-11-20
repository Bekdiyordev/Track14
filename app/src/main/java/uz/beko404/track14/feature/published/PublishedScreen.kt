package uz.beko404.track14.feature.published

import uz.beko404.track14.core.designsystem.EmptyState
import androidx.compose.foundation.layout.Spacer
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.BackButton
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Icon
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
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
    val c = T14Theme.colors

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BackButton(onBack)
            Text(stringResource(R.string.published_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1)
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { TotalsCard(totals) }
            item { Spacer(Modifier.height(8.dp)) }
            val list = apps
            when {
                list == null -> item { SkeletonList(count = 4) }
                list.isEmpty() -> item {
                    EmptyState(T14Paths.Star, stringResource(R.string.published_empty_title), stringResource(R.string.published_empty))
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
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = Color(0x472563EB), spotColor = Color(0x472563EB))
            .clip(shape)
            .background(Brush.linearGradient(0f to Color(0xFF3B82F6), 0.7f to Color(0xFF1D4ED8), 1f to Color(0xFF1E3A8A)))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Total(totals.count.toString(), stringResource(R.string.published_count), Modifier.weight(1f))
        Total(uz.beko404.track14.feature.common.formatCoins(totals.testers.toLong()), stringResource(R.string.published_testers), Modifier.weight(1f))
    }
}

@Composable
private fun Total(value: String, label: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

@Composable
private fun PublishedCard(app: PublishedApp, onOpenStore: () -> Unit, modifier: Modifier = Modifier) {
    val c = T14Theme.colors
    T14Card(modifier, radius = 20.dp, padding = PaddingValues(14.dp), spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTile(app.id, app.name, size = 52.dp, category = app.category)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                val date = if (app.publishedAtMillis > 0) {
                    java.time.Instant.ofEpochMilli(app.publishedAtMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("d-MMM"))
                } else {
                    null
                }
                Text(
                    listOfNotNull(app.category.ifBlank { null }, date?.let { stringResource(R.string.published_on, it) }).joinToString(" · "),
                    fontSize = 12.sp, color = c.textSecondary, maxLines = 1,
                )
            }
            app.rating?.let {
                Text(
                    "★ " + "%.1f".format(it), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.divider).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(app.testers.toString(), stringResource(R.string.published_stat_testers), Modifier.weight(1f))
            Stat(app.completed.toString(), stringResource(R.string.published_stat_completed), Modifier.weight(1f))
            Stat(app.feedbackCount.toString(), stringResource(R.string.published_stat_feedback), Modifier.weight(1f))
        }
        if (app.packageName.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (c.dark) Color(0xFFF1F5F9) else Color(0xFF0F172A)).clickable(onClick = onOpenStore),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fg = if (c.dark) Color(0xFF0B1220) else Color.White
                T14Icon("M5 3l14 9-14 9z", size = 18.dp, tint = fg)
                Text(stringResource(R.string.published_open_play), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg)
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    val c = T14Theme.colors
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (c.dark) Color(0xFF0F172A) else Color(0xFFF8FAFC)).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(value, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = c.textSecondary)
    }
}

/** Home banner: "N apps shipped through Track14". */
@Composable
fun PublishedBanner(totals: PublishedTotals, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier.fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = Color(0x380F172A), spotColor = Color(0x380F172A))
            .clip(shape)
            .background(Brush.linearGradient(T14Theme.colors.bannerBrush))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { T14Icon(T14Paths.Star, size = 22.dp, tint = Color.White) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.published_banner, totals.count), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(stringResource(R.string.published_banner_sub), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
        T14Icon(T14Paths.Chevron, size = 18.dp, tint = Color.White, strokeWidth = 2.2f)
    }
}
