package uz.beko404.track14.feature.apps

import uz.beko404.track14.core.designsystem.EmptyTone
import uz.beko404.track14.core.designsystem.EmptyIcons
import uz.beko404.track14.core.designsystem.EmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AnimatedProgress
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.Categories
import uz.beko404.track14.core.designsystem.Coin14Icon
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.ProBadge
import uz.beko404.track14.core.designsystem.ScreenTitle
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.core.designsystem.T14Chip
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14SearchField
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.featuredBrush
import uz.beko404.track14.domain.model.PublishedTotals
import uz.beko404.track14.domain.repository.CatalogApp
import uz.beko404.track14.domain.rules.CatalogFilter
import uz.beko404.track14.domain.rules.CatalogSort
import uz.beko404.track14.feature.common.HeaderBalancePill
import uz.beko404.track14.feature.published.PublishedBanner

@Composable
fun AppsRoute(
    onOpenApp: (String) -> Unit,
    onOpenPublished: () -> Unit,
    onOpenWallet: () -> Unit = {},
    onSignIn: () -> Unit = {},
    viewModel: AppsViewModel = hiltViewModel(),
) {
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
        header = { HeaderBalancePill(onWallet = onOpenWallet, onSignIn = onSignIn) },
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
    header: @Composable () -> Unit = {},
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { ScreenTitle(stringResource(R.string.apps_title)) { header() } }
        item {
            T14SearchField(
                value = filter.query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.apps_search_hint),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            )
        }
        item { FilterChips(state.categories, filter, onCategorySelected, onSortSelected) }
        when {
            state.isLoading -> item { SkeletonList(count = 5, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            state.error != null -> item { EmptyState(EmptyIcons.Cloud, stringResource(R.string.apps_error), stringResource(R.string.apps_error_desc), Modifier.padding(20.dp), tone = EmptyTone.ERROR) }
            state.totalCount == 0 -> item { EmptyState(T14Paths.Apps, stringResource(R.string.apps_empty), stringResource(R.string.apps_empty_desc), Modifier.padding(20.dp)) }
            else -> {
                if (state.featured.isNotEmpty() && filter.isBrowsing) {
                    item { SectionTitle(stringResource(R.string.apps_featured), top = 8.dp) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 14.dp),
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
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                        )
                    }
                }
                item {
                    SectionTitle(
                        stringResource(R.string.apps_waiting),
                        trailing = stringResource(R.string.apps_count, state.apps.size),
                        top = 24.dp,
                    )
                }
                if (state.apps.isEmpty()) {
                    item { EmptyState(T14Paths.Search, stringResource(R.string.apps_no_results), stringResource(R.string.apps_no_results_desc), Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) }
                } else {
                    items(state.apps, key = { it.id }) { app ->
                        AppRow(
                            app = app,
                            onOpenApp = onOpenApp,
                            modifier = Modifier.animateItem().padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One chip row as on the canvas: "Hammasi", the sort chips (Yangi, Kam testerli, Reyting), then
 * the categories. "Hammasi" clears the category; a sort chip and a category can be on together.
 */
@Composable
private fun FilterChips(
    categories: List<String>,
    filter: CatalogFilter,
    onCategorySelected: (String?) -> Unit,
    onSortSelected: (CatalogSort) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            T14Chip(
                stringResource(R.string.apps_category_all),
                selected = filter.category == null && filter.sort == CatalogSort.NEWEST && filter.query.isBlank(),
                onClick = { onCategorySelected(null); onSortSelected(CatalogSort.NEWEST) },
            )
        }
        items(CatalogSort.entries) { sort ->
            T14Chip(
                stringResource(sort.label()),
                selected = sort == filter.sort && sort != CatalogSort.NEWEST,
                onClick = { onSortSelected(if (sort == filter.sort) CatalogSort.NEWEST else sort) },
            )
        }
        items(categories) { category ->
            T14Chip(category, selected = category == filter.category, onClick = { onCategorySelected(if (category == filter.category) null else category) })
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null, top: androidx.compose.ui.unit.Dp = 20.dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = top, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = T14Theme.colors.textSecondary)
        }
    }
}

/** "Topdagilar" hero card: 276dp, 135° category gradient, two light circles, glass icon box. */
@Composable
private fun FeaturedCard(app: CatalogApp, onOpenApp: (String) -> Unit) {
    val style = Categories.style(app.category, app.id)
    val shape = RoundedCornerShape(22.dp)
    val c = T14Theme.colors
    Box(
        modifier = Modifier
            .width(276.dp)
            .shadow(14.dp, shape, ambientColor = style.color.copy(alpha = 0.35f), spotColor = style.color.copy(alpha = 0.35f))
            .clip(shape)
            .background(featuredBrush(style.color))
            .clickable { onOpenApp(app.id) },
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 34.dp, y = (-34).dp).size(130.dp).background(Color.White.copy(alpha = 0.14f), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = 50.dp).size(100.dp).background(Color.White.copy(alpha = 0.08f), CircleShape))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { T14Icon(style.path, size = 28.dp, tint = Color.White) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(app.name, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app.category, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodySmall)
                }
                if (app.isPro) ProBadge(onHero = true)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(if (c.dark) Color(0xFF151D2E) else Color.White)
                        .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Coin14Icon(20.dp)
                    Text(
                        stringResource(R.string.reward_per_day_long, app.rewardPerDay.value.toInt()),
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                    )
                }
                Text(
                    stringResource(R.string.testers_progress_long, app.testersJoined, app.testersNeeded),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** List card: 54dp category tile, name + PRO tag, category, coins per day, progress to 12 testers. */
@Composable
private fun AppRow(app: CatalogApp, onOpenApp: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = T14Theme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier.fillMaxWidth()
            .shadow(if (c.dark) 0.dp else 6.dp, shape, ambientColor = Color(0x0D0F172A), spotColor = Color(0x140F172A))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (c.dark) Modifier.border(1.dp, c.cardBorder, shape) else Modifier)
            .clickable { onOpenApp(app.id) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppTile(app.id, app.name, size = 54.dp, category = app.category)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            app.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (app.isPro) ProBadge(onHero = false)
                    }
                    Text(app.category, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Coin14Icon(16.dp)
                    Text(
                        stringResource(R.string.reward_per_day_short, app.rewardPerDay.value.toInt()),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.coinText,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedProgress(app.testersProgress, modifier = Modifier.weight(1f))
                Text(
                    if (app.testersJoined >= app.testersNeeded) stringResource(R.string.testers_count, app.testersJoined)
                    else stringResource(R.string.testers_progress_short, app.testersJoined, app.testersNeeded),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = T14Theme.colors.textSecondary)
    }
}

private fun CatalogSort.label(): Int = when (this) {
    CatalogSort.NEWEST -> R.string.apps_sort_newest
    CatalogSort.RATING -> R.string.apps_sort_rating
    CatalogSort.FEW_TESTERS -> R.string.apps_sort_few_testers
}
