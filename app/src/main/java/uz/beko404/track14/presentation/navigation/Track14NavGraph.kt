package uz.beko404.track14.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import uz.beko404.track14.presentation.home.HomeScreen
import uz.beko404.track14.presentation.myapps.MyAppsScreen
import uz.beko404.track14.presentation.profile.ProfileScreen
import uz.beko404.track14.presentation.testing.TestingAppsScreen

@Composable
fun Track14NavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = Track14Destination.Home.route,
    ) {
        composable(route = Track14Destination.Home.route) {
            HomeScreen(contentPadding = contentPadding)
        }

        composable(route = Track14Destination.TestingApps.route) {
            TestingAppsScreen(contentPadding = contentPadding)
        }

        composable(route = Track14Destination.MyApps.route) {
            MyAppsScreen(contentPadding = contentPadding)
        }

        composable(route = Track14Destination.Profile.route) {
            ProfileScreen(contentPadding = contentPadding)
        }
    }
}
