package uz.beko404.track14.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
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
import uz.beko404.track14.admin.core.GoogleSignInResult
import uz.beko404.track14.admin.core.signInWithGoogle
import uz.beko404.track14.admin.feature.ARG_ID
import uz.beko404.track14.admin.feature.AdminSession
import uz.beko404.track14.admin.feature.AppDetailScreen
import uz.beko404.track14.admin.feature.AppsScreen
import uz.beko404.track14.admin.feature.AuditScreen
import uz.beko404.track14.admin.feature.ConfigScreen
import uz.beko404.track14.admin.feature.DashboardScreen
import uz.beko404.track14.admin.feature.MoreScreen
import uz.beko404.track14.admin.feature.PushHistoryScreen
import uz.beko404.track14.admin.feature.PushScreen
import uz.beko404.track14.admin.feature.ReportsScreen
import uz.beko404.track14.admin.feature.SessionViewModel
import uz.beko404.track14.admin.feature.UserDetailScreen
import uz.beko404.track14.admin.feature.UsersScreen
import uz.beko404.track14.admin.logic.Role

/** Five tabs; detail routes light up the tab they belong to (prefixes). */
private enum class Tab(val route: String, val label: String, val icon: String?, val prefixes: List<String>) {
    Home("home", "Bosh", AdminPaths.Home, listOf("audit")),
    Users("users", "Userlar", AdminPaths.Users, listOf("user/")),
    Apps("apps", "Ilovalar", AdminPaths.Apps, listOf("app/")),
    Push("push", "Push", AdminPaths.Bell, listOf("pushTo/", "pushHistory")),
    More("more", "Ko'proq", null, listOf("reports", "config/")),
}

private fun Tab.matches(destination: NavDestination?): Boolean {
    val route = destination?.route ?: return false
    return route == this.route || prefixes.any { route.startsWith(it) }
}

@Composable
fun AdminRoot(session: SessionViewModel = hiltViewModel()) {
    AdminTheme {
        when (val s = session.session.collectAsStateWithLifecycle().value) {
            AdminSession.Checking -> NavyScreen { CircularProgressIndicator(color = Color.White) }
            AdminSession.SignedOut -> LoginScreen()
            AdminSession.CheckFailed -> NavyScreen {
                NavyMessage("Admin rolini tekshirib bo'lmadi", "Internetni tekshirib, qayta urinib ko'ring.")
                WhiteButton("Qayta tekshirish", session::refresh)
                GhostButton("Boshqa akkaunt", session::signOut)
            }
            AdminSession.NotStaff -> NavyScreen {
                NavyMessage(
                    "Bu akkauntda admin roli yo'q",
                    "Rolni superadmin beradi (adminSetRole yoki setRole skripti). Rol berilgandan keyin qayta kiring.",
                )
                WhiteButton("Qayta tekshirish", session::refresh)
                GhostButton("Boshqa akkaunt", session::signOut)
            }
            is AdminSession.Staff -> AdminNav(role = s.role, onSignOut = session::signOut)
        }
    }
}

/** Full navy screen with centered content (login and role checks). */
@Composable
private fun NavyScreen(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(AdminColors.Navy).systemBarsPadding().padding(start = 28.dp, end = 28.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        content = content,
    )
}

@Composable
private fun NavyMessage(title: String, body: String) {
    AdminLogo(72.dp)
    Text(title, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, textAlign = TextAlign.Center)
    Text(body, color = AdminColors.Faint, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
}

@Composable
private fun WhiteButton(text: String, onClick: () -> Unit, enabled: Boolean = true, leading: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.padding(top = 8.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) Color.White else Color(0xFFCBD5E1))
            .clickable(enabled = enabled, role = SemanticsRole.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(text, color = AdminColors.Navy, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
    Text(
        text, color = AdminColors.Faint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun LoginScreen() {
    val context = LocalContext.current
    val webClientId = stringResource(R.string.google_web_client_id)
    val scope = rememberCoroutineScope()
    var signingIn by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    NavyScreen {
        AdminLogo(96.dp)
        Text(
            buildAnnotatedString {
                append("Track14 ")
                withStyle(SpanStyle(color = Color(0xFF60A5FA))) { append("Admin") }
            },
            color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, modifier = Modifier.padding(top = 8.dp),
        )
        Text("Faqat vakolatli adminlar uchun. Oddiy foydalanuvchilar bu yerga kira olmaydi.", color = AdminColors.Faint, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
        WhiteButton(
            if (signingIn) "Kirilmoqda…" else "Google bilan kirish",
            onClick = {
                scope.launch {
                    signingIn = true
                    error = null
                    try {
                        when (val result = signInWithGoogle(context, webClientId, FirebaseAuth.getInstance())) {
                            GoogleSignInResult.SignedIn, GoogleSignInResult.Cancelled -> Unit
                            is GoogleSignInResult.Failed -> error = result.message
                        }
                    } finally {
                        signingIn = false
                    }
                }
            },
            enabled = !signingIn,
            leading = { AdminIcon("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", size = 22.dp, tint = AdminColors.Blue, strokeWidth = 3f) },
        )
        error?.let { Text(it, color = Color(0xFFFCA5A5), fontSize = 13.sp, textAlign = TextAlign.Center) }
        Text("Kirgandan keyin akkauntda admin huquqi tekshiriladi. Huquq bo'lmasa, kirish rad etiladi.", color = AdminColors.Muted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AdminNav(role: Role, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val back: () -> Unit = { nav.popBackStack() }
    val goTab: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = AdminColors.Background,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Color.White)
                    .drawBehind { drawLine(AdminColors.Border, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                    .navigationBarsPadding().height(76.dp),
            ) {
                Tab.entries.forEach { tab ->
                    val selected = tab.matches(destination)
                    val color = if (selected) AdminColors.Blue else AdminColors.Muted
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable(role = SemanticsRole.Tab) { goTab(tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        if (tab.icon != null) AdminIcon(tab.icon, tint = color) else DotsIcon(color)
                        Text(tab.label, color = color, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Home.route) {
                DashboardScreen(role, onOpen = { route ->
                    if (Tab.entries.any { it.route == route }) goTab(route) else nav.navigate(route)
                })
            }
            composable(Tab.Users.route) { UsersScreen(onOpen = { nav.navigate("user/$it") }) }
            composable(Tab.Apps.route) { AppsScreen(onOpen = { nav.navigate("app/$it") }) }
            composable(Tab.Push.route) { PushScreen(role, onHistory = { nav.navigate("pushHistory") }) }
            composable("pushHistory") { PushHistoryScreen(role, onBack = back, onNew = back) }
            composable(Tab.More.route) {
                MoreScreen(role = role, onOpen = { route -> if (Tab.entries.any { it.route == route }) goTab(route) else nav.navigate(route) }, onSignOut = onSignOut)
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
                PushScreen(role, onBack = back, onHistory = { nav.navigate("pushHistory") })
            }
        }
    }
}
