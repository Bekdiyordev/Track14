package uz.beko404.track14.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Accent colours for app tiles; the id decides the colour so it never changes between screens. */
private val AppAccents = listOf(
    Color(0xFF0EA5E9), Color(0xFFEA580C), Color(0xFF16A34A), Color(0xFFDB2777),
    Color(0xFF7C3AED), Color(0xFF0F766E), Color(0xFF2563EB),
)

fun accentFor(id: String): Color = AppAccents[Math.floorMod(id.hashCode(), AppAccents.size)]

fun accentBrush(id: String): Brush {
    val accent = accentFor(id)
    return Brush.linearGradient(listOf(lerp(accent, Color.White, 0.22f), accent))
}

/** Gradient tile with the app's first letter. Real icons (iconUrl) come with image loading. */
@Composable
fun AppTile(id: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(accentBrush(id)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Gold Coin14 coin; shows "14" when it is big enough to read. */
@Composable
fun Coin14Icon(size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(
                Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFFBBF24), Color(0xFFD97706))),
            )
        }
        if (size >= 22.dp) {
            Text(
                "14",
                color = Color(0xFF92400E),
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.4f).sp,
            )
        }
    }
}

@Composable
fun CoinAmount(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Coin14Icon(16.dp)
        Text(text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Text(
        text = "PRO",
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFFDE68A))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        color = Track14Colors.Navy,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp,
    )
}

/** Progress bar that grows from zero when it first appears. */
@Composable
fun AnimatedProgress(fraction: Float, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) { progress.animateTo(fraction.coerceIn(0f, 1f), tween(durationMillis = 700)) }
    LinearProgressIndicator(
        progress = { progress.value },
        modifier = modifier.height(6.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer,
        strokeCap = StrokeCap.Round,
    )
}
