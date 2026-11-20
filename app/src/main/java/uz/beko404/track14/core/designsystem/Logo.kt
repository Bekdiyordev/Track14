package uz.beko404.track14.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * The Track14 mark from the canvas: blue rounded square, a ring of 14 short segments that get brighter
 * clockwise, and "14" in the middle. Drawn in a 120-unit box like the SVG. [pulse] runs the splash glow.
 */
@Composable
fun Track14Logo(size: Dp, modifier: Modifier = Modifier, cornerUnits: Float = 30f, pulse: Boolean = false) {
    val phase = if (pulse) {
        val t = rememberInfiniteTransition(label = "logo")
        val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart), label = "phase")
        p
    } else {
        -1f
    }
    val textSize = with(LocalDensity.current) { (size * (30f / 120f)).toSp() }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val k = this.size.minDimension / 120f
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), Offset.Zero, Offset(this.size.width, this.size.height)),
                cornerRadius = CornerRadius(cornerUnits * k),
            )
            val r = 38f * k
            val center = Offset(60f * k, 60f * k)
            repeat(14) { i ->
                val base = 0.22f + 0.06f * i
                // Splash: each segment brightens in turn (0.3 → 1 → 0.3), 0.12 s apart, as in the CSS keyframes.
                val alpha = if (phase < 0f) base else {
                    val local = ((phase - i * 0.05f) % 1f + 1f) % 1f
                    0.3f + 0.7f * sin(local * PI.toFloat())
                }
                drawArc(
                    color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
                    startAngle = -80.24f + i * (360f / 14f),
                    sweepAngle = 6.2f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = 9f * k, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            "14", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold,
            fontSize = textSize, letterSpacing = (-0.04f * textSize.value).sp, textAlign = TextAlign.Center,
        )
    }
}

/** "Track" in ink and "14" in blue, Manrope 800. */
@Composable
fun Track14Wordmark(fontSizeSp: Int, modifier: Modifier = Modifier) {
    val ink = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val blue = if (T14Theme.colors.dark) Color(0xFF60A5FA) else Track14Colors.Blue600
    Text(
        androidx.compose.ui.text.buildAnnotatedString {
            append("Track")
            pushStyle(androidx.compose.ui.text.SpanStyle(color = blue))
            append("14")
            pop()
        },
        modifier = modifier, color = ink, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold,
        fontSize = fontSizeSp.sp, letterSpacing = (-fontSizeSp * 0.034f).sp,
    )
}

