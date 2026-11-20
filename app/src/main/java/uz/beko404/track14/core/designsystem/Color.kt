package uz.beko404.track14.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Brand tokens from the design canvas (section "Brend"). */
object Track14Colors {
    val Blue600 = Color(0xFF2563EB)
    val Blue700 = Color(0xFF1D4ED8)
    val Blue500 = Color(0xFF3B82F6)
    val Blue400 = Color(0xFF60A5FA)
    val Navy = Color(0xFF0F172A)

    val LightBackground = Color(0xFFF4F7FB)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF1F5F9)
    val LightOutline = Color(0xFFE2E8F0)
    val LightOnSurfaceVariant = Color(0xFF64748B)

    val DarkBackground = Color(0xFF0B1220)
    val DarkSurface = Color(0xFF151D2E)
    val DarkSurfaceVariant = Color(0xFF1E293B)
    val DarkOutline = Color(0xFF263247)
    val DarkOnSurface = Color(0xFFF1F5F9)
    val DarkOnSurfaceVariant = Color(0xFF94A3B8)

    val Success = Color(0xFF22C55E)
    val Danger = Color(0xFFDC2626)
    val CoinGold = Color(0xFFF59E0B)
}

/**
 * Canvas colours that Material's scheme has no slot for, light and dark (the "· Qorong'i" boards).
 * Read them through [T14Theme.colors]; never test isSystemInDarkTheme() in a screen, the user can
 * pick the theme in Settings.
 */
@androidx.compose.runtime.Immutable
data class T14ExtraColors(
    val dark: Boolean,
    val gradientTop: Color,
    val cardBorder: Color,
    val field: Color,
    val fieldBorder: Color,
    val divider: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textBody: Color,
    val chipOn: Color,
    val chipOnText: Color,
    val chipOff: Color,
    val chipOffBorder: Color,
    val chipOffText: Color,
    val coinBg: Color,
    val coinBorder: Color,
    val coinText: Color,
    val progressTrack: Color,
    val featuredProBg: Color,
    val featuredProText: Color,
    val listProBg: Color,
    val blueSoft: Color,
    val blueSoftText: Color,
    val blueBorder: Color,
    val successSoft: Color,
    val successText: Color,
    val dangerSoft: Color,
    val dangerBorder: Color,
    val dangerText: Color,
    val amberSoft: Color,
    val amberBorder: Color,
    val amberText: Color,
    val navBar: Color,
    val navBorder: Color,
    val navSelected: Color,
    val navUnselected: Color,
    val bannerBrush: List<Color>,
)

val LightExtraColors = T14ExtraColors(
    dark = false,
    gradientTop = Color(0xFFEAF1FF),
    cardBorder = Color.Transparent,
    field = Color.White,
    fieldBorder = Color(0xFFE2E8F0),
    divider = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF64748B),
    textTertiary = Color(0xFF94A3B8),
    textBody = Color(0xFF334155),
    chipOn = Color(0xFF0F172A),
    chipOnText = Color.White,
    chipOff = Color.White,
    chipOffBorder = Color(0xFFE2E8F0),
    chipOffText = Color(0xFF334155),
    coinBg = Color(0xFFFFFBEB),
    coinBorder = Color(0xFFFDE68A),
    coinText = Color(0xFF92400E),
    progressTrack = Color(0xFFEFF6FF),
    featuredProBg = Color(0xFFFDE68A),
    featuredProText = Color(0xFF0F172A),
    listProBg = Color(0xFF0F172A),
    blueSoft = Color(0xFFEFF6FF),
    blueSoftText = Color(0xFF1D4ED8),
    blueBorder = Color(0xFFBFDBFE),
    successSoft = Color(0xFFDCFCE7),
    successText = Color(0xFF15803D),
    dangerSoft = Color(0xFFFEF2F2),
    dangerBorder = Color(0xFFFECACA),
    dangerText = Color(0xFFB91C1C),
    amberSoft = Color(0xFFFFFBEB),
    amberBorder = Color(0xFFFDE68A),
    amberText = Color(0xFF92400E),
    navBar = Color.White,
    navBorder = Color(0xFFE2E8F0),
    navSelected = Color(0xFF2563EB),
    navUnselected = Color(0xFF64748B),
    bannerBrush = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A)),
)

val DarkExtraColors = T14ExtraColors(
    dark = true,
    gradientTop = Color(0xFF101A30),
    cardBorder = Color(0xFF1E293B),
    field = Color(0xFF151D2E),
    fieldBorder = Color(0xFF263247),
    divider = Color(0xFF1E293B),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFFA8B3C5),
    textBody = Color(0xFFCBD5E1),
    chipOn = Color(0xFFF1F5F9),
    chipOnText = Color(0xFF0B1220),
    chipOff = Color(0xFF151D2E),
    chipOffBorder = Color(0xFF263247),
    chipOffText = Color(0xFFCBD5E1),
    coinBg = Color(0xFF2A200A),
    coinBorder = Color(0xFF78350F),
    coinText = Color(0xFFFCD34D),
    progressTrack = Color(0xFF14264D),
    featuredProBg = Color(0xFF78350F),
    featuredProText = Color(0xFFF1F5F9),
    listProBg = Color(0xFF1E293B),
    blueSoft = Color(0xFF14264D),
    blueSoftText = Color(0xFF93C5FD),
    blueBorder = Color(0xFF1E3A8A),
    successSoft = Color(0xFF0F2E1E),
    successText = Color(0xFF4ADE80),
    dangerSoft = Color(0xFF2A1215),
    dangerBorder = Color(0xFF5B1A1F),
    dangerText = Color(0xFFFCA5A5),
    amberSoft = Color(0xFF2A200A),
    amberBorder = Color(0xFF78350F),
    amberText = Color(0xFFFCD34D),
    navBar = Color(0xFF151D2E),
    navBorder = Color(0xFF263247),
    navSelected = Color(0xFF60A5FA),
    navUnselected = Color(0xFF94A3B8),
    bannerBrush = listOf(Color(0xFF1E293B), Color(0xFF1E3A8A)),
)

val LocalT14Colors = androidx.compose.runtime.staticCompositionLocalOf { LightExtraColors }

object T14Theme {
    val colors: T14ExtraColors
        @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalT14Colors.current
}
