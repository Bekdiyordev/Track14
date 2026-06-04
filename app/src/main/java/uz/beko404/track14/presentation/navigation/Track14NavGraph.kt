package uz.beko404.track14.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import uz.beko404.track14.presentation.appdetail.AppDetailScreen
import uz.beko404.track14.presentation.auth.AuthUiState
import uz.beko404.track14.presentation.home.HomeScreen
import uz.beko404.track14.presentation.myapps.MyAppsScreen
import uz.beko404.track14.presentation.profile.ProfileScreen
import uz.beko404.track14.presentation.testing.TestingAppsScreen

@Composable
fun Track14NavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues,
    authState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onSignOut: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Track14Destination.Home.route,
    ) {
        composable(route = Track14Destination.Home.route) {
            HomeScreen(
                contentPadding = contentPadding,
                onAppClick = { appId ->
                    navController.navigate(Track14Destination.AppDetail.createRoute(appId))
                },
            )
        }

        composable(route = Track14Destination.TestingApps.route) {
            TestingAppsScreen(
                contentPadding = contentPadding,
                authState = authState,
                onSignIn = onSignIn,
            )
        }

        composable(route = Track14Destination.MyApps.route) {
            MyAppsScreen(
                contentPadding = contentPadding,
                authState = authState,
                onSignIn = onSignIn,
            )
        }

        composable(route = Track14Destination.Profile.route) {
            ProfileScreen(
                contentPadding = contentPadding,
                authState = authState,
                onSignIn = onSignIn,
                onSignOut = onSignOut,
            )
        }

        composable(
            route = Track14Destination.AppDetail.route,
            arguments = listOf(
                navArgument(Track14Destination.AppDetail.appIdArg) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val appId = backStackEntry.arguments
                ?.getString(Track14Destination.AppDetail.appIdArg)
                ?.let(Uri::decode)
                .orEmpty()

            AppDetailScreen(
                appId = appId,
                contentPadding = contentPadding,
                authState = authState,
                onSignIn = onSignIn,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
