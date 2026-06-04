package uz.beko404.track14.presentation.myapps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import uz.beko404.track14.presentation.common.PlaceholderScreen

@Composable
fun MyAppsScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        title = "Ilovalarim",
        subtitle = "O'z ilovalaringiz, Google Group linklari va testerlar holati shu yerda bo'ladi.",
        highlights = listOf(
            "Free limit: MVP da 3 tagacha ilova.",
            "Device ichidan installed app tanlash oqimi.",
            "Owner testerlarni finish yoki reject qila oladi.",
        ),
        contentPadding = contentPadding,
    )
}
