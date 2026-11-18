package uz.beko404.track14.admin.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AdminColors {
    val Navy = Color(0xFF0F172A)
    val Blue = Color(0xFF2563EB)
    val Background = Color(0xFFF4F7FB)
    val Success = Color(0xFF22C55E)
    val Danger = Color(0xFFDC2626)
    val Amber = Color(0xFFF59E0B)
}

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
            onSurface = AdminColors.Navy,
            onSurfaceVariant = Color(0xFF64748B),
            surfaceVariant = Color(0xFFF1F5F9),
            outline = Color(0xFFCBD5E1),
            error = AdminColors.Danger,
        ),
        content = content,
    )
}
