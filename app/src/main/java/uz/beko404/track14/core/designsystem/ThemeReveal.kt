package uz.beko404.track14.core.designsystem

import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import kotlin.math.hypot
import kotlin.math.max

/**
 * Animates a theme switch as a circle anchored at the camera cutout (top centre when there is none):
 * the dark theme grows out of it, and the light theme folds the dark one back into it.
 *
 * The screen in the old theme is captured just before the switch and drawn over the new one,
 * clipped by the circle, so nothing is composed twice.
 */
@Composable
internal fun ThemeReveal(darkTheme: Boolean, content: @Composable (shownDark: Boolean) -> Unit) {
    val view = LocalView.current
    val layer = rememberGraphicsLayer()
    val progress = remember { Animatable(1f) }
    var shownDark by remember { mutableStateOf(darkTheme) }
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var expand by remember { mutableStateOf(true) }

    LaunchedEffect(darkTheme) {
        if (darkTheme == shownDark) return@LaunchedEffect
        val shot = if (animationsEnabled(view)) runCatching { layer.toImageBitmap() }.getOrNull() else null
        if (shot == null) {
            shownDark = darkTheme
            return@LaunchedEffect
        }
        try {
            expand = darkTheme
            snapshot = shot
            progress.snapTo(0f)
            shownDark = darkTheme
            progress.animateTo(1f, tween(durationMillis = 700, easing = RevealEasing))
        } finally {
            snapshot = null
        }
    }

    Box(
        Modifier.fillMaxSize().drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
            val shot = snapshot ?: return@drawWithContent
            val origin = revealOrigin(view, size)
            val maxRadius = farthestCorner(origin, size)
            val p = progress.value
            if (expand) {
                // New (dark) theme grows from the cutout: keep the old screen outside the circle.
                val circle = Path().apply { addOval(Rect(origin, maxRadius * p)) }
                clipPath(circle, ClipOp.Difference) { drawImage(shot) }
            } else {
                // Old (dark) theme shrinks back into the cutout: keep it inside the circle.
                val circle = Path().apply { addOval(Rect(origin, maxRadius * (1f - p))) }
                clipPath(circle) { drawImage(shot) }
            }
        },
    ) {
        content(shownDark)
    }
}

private val RevealEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/** Centre of the top display cutout (the camera hole), or the top centre of the screen. */
private fun revealOrigin(view: View, size: Size): Offset {
    val insets = view.rootWindowInsets ?: return Offset(size.width / 2f, 0f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val cutout = insets.displayCutout?.boundingRectTop
        if (cutout != null && !cutout.isEmpty) {
            val location = IntArray(2).also(view::getLocationInWindow)
            return Offset(cutout.exactCenterX() - location[0], cutout.exactCenterY() - location[1])
        }
    }
    val statusBar = WindowInsetsCompat.toWindowInsetsCompat(insets, view)
        .getInsets(WindowInsetsCompat.Type.statusBars()).top
    return Offset(size.width / 2f, statusBar / 2f)
}

private fun farthestCorner(origin: Offset, size: Size): Float = max(
    max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
    max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
)

/** Respects "Remove animations" / animator duration scale 0. */
private fun animationsEnabled(view: View): Boolean =
    Settings.Global.getFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
