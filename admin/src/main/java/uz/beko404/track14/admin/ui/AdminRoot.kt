package uz.beko404.track14.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import uz.beko404.track14.admin.R
import uz.beko404.track14.admin.core.signInWithGoogle
import uz.beko404.track14.admin.feature.ARG_ID
import uz.beko404.track14.admin.feature.AdminSession
import uz.beko404.track14.admin.feature.AppDetailScreen
import uz.beko404.track14.admin.feature.AppsScreen
import uz.beko404.track14.admin.feature.AuditScreen
import uz.beko404.track14.admin.feature.ConfigScreen
import uz.beko404.track14.admin.feature.DashboardScreen
import uz.beko404.track14.admin.feature.MoreScreen
import uz.beko404.track14.admin.feature.PushScreen
import uz.beko404.track14.admin.feature.ReportsScreen
import uz.beko404.track14.admin.feature.SessionViewModel
import uz.beko404.track14.admin.feature.UserDetailScreen
import uz.beko404.track14.admin.feature.UsersScreen
import uz.beko404.track14.admin.logic.Role

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Bosh", Icons.Filled.Home),
    Users("users", "Userlar", Icons.Filled.Person),
    Apps("apps", "Ilovalar", Icons.AutoMirrored.Filled.List),
    Push("push", "Push", Icons.Filled.Notifications),
    More("more", "Ko'proq", Icons.Filled.Menu),
}

@Composable
fun AdminRoot(session: SessionViewModel = hiltViewModel()) {
    AdminTheme {
        when (val s = session.session.collectAsStateWithLifecycle().value) {
            AdminSession.Checking -> Centered { CircularProgressIndicator() }
            AdminSession.SignedOut -> LoginScreen(onSignedIn = session::refresh)
            AdminSession.NotStaff -> Centered {
                Text("Bu akkauntda admin roli yo'q.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    "Rolni superadmin beradi (adminSetRole yoki setRole skripti). Rol berilgandan keyin qayta kiring.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(onClick = session::refresh) { Text("Qayta tekshirish") }
                OutlinedButton(onClick = session::signOut) { Text("Boshqa akkaunt") }
            }
            is AdminSession.Staff -> AdminNav(role = s.role, onSignOut = session::signOut)
        }
    }
}

@Composable
private fun LoginScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val webClientId = stringResource(R.string.google_web_client_id)
    val scope = rememberCoroutineScope()
    Centered {
        Text("Track14 Admin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("Faqat xodimlar uchun. Kirish Google akkaunti va admin roli bilan.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(
            onClick = { scope.launch { if (signInWithGoogle(context, webClientId, FirebaseAuth.getInstance())) onSignedIn() } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Google bilan kirish") }
    }
}

@Composable
private fun AdminNav(role: Role, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val onTab = Tab.entries.any { t -> destination?.hierarchy?.any { it.route == t.route } == true }
    val back: () -> Unit = { nav.popBackStack() }

    Scaffold(
        bottomBar = {
            if (onTab) NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Home.route) {
                DashboardScreen(role, onOpen = { route ->
                    val tab = Tab.entries.firstOrNull { it.route == route }
                    if (tab != null) {
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        nav.navigate(route)
                    }
                })
            }
            composable(Tab.Users.route) { UsersScreen(onOpen = { nav.navigate("user/$it") }) }
            composable(Tab.Apps.route) { AppsScreen(onOpen = { nav.navigate("app/$it") }) }
            composable(Tab.Push.route) { PushScreen(role) }
            composable(Tab.More.route) {
                MoreScreen(
                    role = role,
                    onOpen = { route -> nav.navigate(route) },
                    onSignOut = onSignOut,
                )
            }
            composable("user/{$ARG_ID}", arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType })) {
                UserDetailScreen(role, onBack = back, onPush = { nav.navigate("pushTo/$it") })
            }
            composable("app/{$ARG_ID}", arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType })) {
                AppDetailScreen(role, onBack = back, onOpenOwner = { nav.navigate("user/$it") })
            }
            composable("config/{$ARG_ID}", arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType })) {
                ConfigScreen(role, onBack = back)
            }
            composable("reports") {
                ReportsScreen(role, onBack = back, onOpenTarget = { type, id ->
                    when (type) {
                        "app" -> nav.navigate("app/$id")
                        "user" -> nav.navigate("user/$id")
                    }
                })
            }
            composable("audit") { AuditScreen(onBack = back) }
            composable("pushTo/{$ARG_ID}", arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType })) {
                PushScreen(role, onBack = back)
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) { content() }
}
