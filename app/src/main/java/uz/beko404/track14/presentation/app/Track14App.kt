package uz.beko404.track14.presentation.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uz.beko404.track14.domain.model.AuthSession
import uz.beko404.track14.presentation.auth.AuthUiState
import uz.beko404.track14.presentation.navigation.Track14Destination
import uz.beko404.track14.presentation.navigation.Track14NavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Track14App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    var signedInEmail by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val authState = AuthUiState(
        session = signedInEmail?.let { email ->
            AuthSession(
                userId = "auth-${email.hashCode()}",
                email = email,
                displayName = email.substringBefore('@'),
            )
        },
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Track14")
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Track14Destination.bottomBarDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(Track14Destination.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = destination.label,
                            )
                        },
                        label = {
                            Text(text = destination.label)
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Track14NavGraph(
            navController = navController,
            contentPadding = innerPadding,
            authState = authState,
            onSignIn = { email, _ ->
                signedInEmail = email
            },
            onSignOut = {
                signedInEmail = null
            },
        )
    }
}
