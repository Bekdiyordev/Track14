package uz.beko404.track14.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.t14Vector

/** The four bottom-bar tabs. Routes are plain strings; arguments arrive with the first detail screen. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Apps("apps", R.string.tab_apps, t14Vector("apps", T14Paths.Apps)),
    MyTests("my_tests", R.string.tab_my_tests, t14Vector("tests", T14Paths.Tests)),
    MyApps("my_apps", R.string.tab_my_apps, t14Vector("myApps", T14Paths.MyApps)),
    Profile("profile", R.string.tab_profile, t14Vector("profile", T14Paths.Profile)),
}
