package uz.beko404.track14.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.beko404.track14.R
import uz.beko404.track14.domain.model.AppStatus

/** Neutral pill with a coloured dot (design decision: no green fills on owner cards). */
@Composable
fun StatusPill(status: AppStatus, modifier: Modifier = Modifier) {
    val (label, dot) = when (status) {
        AppStatus.DRAFT -> R.string.status_draft to Color(0xFF94A3B8)
        AppStatus.ACTIVE -> R.string.status_active to Track14Colors.Success
        AppStatus.PAUSED -> R.string.status_paused to Track14Colors.CoinGold
        AppStatus.PUBLISHED -> R.string.status_published to Track14Colors.Blue600
        AppStatus.SUSPENDED -> R.string.status_suspended to Track14Colors.Danger
        AppStatus.REMOVED -> R.string.status_removed to Color(0xFF94A3B8)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, T14Theme.colors.fieldBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dot))
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)), fontWeight = FontWeight.Bold)
    }
}
