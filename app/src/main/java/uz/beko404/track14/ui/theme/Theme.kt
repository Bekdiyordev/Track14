package uz.beko404.track14.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class Track14ThemeMode(val label: String) {
    System("Tizim"),
    Light("Yorug'"),
    Dark("Qorong'i"),
}

private val LightColorScheme = lightColorScheme(
    primary = TrackGreen,
    onPrimary = Color.White,
    primaryContainer = TrackGreenLight,
    onPrimaryContainer = Color(0xFF062D24),
    secondary = TrackBlue,
    onSecondary = Color.White,
    secondaryContainer = TrackBlueLight,
    onSecondaryContainer = Color(0xFF071F55),
    tertiary = TrackAmber,
    onTertiary = Color(0xFF2E2100),
    error = TrackRed,
    background = TrackBackgroundLight,
    onBackground = TrackTextLight,
    surface = TrackSurfaceLight,
    onSurface = TrackTextLight,
    surfaceVariant = TrackSurfaceVariantLight,
    onSurfaceVariant = TrackTextMutedLight,
    outline = Color(0xFF75847D),
)

private val DarkColorScheme = darkColorScheme(
    primary = TrackGreenLight,
    onPrimary = Color(0xFF00382C),
    primaryContainer = TrackGreenDark,
    onPrimaryContainer = Color(0xFFC1F2E3),
    secondary = TrackBlueLight,
    onSecondary = Color(0xFF0A2F74),
    secondaryContainer = Color(0xFF1647AA),
    onSecondaryContainer = Color(0xFFDDE6FF),
    tertiary = TrackAmber,
    onTertiary = Color(0xFF2E2100),
    error = Color(0xFFFFB4AB),
    background = TrackBackgroundDark,
    onBackground = TrackTextDark,
    surface = TrackSurfaceDark,
    onSurface = TrackTextDark,
    surfaceVariant = TrackSurfaceVariantDark,
    onSurfaceVariant = TrackTextMutedDark,
    outline = Color(0xFF899891),
)

@Composable
fun Track14Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
