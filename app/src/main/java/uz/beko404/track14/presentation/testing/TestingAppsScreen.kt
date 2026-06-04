package uz.beko404.track14.presentation.testing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import uz.beko404.track14.presentation.common.PlaceholderScreen

@Composable
fun TestingAppsScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        title = "Testlar",
        subtitle = "Siz qo'shilgan testlar va 14 kunlik davomiylik shu tabda yuritiladi.",
        highlights = listOf(
            "Har bir joined app uchun bugungi holat.",
            "Start Test tugmasi va 30 soniya qoidasi.",
            "14 kunlik streak grid keyingi UI foundation stepida qo'shiladi.",
        ),
        contentPadding = contentPadding,
    )
}
