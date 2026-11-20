package uz.beko404.track14.core.designsystem

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Track14Colors.Blue600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Track14Colors.Blue700,
    background = Track14Colors.LightBackground,
    onBackground = Track14Colors.Navy,
    surface = Track14Colors.LightSurface,
    onSurface = Track14Colors.Navy,
    surfaceVariant = Track14Colors.LightSurfaceVariant,
    onSurfaceVariant = Track14Colors.LightOnSurfaceVariant,
    outline = Track14Colors.LightOutline,
    error = Track14Colors.Danger,
)

private val DarkColors = darkColorScheme(
    primary = Track14Colors.Blue400,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF14264D),
    onPrimaryContainer = Track14Colors.Blue400,
    background = Track14Colors.DarkBackground,
    onBackground = Track14Colors.DarkOnSurface,
    surface = Track14Colors.DarkSurface,
    onSurface = Track14Colors.DarkOnSurface,
    surfaceVariant = Track14Colors.DarkSurfaceVariant,
    onSurfaceVariant = Track14Colors.DarkOnSurfaceVariant,
    outline = Track14Colors.DarkOutline,
    error = Color(0xFFF87171),
)

/** Corner radii from the canvas: chips 12, fields 14, buttons 16, cards 18–22, sheets 28. */
private val Track14Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun Track14Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val activity = LocalActivity.current as? ComponentActivity
    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.argb(0xe6, 0xff, 0xff, 0xff),
                android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b),
            ) { darkTheme },
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalT14Colors provides if (darkTheme) DarkExtraColors else LightExtraColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Track14Typography,
            shapes = Track14Shapes,
            content = content,
        )
    }
}
