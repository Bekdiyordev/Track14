package uz.beko404.track14.feature.wallet

import uz.beko404.track14.core.designsystem.EmptyTone
import uz.beko404.track14.core.designsystem.EmptyIcons
import uz.beko404.track14.core.designsystem.EmptyState
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.billing.PackOffer
import uz.beko404.track14.core.billing.PurchaseOutcome
import uz.beko404.track14.core.designsystem.BackButton
import uz.beko404.track14.core.designsystem.Coin14Icon
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.model.TxFilter
import uz.beko404.track14.domain.model.TxType
import uz.beko404.track14.domain.model.WalletTransaction
import uz.beko404.track14.feature.common.formatCoins
import uz.beko404.track14.feature.messageRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BackButton(onBack)
        Text(title, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
    }
}

// ---------------------------------------------------------------- wallet

@Composable
fun WalletRoute(onBack: () -> Unit, onBuy: () -> Unit, viewModel: WalletViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = T14Theme.colors
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val todayLabel = stringResource(R.string.day_today_caps)
    val yesterdayLabel = stringResource(R.string.day_yesterday_caps)
    val month = state.all.filter {
        it.createdAtMillis > 0 && Instant.ofEpochMilli(it.createdAtMillis).atZone(zone).toLocalDate().let { d -> d.year == today.year && d.month == today.month }
    }
    val monthIn = month.filter { it.isIncome }.sumOf { it.amount.value }
    val monthOut = -month.filter { !it.isIncome }.sumOf { it.amount.value }

    Column(Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.wallet_title), onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
            item { BalanceCard(state.balance?.value, monthOut, monthIn, state.purchasesEnabled, onBuy) }
            item {
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxFilter.entries.forEach { f ->
                        val on = state.filter == f
                        Box(
                            Modifier.height(36.dp).clip(RoundedCornerShape(999.dp)).background(if (on) c.chipOn else c.chipOff)
                                .border(1.dp, if (on) c.chipOn else c.chipOffBorder, RoundedCornerShape(999.dp))
                                .clickable { viewModel.setFilter(f) }.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(stringResource(f.label()), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.chipOnText else c.chipOffText) }
                    }
                }
            }
            item {
                T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), spacing = 0.dp) {
                    if (state.shown.isEmpty()) {
                        EmptyState(T14Paths.Wallet, stringResource(R.string.wallet_empty), stringResource(R.string.wallet_empty_desc), Modifier.padding(vertical = 8.dp), card = false)
                    }
                    var last = ""
                    state.shown.forEach { tx ->
                        val day = dayLabel(tx.createdAtMillis, zone, today, todayLabel, yesterdayLabel)
                        if (day != last) {
                            last = day
                            if (day.isNotEmpty()) {
                                Text(day, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textSecondary, letterSpacing = 0.4.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                            }
                        }
                        TransactionRow(tx)
                    }
                }
            }
        }
    }
}

private fun dayLabel(millis: Long, zone: ZoneId, today: LocalDate, todayLabel: String, yesterdayLabel: String): String {
    if (millis <= 0) return ""
    val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return when (d) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> d.format(DateTimeFormatter.ofPattern("d-MMMM")).uppercase()
    }
}

/** Blue gradient card: balance, this month's spend and income, white "buy" button. */
@Composable
private fun BalanceCard(balance: Long?, monthOut: Long, monthIn: Long, purchasesEnabled: Boolean, onBuy: () -> Unit) {
    val soft = Color(0xFFEFF6FF)
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = Color(0x522563EB), spotColor = Color(0x522563EB))
            .clip(shape)
            .background(Brush.linearGradient(0f to Color(0xFF3B82F6), 0.6f to Color(0xFF1D4ED8), 1f to Color(0xFF1E3A8A)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.wallet_available), fontSize = 13.sp, color = soft)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Coin14Icon(34.dp)
                Text(balance?.let(::formatCoins) ?: "…", fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, color = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(stringResource(R.string.wallet_month_spent), "−" + formatCoins(monthOut), Modifier.weight(1f))
            MiniStat(stringResource(R.string.wallet_month_earned), "+" + formatCoins(monthIn), Modifier.weight(1f))
        }
        if (purchasesEnabled) {
            Row(
                Modifier.fillMaxWidth().height(52.dp)
                    .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x380F172A), spotColor = Color(0x380F172A))
                    .clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(onClick = onBuy),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                T14Icon(T14Paths.Plus, size = 20.dp, tint = Color(0xFF1D4ED8), strokeWidth = 2.4f)
                Text(stringResource(R.string.wallet_buy), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 12.sp, color = Color(0xFFEFF6FF))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private data class TxLook(val icon: String, val bg: Color, val fg: Color)

private const val ICON_DAY = "M5 12l5 5 9-10"
private const val ICON_BONUS = "M12 3l2.5 5.5 6 .7-4.5 4 1.2 6L12 16.3 6.8 19.2 8 13.2l-4.5-4 6-.7z"
private const val ICON_PENALTY = "M12 7v6M12 17v.5"
private const val ICON_LISTING = "M12 16V4M7 9l5-5 5 5M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4"

private fun TxType.look(): TxLook = when (this) {
    TxType.PENALTY -> TxLook(ICON_PENALTY, Color(0xFFFEE2E2), Color(0xFFB91C1C))
    TxType.LISTING_FEE, TxType.FEATURE -> TxLook(ICON_LISTING, Color(0xFFEFF6FF), Color(0xFF1D4ED8))
    TxType.PURCHASE -> TxLook(T14Paths.Plus, Color(0xFFFEF3C7), Color(0xFFB45309))
    TxType.BONUS, TxType.WELCOME -> TxLook(ICON_BONUS, Color(0xFFFEF3C7), Color(0xFFB45309))
    TxType.ADJUSTMENT -> TxLook(T14Paths.Settings, Color(0xFFF1F5F9), Color(0xFF475569))
    else -> TxLook(ICON_DAY, Color(0xFFDCFCE7), Color(0xFF15803D))
}

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    val c = T14Theme.colors
    val look = tx.type.look()
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (c.dark) look.fg.copy(alpha = 0.18f) else look.bg),
            contentAlignment = Alignment.Center,
        ) { T14Icon(look.icon, size = 18.dp, tint = if (c.dark) androidx.compose.ui.graphics.lerp(look.fg, Color.White, 0.45f) else look.fg, strokeWidth = 2.2f) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(stringResource(tx.type.label()), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (tx.createdAtMillis > 0) {
                Text(
                    Instant.ofEpochMilli(tx.createdAtMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 12.sp, color = c.textSecondary,
                )
            }
        }
        val bad = tx.type == TxType.PENALTY
        Text(
            (if (tx.isIncome) "+" else "−") + formatCoins(kotlin.math.abs(tx.amount.value)),
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = when {
                tx.isIncome -> c.successText
                bad -> c.dangerText
                else -> MaterialTheme.colorScheme.onSurface
            },
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
    val c = T14Theme.colors
    Column(Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.shop_title), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Row(
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.amberSoft)
                        .border(1.dp, c.amberBorder, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Coin14Icon(28.dp)
                    val ink = if (c.dark) c.amberText else Color(0xFF78350F)
                    val text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.shop_fees, formatCoins(state.economy.fee(Plan.STANDARD).value), formatCoins(state.economy.fee(Plan.PRO).value)))
                        }
                        append(stringResource(R.string.shop_fees_rest))
                    }
                    Text(text, fontSize = 13.sp, lineHeight = 20.sp, color = ink)
                }
            }
            when {
                state.loading -> item { SkeletonList(count = 4, modifier = Modifier.padding(20.dp)) }
                state.offers.isEmpty() -> item {
                    EmptyState(
                        EmptyIcons.Cart, stringResource(R.string.shop_unavailable_title), stringResource(R.string.shop_unavailable),
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp), tone = EmptyTone.ERROR,
                    )
                }
                else -> {
                    item { Spacer(Modifier.height(14.dp)) }
                    items(state.offers, key = { it.pack.productId }) { offer ->
                        Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)) {
                            PackCard(offer, selected = offer.pack.productId == state.selected) { viewModel.select(offer.pack.productId) }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.shop_terms), fontSize = 12.sp, lineHeight = 18.sp, color = c.textSecondary, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp)) }
        }
        Column(
            Modifier.fillMaxWidth().background(c.navBar)
                .drawBehind { drawLine(c.navBorder, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val offer = state.selectedOffer
            state.outcome?.let { OutcomeText(it) }
            val after = state.balance?.let { b -> b.value + (offer?.pack?.coins?.value ?: 0L) }
            Row {
                Text(stringResource(R.string.shop_after), fontSize = 13.sp, color = c.textBody, modifier = Modifier.weight(1f))
                Text(after?.let { stringResource(R.string.shop_after_value, formatCoins(it)) } ?: "…", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            PrimaryButton(
                if (offer != null) stringResource(R.string.shop_buy, offer.price) else stringResource(R.string.shop_choose),
                onClick = { context.findActivity()?.let(viewModel::buy) },
                enabled = offer != null && !state.launching,
                height = 52.dp,
            )
        }
    }
}

private fun packNote(coins: Long): Int = when {
    coins <= 500 -> R.string.pack_note_500
    coins <= 1_000 -> R.string.pack_note_1000
    coins <= 2_000 -> R.string.pack_note_2000
    coins <= 5_000 -> R.string.pack_note_5000
    else -> R.string.pack_note_10000
}

@Composable
private fun PackCard(offer: PackOffer, selected: Boolean, onClick: () -> Unit) {
    val c = T14Theme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.fillMaxWidth()
            .then(
                if (c.dark) Modifier
                else Modifier.shadow(if (selected) 12.dp else 2.dp, shape, ambientColor = if (selected) Color(0x292563EB) else Color(0x0A0F172A), spotColor = if (selected) Color(0x292563EB) else Color(0x0A0F172A)),
            )
            .clip(shape)
            .background(if (selected) c.blueSoft else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) Track14Colors.Blue600 else c.fieldBorder, shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c.amberSoft).border(1.dp, c.amberBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Coin14Icon(34.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(formatCoins(offer.pack.coins.value), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                    Text("Coin14", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), modifier = Modifier.padding(bottom = 3.dp))
                }
                Text(stringResource(packNote(offer.pack.coins.value)), fontSize = 12.sp, color = c.textSecondary, maxLines = 1)
            }
            Column(Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(offer.price, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Box(
                    Modifier.size(20.dp).clip(CircleShape).background(if (selected) Track14Colors.Blue600 else MaterialTheme.colorScheme.surface)
                        .border(2.dp, if (selected) Track14Colors.Blue600 else Color(0xFFCBD5E1), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (selected) T14Icon(T14Paths.Check, size = 11.dp, tint = Color.White, strokeWidth = 4f) }
            }
        }
        if (offer.pack.popular) {
            Text(
                stringResource(R.string.shop_popular),
                modifier = Modifier.align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp,
            )
        }
    }
}

@Composable
private fun OutcomeText(outcome: PurchaseOutcome) {
    val c = T14Theme.colors
    val (text, color) = when (outcome) {
        is PurchaseOutcome.Credited -> stringResource(R.string.shop_credited, formatCoins(outcome.coins.value)) to c.successText
        PurchaseOutcome.Pending -> stringResource(R.string.shop_pending) to c.textSecondary
        PurchaseOutcome.Cancelled -> stringResource(R.string.shop_cancelled) to c.textSecondary
        is PurchaseOutcome.Failed -> stringResource(outcome.error.messageRes()) to c.dangerText
        is PurchaseOutcome.BillingError -> stringResource(R.string.shop_error) to c.dangerText
    }
    Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}
