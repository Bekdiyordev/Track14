package uz.beko404.track14.admin.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.beko404.track14.admin.R

/** Admin canvas palette (A-screens): navy header, slate text, blue accent. */
object AdminColors {
    val Navy = Color(0xFF0F172A)
    val Blue = Color(0xFF2563EB)
    val BlueDark = Color(0xFF1D4ED8)
    val BlueSoft = Color(0xFFEFF6FF)
    val Background = Color(0xFFF4F7FB)
    val Border = Color(0xFFE2E8F0)
    val Divider = Color(0xFFF1F5F9)
    val Text = Color(0xFF0F172A)
    val Body = Color(0xFF334155)
    val Muted = Color(0xFF64748B)
    val Faint = Color(0xFF94A3B8)
    val Success = Color(0xFF15803D)
    val SuccessSoft = Color(0xFFDCFCE7)
    val Danger = Color(0xFFDC2626)
    val DangerText = Color(0xFFB91C1C)
    val DangerSoft = Color(0xFFFEE2E2)
    val DangerBorder = Color(0xFFFECACA)
    val Amber = Color(0xFFF59E0B)
    val AmberText = Color(0xFF92400E)
    val AmberSoft = Color(0xFFFEF3C7)
}

val Manrope = FontFamily(
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun inter(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Inter, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

private fun manrope(size: Int, line: Int) =
    TextStyle(fontFamily = Manrope, fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight.ExtraBold)

private val AdminTypography = Typography(
    headlineMedium = manrope(24, 30),
    headlineSmall = manrope(22, 28),
    titleLarge = manrope(19, 24),
    titleMedium = manrope(17, 22),
    titleSmall = inter(15, 20, FontWeight.SemiBold),
    bodyLarge = inter(16, 24),
    bodyMedium = inter(15, 22),
    bodySmall = inter(13, 19),
    labelLarge = inter(15, 20, FontWeight.SemiBold),
    labelMedium = inter(13, 18, FontWeight.SemiBold),
    labelSmall = inter(11, 14, FontWeight.SemiBold),
)

/** Light only: an internal tool, used on a few staff phones. */
@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AdminColors.Blue,
            onPrimary = Color.White,
            secondary = AdminColors.Navy,
            background = AdminColors.Background,
            surface = Color.White,
            onSurface = AdminColors.Text,
            onBackground = AdminColors.Text,
            onSurfaceVariant = AdminColors.Muted,
            surfaceVariant = AdminColors.Divider,
            outline = Color(0xFFCBD5E1),
            error = AdminColors.Danger,
        ),
        typography = AdminTypography,
        content = content,
    )
}
