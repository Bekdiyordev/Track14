package uz.beko404.track14.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
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

/** Lighten [color] towards white by [f], like the canvas lit() helper. */
fun lit(color: Color, f: Float): Color = lerp(color, Color.White, f)

/** 145° tile gradient from the canvas: lit(colour, 0.22) to the colour. */
fun tileBrush(color: Color): Brush = Brush.linearGradient(listOf(lit(color, 0.22f), color))

/** 135° hero gradient for "Topdagilar" cards: light, base at 70%, deep end. */
fun featuredBrush(color: Color): Brush = Brush.linearGradient(
    0f to lit(color, 0.3f),
    0.7f to color,
    1f to lerp(color, Color.Black, 0.25f),
)

/**
 * App icon tile: category colour gradient, a faint top highlight, coloured soft shadow and the
 * category glyph in white (canvas: 54dp tile, 16dp corners, 28dp glyph).
 */
@Composable
fun AppTile(id: String, name: String, size: Dp, modifier: Modifier = Modifier, category: String? = null) {
    val style = Categories.style(category, id)
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier = modifier
            .size(size)
            .shadow(size * 0.12f, shape, ambientColor = style.color.copy(alpha = 0.33f), spotColor = style.color.copy(alpha = 0.33f))
            .clip(shape)
            .background(tileBrush(style.color))
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.18f), 0.08f to Color.Transparent)),
        contentAlignment = Alignment.Center,
    ) {
        if (category != null) {
            T14Icon(style.path, size = size * 0.52f, tint = Color.White)
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}

/** Gold Coin14 coin exactly as the canvas draws it: gradient disc, inner ring, light arc. */
@Composable
fun Coin14Icon(size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val u = this.size.minDimension / 24f
            drawCircle(
                Brush.linearGradient(
                    0f to Color(0xFFFDE68A), 0.55f to Color(0xFFFBBF24), 1f to Color(0xFFD97706),
                    start = androidx.compose.ui.geometry.Offset(2.4f * u, 0f),
                    end = androidx.compose.ui.geometry.Offset(21.6f * u, 24f * u),
                ),
                radius = 11f * u,
            )
            if (size >= 18.dp) {
                drawCircle(Color(0xFFB45309).copy(alpha = 0.4f), radius = 8.2f * u, style = Stroke(width = 1.2f * u))
            }
            drawArc(
                Color.White.copy(alpha = 0.55f),
                startAngle = 205f, sweepAngle = 65f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(3.8f * u, 3.8f * u),
                size = androidx.compose.ui.geometry.Size(16.4f * u, 16.4f * u),
                style = Stroke(width = 1.4f * u, cap = StrokeCap.Round),
            )
        }
        if (size >= 28.dp) {
            Text("14", color = Color(0xFF92400E), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.36f).sp)
        }
    }
}

/** Coin + amount in the canvas amber (light #92400E, dark #FCD34D). */
@Composable
fun CoinAmount(text: String, modifier: Modifier = Modifier, color: Color = T14Theme.colors.coinText, iconSize: Dp = 16.dp) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Coin14Icon(iconSize)
        Text(text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

/** PRO label: gold pill on hero cards, dark square tag in lists. */
@Composable
fun ProBadge(modifier: Modifier = Modifier, onHero: Boolean = true) {
    val c = T14Theme.colors
    Text(
        text = "PRO",
        modifier = modifier
            .clip(RoundedCornerShape(if (onHero) 999.dp else 5.dp))
            .background(if (onHero) c.featuredProBg else c.listProBg)
            .padding(horizontal = if (onHero) 8.dp else 6.dp, vertical = if (onHero) 3.dp else 2.dp),
        color = if (onHero) c.featuredProText else Color.White,
        fontFamily = Inter,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
    )
}

/** 6dp progress bar, blue gradient fill (#60A5FA → #2563EB), grows from zero when it appears. */
@Composable
fun AnimatedProgress(fraction: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) { progress.animateTo(fraction.coerceIn(0f, 1f), tween(durationMillis = 800)) }
    Box(modifier.height(height).clip(RoundedCornerShape(999.dp)).background(T14Theme.colors.progressTrack)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(progress.value).clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB)))),
        )
    }
}
