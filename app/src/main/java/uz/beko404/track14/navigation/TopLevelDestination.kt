package uz.beko404.track14.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import uz.beko404.track14.R

/** The four bottom-bar tabs. Routes are plain strings; arguments arrive with the first detail screen. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Apps("apps", R.string.tab_apps, Icons.Filled.Home),
    MyTests("my_tests", R.string.tab_my_tests, Icons.Filled.CheckCircle),
    MyApps("my_apps", R.string.tab_my_apps, Icons.Filled.Add),
    Profile("profile", R.string.tab_profile, Icons.Filled.Person),
}
