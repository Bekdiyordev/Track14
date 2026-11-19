package uz.beko404.track14.core.designsystem

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import uz.beko404.track14.R

/** Moving highlight over placeholder blocks; still when the system turns animations off. */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val context = LocalContext.current
    val animationsOn = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    if (!animationsOn) return@composed background(base)
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(
        Brush.linearGradient(
            colors = listOf(base, highlight.copy(alpha = 0.9f), base),
            start = Offset(x * 600f, 0f),
            end = Offset(x * 600f + 600f, 0f),
        ),
    )
}

@Composable
private fun Bone(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).shimmer())
}

/** Card-shaped placeholder: a tile, two text lines and an optional row of day cells. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier, withDays: Boolean = false) {
    Column(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).shimmer())
            Column(Modifier.padding(start = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Bone(Modifier.fillMaxWidth(0.6f).height(14.dp))
                Bone(Modifier.fillMaxWidth(0.35f).height(10.dp))
            }
        }
        if (withDays) Bone(Modifier.fillMaxWidth().height(12.dp))
    }
}

/** A few skeleton cards while a list loads; announced once as "Loading" for TalkBack. */
@Composable
fun SkeletonList(count: Int = 4, withDays: Boolean = false, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.loading)
    Column(
        modifier.fillMaxWidth().semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) { SkeletonCard(withDays = withDays) }
    }
}
