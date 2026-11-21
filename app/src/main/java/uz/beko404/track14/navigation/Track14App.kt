package uz.beko404.track14.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import uz.beko404.track14.core.designsystem.ScreenBackground
import uz.beko404.track14.core.designsystem.T14Theme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import uz.beko404.track14.core.settings.ThemeMode
import uz.beko404.track14.feature.settings.SettingsRoute
import uz.beko404.track14.feature.testdetail.TestDetailRoute
import uz.beko404.track14.feature.testdetail.TestDetailViewModel
import uz.beko404.track14.feature.testdetail.TesterDetailRoute
import uz.beko404.track14.feature.testdetail.TesterDetailViewModel
import uz.beko404.track14.feature.settings.SettingsViewModel
import uz.beko404.track14.feature.welcome.LanguageScreen
import uz.beko404.track14.feature.welcome.OnboardingScreen
import uz.beko404.track14.feature.welcome.isDark
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.domain.model.GateState
import uz.beko404.track14.feature.gate.GateScreen
import uz.beko404.track14.feature.gate.GateViewModel
import uz.beko404.track14.feature.gate.OptionalUpdateDialog
import uz.beko404.track14.feature.gate.SupportRoute
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uz.beko404.track14.core.designsystem.Track14Theme
import uz.beko404.track14.domain.model.AppIds
import uz.beko404.track14.feature.appdetail.AppDetailRoute
import uz.beko404.track14.feature.appdetail.AppDetailViewModel
import uz.beko404.track14.feature.apps.AppsRoute
import uz.beko404.track14.feature.myapps.ARG_APP_ID
import uz.beko404.track14.feature.myapps.AddAppRoute
import uz.beko404.track14.feature.myapps.DashboardRoute
import uz.beko404.track14.feature.myapps.FeedbackListRoute
import uz.beko404.track14.feature.myapps.MyAppsRoute
import uz.beko404.track14.feature.myapps.PlanRoute
import uz.beko404.track14.feature.jointest.JoinTestRoute
import uz.beko404.track14.feature.jointest.JoinTestViewModel
import uz.beko404.track14.feature.mytests.MyTestsRoute
import uz.beko404.track14.feature.profile.ProfileScreen
import uz.beko404.track14.feature.published.PublishedRoute
import uz.beko404.track14.feature.social.FeedbackRoute
import uz.beko404.track14.feature.social.FeedbackViewModel
import uz.beko404.track14.feature.social.NotificationsRoute
import uz.beko404.track14.feature.social.ReportRoute
import uz.beko404.track14.feature.social.ReportViewModel
import uz.beko404.track14.feature.wallet.ShopRoute
import uz.beko404.track14.feature.wallet.WalletRoute

@Composable
fun Track14App(
    gateViewModel: GateViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val darkTheme = settings.theme.isDark(isSystemInDarkTheme())
    Track14Theme(darkTheme = darkTheme) {
        // First launch: language, then onboarding. Both are shown once.
        if (!settings.languageChosen) {
            LanguageScreen(
                current = settingsViewModel.language,
                darkTheme = darkTheme,
                onToggleTheme = { settingsViewModel.setTheme(if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK) },
                onContinue = settingsViewModel::confirmLanguage,
                onSelect = settingsViewModel::previewLanguage,
            )
            return@Track14Theme
        }
        if (!settings.onboardingDone) {
            OnboardingScreen(onDone = settingsViewModel::completeOnboarding)
            return@Track14Theme
        }
        val gate by gateViewModel.state.collectAsStateWithLifecycle()
        val support by gateViewModel.support.collectAsStateWithLifecycle()
        val context = LocalContext.current
        when (val g = gate) {
            is GateState.UpdateRequired, is GateState.Maintenance, is GateState.Blocked -> {
                GateScreen(g, support, onSignOut = settingsViewModel::signOut, onRetry = { (context as? android.app.Activity)?.recreate() })
                return@Track14Theme
            }
            is GateState.Open -> if (g.optionalUpdate) {
                OptionalUpdateDialog(
                    onUpdate = {
                        gateViewModel.dismissOptionalUpdate()
                        ExternalLinks.openStorePage(context, context.packageName)
                    },
                    onLater = gateViewModel::dismissOptionalUpdate,
                )
            }
            GateState.Loading -> Unit
        }
        val navController = rememberNavController()
        var showSignIn by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        val t14 = T14Theme.colors
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val onTopLevel = TopLevelDestination.entries.any { tab ->
            currentDestination?.hierarchy?.any { it.route == tab.route } == true
        }

        ScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (onTopLevel) NavigationBar(
                    containerColor = t14.navBar,
                    tonalElevation = 0.dp,
                    modifier = Modifier.drawBehind {
                        drawLine(t14.navBorder, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                    },
                ) {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = {
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                Text(
                                    stringResource(tab.label),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = t14.navSelected,
                                selectedTextColor = t14.navSelected,
                                unselectedIconColor = t14.navUnselected,
                                unselectedTextColor = t14.navUnselected,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = TopLevelDestination.Apps.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(TopLevelDestination.Apps.route) {
                    AppsRoute(
                        onOpenApp = { id -> if (AppIds.isValid(id)) navController.navigate("$APP_DETAIL_ROUTE/$id") },
                        onOpenPublished = { navController.navigate(PUBLISHED_ROUTE) },
                        onOpenWallet = { navController.navigate(WALLET_ROUTE) },
                        onSignIn = { showSignIn = true },
                    )
                }
                composable(PUBLISHED_ROUTE) {
                    PublishedRoute(onBack = { navController.popBackStack() })
                }
                composable(
                    route = "$APP_DETAIL_ROUTE/{${AppDetailViewModel.ARG_APP_ID}}",
                    arguments = listOf(navArgument(AppDetailViewModel.ARG_APP_ID) { type = NavType.StringType }),
                ) {
                    AppDetailRoute(
                        onBack = { navController.popBackStack() },
                        onStartTest = { id ->
                            if (AppIds.isValid(id)) navController.navigate("$JOIN_ROUTE/$id")
                        },
                        onReport = { id -> navController.navigate("$REPORT_ROUTE/app/$id") },
                    )
                }
                composable(
                    route = "$JOIN_ROUTE/{${JoinTestViewModel.ARG_CAMPAIGN_ID}}",
                    arguments = listOf(navArgument(JoinTestViewModel.ARG_CAMPAIGN_ID) { type = NavType.StringType }),
                ) {
                    JoinTestRoute(
                        onBack = { navController.popBackStack() },
                        onStarted = {
                            navController.navigate(TopLevelDestination.MyTests.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(TopLevelDestination.MyTests.route) {
                    MyTestsRoute(
                        onFeedback = { id -> navController.navigate("$FEEDBACK_ROUTE/$id") },
                        onOpenTest = { id -> navController.navigate("$TEST_DETAIL_ROUTE/$id") },
                        onOpenWallet = { navController.navigate(WALLET_ROUTE) },
                        onBrowse = {
                            navController.navigate(TopLevelDestination.Apps.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(
                    route = "$TEST_DETAIL_ROUTE/{${TestDetailViewModel.ARG_TEST_ID}}",
                    arguments = listOf(navArgument(TestDetailViewModel.ARG_TEST_ID) { type = NavType.StringType }),
                ) {
                    TestDetailRoute(
                        onBack = { navController.popBackStack() },
                        onFeedback = { id -> navController.navigate("$FEEDBACK_ROUTE/$id") },
                    )
                }
                dialog(
                    route = "$FEEDBACK_ROUTE/{${FeedbackViewModel.ARG_TEST_ID}}",
                    arguments = listOf(navArgument(FeedbackViewModel.ARG_TEST_ID) { type = NavType.StringType }),
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
                ) { FeedbackRoute(onDone = { navController.popBackStack() }) }
                composable(
                    route = "$REPORT_ROUTE/{${ReportViewModel.ARG_TARGET_TYPE}}/{${ReportViewModel.ARG_TARGET_ID}}",
                    arguments = listOf(
                        navArgument(ReportViewModel.ARG_TARGET_TYPE) { type = NavType.StringType },
                        navArgument(ReportViewModel.ARG_TARGET_ID) { type = NavType.StringType },
                    ),
                ) { ReportRoute(onDone = { navController.popBackStack() }) }
                composable(SETTINGS_ROUTE) { SettingsRoute(onBack = { navController.popBackStack() }) }
                composable(SUPPORT_ROUTE) { SupportRoute(onBack = { navController.popBackStack() }) }
                composable(NOTIFICATIONS_ROUTE) { NotificationsRoute(onBack = { navController.popBackStack() }) }
                composable(TopLevelDestination.MyApps.route) {
                    MyAppsRoute(
                        onAddApp = { navController.navigate(ADD_APP_ROUTE) },
                        onOpenDraft = { id -> if (AppIds.isValid(id)) navController.navigate("$PLAN_ROUTE/$id") },
                        onOpenDashboard = { id -> if (AppIds.isValid(id)) navController.navigate("$DASHBOARD_ROUTE/$id") },
                        onOpenWallet = { navController.navigate(WALLET_ROUTE) },
                        onAppeal = { navController.navigate(SUPPORT_ROUTE) },
                    )
                }
                composable(ADD_APP_ROUTE) {
                    AddAppRoute(
                        onBack = { navController.popBackStack() },
                        onCreated = { id ->
                            navController.navigate("$PLAN_ROUTE/$id") { popUpTo(ADD_APP_ROUTE) { inclusive = true } }
                        },
                    )
                }
                composable(
                    route = "$PLAN_ROUTE/{$ARG_APP_ID}",
                    arguments = listOf(navArgument(ARG_APP_ID) { type = NavType.StringType }),
                ) {
                    PlanRoute(
                        onBack = { navController.popBackStack() },
                        onPaid = { id ->
                            navController.navigate("$DASHBOARD_ROUTE/$id") { popUpTo(TopLevelDestination.MyApps.route) }
                        },
                        onBuy = { navController.navigate(SHOP_ROUTE) },
                    )
                }
                composable(
                    route = "$DASHBOARD_ROUTE/{$ARG_APP_ID}",
                    arguments = listOf(navArgument(ARG_APP_ID) { type = NavType.StringType }),
                ) { entry ->
                    DashboardRoute(
                        onBack = { navController.popBackStack() },
                        onReportTester = { testId -> navController.navigate("$REPORT_ROUTE/tester/$testId") },
                        onOpenTester = { testId -> navController.navigate("$TESTER_DETAIL_ROUTE/${entry.arguments?.getString(ARG_APP_ID)}/$testId") },
                        onOpenFeedback = { navController.navigate("$FEEDBACKS_ROUTE/${entry.arguments?.getString(ARG_APP_ID)}") },
                    )
                }
                composable(
                    route = "$FEEDBACKS_ROUTE/{$ARG_APP_ID}",
                    arguments = listOf(navArgument(ARG_APP_ID) { type = NavType.StringType }),
                ) { FeedbackListRoute(onBack = { navController.popBackStack() }) }
                composable(
                    route = "$TESTER_DETAIL_ROUTE/{${TesterDetailViewModel.ARG_APP_ID}}/{${TesterDetailViewModel.ARG_TEST_ID}}",
                    arguments = listOf(
                        navArgument(TesterDetailViewModel.ARG_APP_ID) { type = NavType.StringType },
                        navArgument(TesterDetailViewModel.ARG_TEST_ID) { type = NavType.StringType },
                    ),
                ) {
                    TesterDetailRoute(
                        onBack = { navController.popBackStack() },
                        onReport = { testId -> navController.navigate("$REPORT_ROUTE/tester/$testId") },
                    )
                }
                composable(TopLevelDestination.Profile.route) {
                    ProfileScreen(
                        onOpenWallet = { navController.navigate(WALLET_ROUTE) },
                        onOpenNotifications = { navController.navigate(NOTIFICATIONS_ROUTE) },
                        onOpenSupport = { navController.navigate(SUPPORT_ROUTE) },
                        onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                        onOpenPublished = { navController.navigate(PUBLISHED_ROUTE) },
                        onOpenMyApps = {
                            navController.navigate(TopLevelDestination.MyApps.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(WALLET_ROUTE) {
                    WalletRoute(onBack = { navController.popBackStack() }, onBuy = { navController.navigate(SHOP_ROUTE) })
                }
                composable(SHOP_ROUTE) { ShopRoute(onBack = { navController.popBackStack() }) }
            }
            uz.beko404.track14.feature.auth.SignInSheet(visible = showSignIn, onDismiss = { showSignIn = false })
        }
        }
    }
}

private const val APP_DETAIL_ROUTE = "app"
private const val TEST_DETAIL_ROUTE = "test"
private const val TESTER_DETAIL_ROUTE = "tester"
private const val JOIN_ROUTE = "join"
private const val ADD_APP_ROUTE = "add_app"
private const val PLAN_ROUTE = "plan"
private const val DASHBOARD_ROUTE = "dashboard"
private const val WALLET_ROUTE = "wallet"
private const val SHOP_ROUTE = "shop"
private const val FEEDBACKS_ROUTE = "feedbacks"
private const val FEEDBACK_ROUTE = "feedback"
private const val REPORT_ROUTE = "report"
private const val NOTIFICATIONS_ROUTE = "notifications"
private const val SUPPORT_ROUTE = "support"
private const val SETTINGS_ROUTE = "settings"
private const val PUBLISHED_ROUTE = "published"
