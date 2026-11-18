package uz.beko404.track14.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    primary = Track14Colors.Blue500,
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

@Composable
fun Track14Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
