package uz.beko404.track14.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.CoinPill
import uz.beko404.track14.core.designsystem.T14Theme

/** Header right side on tab screens: coin balance (opens the wallet) or the "Kirish" pill. */
@Composable
fun HeaderBalancePill(onWallet: () -> Unit, onSignIn: () -> Unit, viewModel: BalanceViewModel = hiltViewModel()) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    when (val b = balance) {
        HeaderBalance.Unknown -> Unit
        HeaderBalance.SignedOut -> {
            val c = T14Theme.colors
            Box(
                Modifier.height(40.dp).clip(RoundedCornerShape(999.dp)).background(c.blueSoft).clickable(onClick = onSignIn).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.header_sign_in), color = c.blueSoftText, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
            }
        }
        is HeaderBalance.Balance -> CoinPill(formatCoins(b.coins.value), onClick = onWallet)
    }
}
