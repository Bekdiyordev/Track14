package uz.beko404.track14.presentation.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import uz.beko404.track14.presentation.common.PlaceholderScreen

@Composable
fun ProfileScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        title = "Profil",
        subtitle = "Hisob, theme, support va community sozlamalari profil ekranida jamlanadi.",
        highlights = listOf(
            "Email orqali sign in va sign out holati.",
            "System, light va dark theme tanlovi.",
            "Support, Track14 Google Group va community linklari.",
        ),
        contentPadding = contentPadding,
    )
}
