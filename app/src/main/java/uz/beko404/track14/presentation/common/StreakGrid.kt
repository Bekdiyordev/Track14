package uz.beko404.track14.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uz.beko404.track14.ui.theme.Track14Theme

enum class StreakDayState {
    Pending,
    Completed,
    Missed,
}

@Composable
fun StreakGrid(
    states: List<StreakDayState>,
    modifier: Modifier = Modifier,
) {
    val days = List(14) { index ->
        states.getOrElse(index) { StreakDayState.Pending }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.chunked(7).forEachIndexed { rowIndex, rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowDays.forEachIndexed { columnIndex, state ->
                    val dayNumber = rowIndex * 7 + columnIndex + 1
                    StreakDayBox(
                        dayNumber = dayNumber,
                        state = state,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakDayBox(
    dayNumber: Int,
    state: StreakDayState,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (state) {
        StreakDayState.Pending -> MaterialTheme.colorScheme.surfaceVariant
        StreakDayState.Completed -> MaterialTheme.colorScheme.primary
        StreakDayState.Missed -> MaterialTheme.colorScheme.error
    }
    val contentColor = when (state) {
        StreakDayState.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
        StreakDayState.Completed -> MaterialTheme.colorScheme.onPrimary
        StreakDayState.Missed -> MaterialTheme.colorScheme.onError
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StreakGridPreview() {
    Track14Theme {
        StreakGrid(
            states = listOf(
                StreakDayState.Completed,
                StreakDayState.Completed,
                StreakDayState.Missed,
                StreakDayState.Completed,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
