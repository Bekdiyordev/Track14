package uz.beko404.track14.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import uz.beko404.track14.domain.model.DayStatus

/** The 14-day card: green = passed, red = missed, grey = ahead; today has an outline. */
@Composable
fun DayDots(days: List<DayStatus>, today: Int?, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEachIndexed { index, status ->
            val color = when (status) {
                DayStatus.PASSED -> Track14Colors.Success
                DayStatus.MISSED -> Track14Colors.Danger
                DayStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
            }
            val isToday = today == index + 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .then(
                        if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)) else Modifier,
                    ),
            )
        }
    }
}
