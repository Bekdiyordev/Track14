package uz.beko404.track14.presentation.navigation

import androidx.annotation.DrawableRes
import uz.beko404.track14.R

sealed class Track14Destination(
    val route: String,
    val label: String,
    @param:DrawableRes val iconRes: Int,
) {
    data object Home : Track14Destination(
        route = "home",
        label = "Asosiy",
        iconRes = R.drawable.ic_nav_home,
    )

    data object TestingApps : Track14Destination(
        route = "testing_apps",
        label = "Testlar",
        iconRes = R.drawable.ic_nav_testing,
    )

    data object MyApps : Track14Destination(
        route = "my_apps",
        label = "Ilovalarim",
        iconRes = R.drawable.ic_nav_my_apps,
    )

    data object Profile : Track14Destination(
        route = "profile",
        label = "Profil",
        iconRes = R.drawable.ic_nav_profile,
    )

    companion object {
        val bottomBarDestinations = listOf(
            Home,
            TestingApps,
            MyApps,
            Profile,
        )
    }
}
