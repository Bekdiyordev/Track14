package uz.beko404.track14.feature.wallet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.R
import uz.beko404.track14.core.billing.PackOffer
import uz.beko404.track14.core.billing.PurchaseOutcome
import uz.beko404.track14.core.designsystem.Coin14Icon
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.domain.model.TxFilter
import uz.beko404.track14.domain.model.TxType
import uz.beko404.track14.domain.model.WalletTransaction
import uz.beko404.track14.feature.messageRes

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

private fun formatCoins(v: Long): String = "%,d".format(v).replace(',', ' ')

// ---------------------------------------------------------------- wallet

@Composable
fun WalletRoute(onBack: () -> Unit, onBuy: () -> Unit, viewModel: WalletViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.wallet_title), onBack)
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { BalanceCard(state, onBuy) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TxFilter.entries) { f ->
                        FilterChip(selected = state.filter == f, onClick = { viewModel.setFilter(f) }, label = { Text(stringResource(f.label())) })
                    }
                }
            }
            if (state.shown.isEmpty()) {
                item {
                    Text(stringResource(R.string.wallet_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.shown, key = { it.id }) { tx -> TransactionRow(tx, Modifier.animateItem()) }
        }
    }
}

@Composable
private fun BalanceCard(state: WalletUiState, onBuy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Track14Colors.Blue500, Track14Colors.Blue700, Color(0xFF1E3A8A))))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.wallet_available), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Coin14Icon(34.dp)
            Text(
                state.balance?.let { formatCoins(it.value) } ?: "…",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(stringResource(R.string.wallet_earned), "+" + formatCoins(state.earned.value), Modifier.weight(1f))
            MiniStat(stringResource(R.string.wallet_spent), "−" + formatCoins(state.spent.value), Modifier.weight(1f))
        }
        if (state.purchasesEnabled) Button(
            onClick = onBuy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Track14Colors.Blue700),
        ) { Text(stringResource(R.string.wallet_buy), fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f)).padding(12.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (tx.isIncome) Track14Colors.Success.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Coin14Icon(18.dp) }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(tx.type.label()), fontWeight = FontWeight.SemiBold)
            if (tx.createdAtMillis > 0) {
                Text(
                    DateUtils.getRelativeTimeSpanString(tx.createdAtMillis).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            (if (tx.isIncome) "+" else "−") + formatCoins(kotlin.math.abs(tx.amount.value)),
            fontWeight = FontWeight.Bold,
            color = if (tx.isIncome) Track14Colors.Success else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun TxFilter.label(): Int = when (this) {
    TxFilter.ALL -> R.string.wallet_filter_all
    TxFilter.INCOME -> R.string.wallet_filter_income
    TxFilter.SPENT -> R.string.wallet_filter_spent
}

private fun TxType.label(): Int = when (this) {
    TxType.WELCOME -> R.string.tx_welcome
    TxType.PURCHASE -> R.string.tx_purchase
    TxType.EARN_DAY -> R.string.tx_earn_day
    TxType.BONUS -> R.string.tx_bonus
    TxType.FEEDBACK -> R.string.tx_feedback
    TxType.LISTING_FEE -> R.string.tx_listing_fee
    TxType.PENALTY -> R.string.tx_penalty
    TxType.FEATURE -> R.string.tx_feature
    TxType.REFUND -> R.string.tx_refund
    TxType.ADJUSTMENT -> R.string.tx_adjustment
}

// ---------------------------------------------------------------- shop

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ShopRoute(onBack: () -> Unit, viewModel: ShopViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.shop_title), onBack)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.shop_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                state.loading -> SkeletonList(count = 4, modifier = Modifier.padding(vertical = 12.dp))
                state.offers.isEmpty() -> Text(stringResource(R.string.shop_unavailable), style = MaterialTheme.typography.bodyMedium)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.offers, key = { it.pack.productId }) { offer ->
                        PackCard(offer, selected = offer.pack.productId == state.selected) { viewModel.select(offer.pack.productId) }
                    }
                    item {
                        Text(stringResource(R.string.shop_terms), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.outcome?.let { OutcomeText(it) }
            val offer = state.selectedOffer
            Button(
                onClick = { context.findActivity()?.let(viewModel::buy) },
                enabled = offer != null && !state.launching,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (offer != null) stringResource(R.string.shop_buy, offer.price) else stringResource(R.string.shop_choose))
            }
        }
    }
}

@Composable
private fun PackCard(offer: PackOffer, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        label = "pack",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFFBEB)),
                contentAlignment = Alignment.Center,
            ) { Coin14Icon(32.dp) }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(formatCoins(offer.pack.coins.value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Coin14", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                }
            }
            Text(offer.price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
        if (offer.pack.popular) {
            Text(
                stringResource(R.string.shop_popular),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun OutcomeText(outcome: PurchaseOutcome) {
    val (text, color) = when (outcome) {
        is PurchaseOutcome.Credited -> stringResource(R.string.shop_credited, formatCoins(outcome.coins.value)) to Track14Colors.Success
        PurchaseOutcome.Pending -> stringResource(R.string.shop_pending) to MaterialTheme.colorScheme.onSurfaceVariant
        PurchaseOutcome.Cancelled -> stringResource(R.string.shop_cancelled) to MaterialTheme.colorScheme.onSurfaceVariant
        is PurchaseOutcome.Failed -> stringResource(outcome.error.messageRes()) to MaterialTheme.colorScheme.error
        is PurchaseOutcome.BillingError -> stringResource(R.string.shop_error) to MaterialTheme.colorScheme.error
    }
    Text(text, color = color, style = MaterialTheme.typography.bodySmall)
}
