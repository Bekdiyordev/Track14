package uz.beko404.track14.presentation.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import uz.beko404.track14.presentation.common.PlaceholderScreen

@Composable
fun HomeScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        title = "Asosiy",
        subtitle = "Google Play yopiq testiga tester kerak bo'lgan ilovalar shu yerda ko'rinadi.",
        highlights = listOf(
            "Ranking bo'yicha saralangan ilovalar ro'yxati.",
            "Testerlar soni va kerakli tester ehtiyoji.",
            "Keyingi stepda fake data bilan AppCard lar ulanadi.",
        ),
        contentPadding = contentPadding,
    )
}
