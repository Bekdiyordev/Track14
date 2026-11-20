package uz.beko404.track14.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The canvas draws every icon as a 24×24 stroked SVG path (stroke 2, round caps and joins).
 * These are those exact paths; circles and rects are written as arcs and lines.
 */
object T14Paths {
    // bottom bar
    const val Apps = "M5 3h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M16 3h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M5 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z M16 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z"
    const val Tests = "M6 4h12a3 3 0 0 1 3 3v11a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z M8 2v4M16 2v4M3 10h18M9 15l2 2 4-4"
    const val MyApps = "M12 3v12M7 8l5-5 5 5 M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4"
    const val Profile = "M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0 M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6"

    // general
    const val Search = "M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0 M20 20l-3.5-3.5"
    const val Back = "M15 5l-7 7 7 7"
    const val Chevron = "M9 5l7 7-7 7"
    const val Close = "M6 6l12 12M18 6L6 18"
    const val Star = "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"
    const val Bell = "M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4"
    const val Settings = "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6 M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"
    const val Help = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.7.3-1 .9-1 1.7v.5 M12 17h.01"
    const val Wallet = "M3 7h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M3 7l12-3v3"
    const val Plus = "M12 5v14M5 12h14"
    const val Check = "M5 12l5 5L20 7"
    const val Warning = "M12 3l9.5 17h-19z M12 10v4M12 17v.5"
    const val Flag = "M5 21V4M5 4h11l-2 4 2 4H5"
    const val Lock = "M5 11h14v10H5z M8 11V7a4 4 0 0 1 8 0v4"
    const val Info = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 11v5M12 8h.01"
    const val Upload = MyApps
    const val Play = "M8 5v14l11-7z"
    const val Users = "M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0 M2.5 20c.8-3.5 3.3-5 6.5-5s5.7 1.5 6.5 5 M14.5 9a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0 M17 14.5c2.5.2 4 1.7 4.5 5"
    const val Dots = "M4 12h.01M12 12h.01M20 12h.01"
    const val Logout = "M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3 M10 17l-5-5 5-5 M5 12h11"
    const val Trophy = "M8 4h8v5a4 4 0 0 1-8 0z M8 6H5a3 3 0 0 0 3 4 M16 6h3a3 3 0 0 1-3 4 M12 13v4 M8 20h8"

    // category glyphs (canvas ICON map)
    const val Book = "M12 6.5C10.5 5 8 4.5 4 4.5v13c4 0 6.5.5 8 2 1.5-1.5 4-2 8-2v-13c-4 0-6.5.5-8 2z M12 6.5v13"
    const val Steps = "M8 4c2 0 3 2 3 4.5S10 12 8 12 5 10.5 5 8.5 6 4 8 4z M16 12c2 0 3 2 3 4.5S18 20 16 20s-3-1-3-3.5 1-4.5 3-4.5z"
    const val Bowl = "M4 12h16a8 8 0 0 1-16 0z M9 8c0-1.5 1-1.5 1-3M14 8c0-1.5 1-1.5 1-3"
    const val Dice = "M8 4h8a4 4 0 0 1 4 4v8a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V8a4 4 0 0 1 4-4z M9 9h.01 M15 9h.01 M9 15h.01 M15 15h.01 M12 12h.01"
    const val Moon = "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z M17 4v3M15.5 5.5h3"
    const val Briefcase = "M4 8h16v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z M9 8V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v3 M4 13h16"
    const val Sparkle = "M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5L18 18M18 6l-2.5 2.5M8.5 15.5L6 18"
}

/** Category look from the canvas: tile colour and glyph. */
data class CategoryStyle(val color: Color, val path: String)

object Categories {
    private val styles = mapOf(
        "Ta'lim" to CategoryStyle(Color(0xFF0EA5E9), T14Paths.Book),
        "Moliya" to CategoryStyle(Color(0xFF16A34A), T14Paths.Wallet),
        "O'yinlar" to CategoryStyle(Color(0xFF7C3AED), T14Paths.Dice),
        "Sog'liq" to CategoryStyle(Color(0xFFEA580C), T14Paths.Steps),
        "Taom" to CategoryStyle(Color(0xFFDB2777), T14Paths.Bowl),
        "Turmush tarzi" to CategoryStyle(Color(0xFF0F766E), T14Paths.Moon),
        "Ish va unumdorlik" to CategoryStyle(Color(0xFF2563EB), T14Paths.Briefcase),
    )

    fun style(category: String?, fallbackId: String): CategoryStyle =
        styles[category] ?: CategoryStyle(accentFor(fallbackId), T14Paths.Sparkle)
}

/** Draws one canvas path, scaled from the 24×24 viewBox, in the current content colour. */
@Composable
fun T14Icon(
    path: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    strokeWidth: Float = 2f,
) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(parsed, tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** The same path as an ImageVector, for Material slots that take one (bottom bar, icon buttons). */
fun t14Vector(name: String, path: String, strokeWidth: Float = 2f): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
