package uz.beko404.track14.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AnimatedProgress
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.CoinAmount
import uz.beko404.track14.core.designsystem.ProBadge
import uz.beko404.track14.core.designsystem.accentBrush
import uz.beko404.track14.domain.model.PublishedTotals
import uz.beko404.track14.domain.repository.CatalogApp
import uz.beko404.track14.feature.published.PublishedBanner
import uz.beko404.track14.domain.rules.CatalogFilter
import uz.beko404.track14.domain.rules.CatalogSort

@Composable
fun AppsRoute(onOpenApp: (String) -> Unit, onOpenPublished: () -> Unit, viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val totals by viewModel.publishedTotals.collectAsStateWithLifecycle()
    AppsScreen(
        publishedCount = totals.count,
        onOpenPublished = onOpenPublished,
        state = state,
        filter = filter,
        onQueryChange = viewModel::onQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onSortSelected = viewModel::onSortSelected,
        onOpenApp = onOpenApp,
    )
}

@Composable
fun AppsScreen(
    publishedCount: Int,
    onOpenPublished: () -> Unit,
    state: AppsUiState,
    filter: CatalogFilter,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSortSelected: (CatalogSort) -> Unit,
    onOpenApp: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(
                stringResource(R.string.apps_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            )
        }
        item { SearchField(filter.query, onQueryChange) }
        item { SortChips(filter.sort, onSortSelected) }
        if (state.categories.isNotEmpty()) {
            item { CategoryChips(state.categories, filter.category, onCategorySelected) }
        }
        when {
            state.isLoading -> item { CenteredMessage { CircularProgressIndicator() } }
            state.error != null -> item { CenteredMessage { Text(stringResource(R.string.apps_error)) } }
            state.totalCount == 0 -> item { CenteredMessage { Text(stringResource(R.string.apps_empty)) } }
            else -> {
                if (state.featured.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.apps_featured)) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.featured, key = { it.id }) { FeaturedCard(it, onOpenApp) }
                        }
                    }
                }
                if (publishedCount > 0 && filter.isBrowsing) {
                    item {
                        PublishedBanner(
                            totals = PublishedTotals(count = publishedCount),
                            onClick = onOpenPublished,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
                        )
                    }
                }
                item {
                    SectionTitle(
                        stringResource(R.string.apps_waiting),
                        trailing = stringResource(R.string.apps_count, state.apps.size),
                    )
                }
                if (state.apps.isEmpty()) {
                    item { CenteredMessage { Text(stringResource(R.string.apps_no_results)) } }
                } else {
                    items(state.apps, key = { it.id }) { app ->
                        AppRow(
                            app = app,
                            onOpenApp = onOpenApp,
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        placeholder = { Text(stringResource(R.string.apps_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.apps_search_clear))
                }
            }
        },
    )
}

@Composable
private fun SortChips(selected: CatalogSort, onSortSelected: (CatalogSort) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CatalogSort.entries) { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSortSelected(sort) },
                label = { Text(stringResource(sort.label())) },
            )
        }
    }
}

@Composable
private fun CategoryChips(categories: List<String>, selected: String?, onCategorySelected: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.apps_category_all)) },
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onCategorySelected(if (category == selected) null else category) },
                label = { Text(category) },
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeaturedCard(app: CatalogApp, onOpenApp: (String) -> Unit) {
    Box(
        modifier = Modifier
            .width(276.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(accentBrush(app.id))
            .clickable { onOpenApp(app.id) }
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(app.name.first().uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.name, color = Color.White, fontWeight = FontWeight.ExtraBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(app.category, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodySmall)
                }
                if (app.isPro) ProBadge()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CoinAmount(
                    text = stringResource(R.string.reward_per_day, app.rewardPerDay.value.toInt()),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
                Text(
                    stringResource(R.string.testers_progress, app.testersJoined, app.testersNeeded),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: CatalogApp, onOpenApp: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = { onOpenApp(app.id) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTile(app.id, app.name, size = 52.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                app.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (app.isPro) ProBadge()
                        }
                        Text(app.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CoinAmount(stringResource(R.string.reward_per_day, app.rewardPerDay.value.toInt()))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnimatedProgress(app.testersProgress, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.testers_progress, app.testersJoined, app.testersNeeded),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { content() }
}

private fun CatalogSort.label(): Int = when (this) {
    CatalogSort.NEWEST -> R.string.apps_sort_newest
    CatalogSort.RATING -> R.string.apps_sort_rating
    CatalogSort.FEW_TESTERS -> R.string.apps_sort_few_testers
}
