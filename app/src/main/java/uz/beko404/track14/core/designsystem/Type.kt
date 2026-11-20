package uz.beko404.track14.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.beko404.track14.R

/** Headings: Manrope (canvas: 26/800 screen titles, 17–18/800 section titles). */
val Manrope = FontFamily(
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/** Body and controls: Inter (canvas: 15/400 body, 13 secondary, 16/600 buttons). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun manrope(size: Int, line: Int, weight: FontWeight = FontWeight.ExtraBold) =
    TextStyle(fontFamily = Manrope, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun inter(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

/** Sizes taken from the design canvas, mapped onto the Material slots the screens use. */
val Track14Typography = Typography(
    displaySmall = manrope(32, 38),
    headlineLarge = manrope(28, 34),
    headlineMedium = manrope(26, 32),
    headlineSmall = manrope(22, 28),
    titleLarge = manrope(20, 26),
    titleMedium = manrope(17, 22),
    titleSmall = manrope(15, 20, FontWeight.Bold),
    bodyLarge = inter(16, 24),
    bodyMedium = inter(15, 22),
    bodySmall = inter(13, 19),
    labelLarge = inter(16, 20, FontWeight.SemiBold),
    labelMedium = inter(13, 16, FontWeight.SemiBold),
    labelSmall = inter(11, 14, FontWeight.SemiBold),
)
