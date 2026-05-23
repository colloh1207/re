package com.sdd.marketplace.core.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import com.sdd.marketplace.feature.boost.ui.BoostScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.sdd.marketplace.core.ui.components.SddBottomNavBar
import com.sdd.marketplace.feature.auth.ui.screens.*
import com.sdd.marketplace.feature.auth.viewmodel.AuthViewModel
import com.sdd.marketplace.feature.chat.ui.screens.*
import com.sdd.marketplace.feature.followers.ui.FollowersScreen
import com.sdd.marketplace.feature.home.ui.screens.HomeScreen
import com.sdd.marketplace.feature.kyc.ui.KycVerificationScreen
import com.sdd.marketplace.feature.notifications.ui.NotificationsScreen
import com.sdd.marketplace.feature.orders.ui.*
import com.sdd.marketplace.feature.product.ui.screens.*
import com.sdd.marketplace.feature.profile.ui.screens.*
import com.sdd.marketplace.feature.search.ui.screens.SearchScreen
import com.sdd.marketplace.feature.settings.ui.screens.*
import com.sdd.marketplace.feature.static.ui.*
import com.sdd.marketplace.feature.telegram.ui.TelegramBotScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object OtpVerify : Screen("otp_verify/{phone}") { fun createRoute(phone: String) = "otp_verify/$phone" }
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object ProductDetail : Screen("product/{productId}") { fun createRoute(id: String) = "product/$id" }
    object PostProduct : Screen("post_product")
    object EditProduct : Screen("edit_product/{productId}") { fun createRoute(id: String) = "edit_product/$id" }
    object Inbox : Screen("inbox")
    object ChatDetail : Screen("chat/{chatId}") { fun createRoute(id: String) = "chat/$id" }
    object Profile : Screen("profile?userId={userId}") { fun createRoute(id: String? = null) = if (id != null) "profile?userId=$id" else "profile" }
    object EditProfile : Screen("edit_profile")
    object Search : Screen("search")
    object Notifications : Screen("notifications")
    object Wishlist : Screen("wishlist")
    object SellerProfile : Screen("seller/{sellerId}") { fun createRoute(id: String) = "seller/$id" }
    object Followers : Screen("followers/{userId}/{tab}") { fun createRoute(id: String, tab: String = "followers") = "followers/$id/$tab" }
    object Orders : Screen("orders")
    object OrderDetail : Screen("order/{orderId}") { fun createRoute(id: String) = "order/$id" }
    object Payment : Screen("payment/{orderId}") { fun createRoute(id: String) = "payment/$id" }
    object Wallet : Screen("wallet")
    object Boost : Screen("boost/{productId}") {
        fun createRoute(id: String = "") = "boost/$id"
    }
    object Achievements : Screen("achievements")
    object InviteEarn : Screen("invite_earn")
    object SellerShop : Screen("seller_shop/{userId}") { fun createRoute(id: String) = "seller_shop/$id" }
    object Settings : Screen("settings")
    object ChangePassword : Screen("change_password")
    object ChangeEmail : Screen("change_email")
    object DeleteAccount : Screen("delete_account")
    object ConfirmLogout : Screen("confirm_logout")
    object SwitchAccount : Screen("switch_account")
    object ChangeLanguage : Screen("change_language")
    object KycVerification : Screen("kyc_verification")
    object TelegramBot : Screen("telegram_bot")
    object TermsConditions : Screen("terms_conditions")
    object PrivacyPolicy : Screen("privacy_policy")
    object SellerTerms : Screen("seller_terms")
    object BuyerTerms : Screen("buyer_terms")
    object HelpSupport : Screen("help_support")
    object ReportBug : Screen("report_bug")
    object RateApp : Screen("rate_app")
    object MyReviews : Screen("my_reviews")
    object SavedItems : Screen("saved_items")
    object MySoldItems : Screen("my_sold_items")
    object Coupons : Screen("coupons")
    object PrivacySettings : Screen("privacy_settings")
    object ModerationPanel : Screen("moderation_panel")
    object SuspensionNotice : Screen("suspension_notice")
    object Appeal : Screen("appeal")
    object PaymentSuccess : Screen("payment_success/{orderId}?amount={amount}") {
        fun createRoute(orderId: String, amount: String = "") =
            "payment_success/$orderId?amount=${java.net.URLEncoder.encode(amount, "UTF-8")}"
    }
    object KycApproved : Screen("kyc_approved")
}

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, "home", "home_outlined"),
    BottomNavItem("Chats", Screen.Inbox.route, "chat_bubble", "chat_bubble_outline"),
    BottomNavItem("Post", Screen.PostProduct.route, "add", "add"),
    BottomNavItem("Profile", Screen.Profile.createRoute(), "person", "person_outlined"),
    BottomNavItem("Search", Screen.Search.route, "search", "search")
)

data class BottomNavItem(val label: String, val route: String, val selectedIcon: String, val unselectedIcon: String)

val protectedRoutes = setOf(
    Screen.Inbox.route,
    Screen.PostProduct.route,
    Screen.EditProfile.route,
    Screen.Achievements.route,
    Screen.InviteEarn.route,
    Screen.Orders.route,
    Screen.Notifications.route,
    Screen.KycVerification.route,
    Screen.MyReviews.route,
    Screen.SavedItems.route,
    Screen.MySoldItems.route
)

@Composable
fun SddNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (authState.isAuthenticated) Screen.Home.route else Screen.Login.route,
        enterTransition = { slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing), initialOffsetX = { it / 3 }) + fadeIn(tween(200)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing), targetOffsetX = { -it / 3 }) + fadeOut(tween(200)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing), initialOffsetX = { -it / 3 }) + fadeIn(tween(200)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing), targetOffsetX = { it / 3 }) + fadeOut(tween(200)) }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgot = { navController.navigate(Screen.ForgotPassword.route) },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onNavigateToOtp = { phone -> navController.navigate(Screen.OtpVerify.createRoute(phone)) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onNavigateToOtp = { phone -> navController.navigate(Screen.OtpVerify.createRoute(phone)) }
            )
        }
        composable(
            Screen.OtpVerify.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType; defaultValue = "" })
        ) {
            OtpVerifyScreen(
                onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOtp = {
                    navController.navigate(Screen.OtpVerify.createRoute("email")) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) { MainScaffold(navController, authViewModel) { HomeScreen(navController) } }

        composable(Screen.Inbox.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                MainScaffold(navController, authViewModel) { InboxScreen(navController) }
            }
        }

        composable(Screen.PostProduct.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                PostProductScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPostSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.PostProduct.route) { inclusive = true } } },
                    onBoostListing = { productId ->
                        navController.navigate(Screen.Boost.createRoute(productId)) {
                            popUpTo(Screen.PostProduct.route) { inclusive = true }
                        }
                    },
                    onNavigateToKyc = { navController.navigate(Screen.KycVerification.route) }
                )
            }
        }

        composable(
            Screen.Boost.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                BoostScreen(
                    preSelectedProductId = backStackEntry.arguments?.getString("productId")?.takeIf { it.isNotBlank() },
                    onNavigateBack       = { navController.popBackStack() },
                    onNavigateHome       = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            Screen.EditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                EditProductScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditSuccess = { navController.popBackStack() }
                )
            }
        }

        composable(
            Screen.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) {
            val userId = it.arguments?.getString("userId")
            if (userId == null && !authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                MainScaffold(navController, authViewModel) { ProfileScreen(navController) }
            }
        }

        composable(Screen.Search.route) { MainScaffold(navController, authViewModel) { SearchScreen(navController) } }

        composable(
            Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { ProductDetailScreen(navController) }

        composable(
            Screen.ChatDetail.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                ChatDetailScreen(navController)
            }
        }

        composable(Screen.Notifications.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                NotificationsScreen(navController)
            }
        }

        composable(Screen.Wishlist.route) { WishlistScreen(navController) }
        composable(Screen.SavedItems.route) { WishlistScreen(navController) }

        composable(Screen.EditProfile.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                EditProfileScreen(navController)
            }
        }

        composable(Screen.Achievements.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                AchievementsScreen(navController)
            }
        }

        composable(Screen.InviteEarn.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                InviteEarnScreen(navController)
            }
        }

        composable(
            Screen.SellerShop.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { SellerShopScreen(navController) }

        composable(Screen.Orders.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                OrdersScreen(navController)
            }
        }

        composable(
            Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { OrderDetailScreen(navController) }

        composable(Screen.MyReviews.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                MyReviewsScreen(navController)
            }
        }

        composable(Screen.MySoldItems.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                MySoldItemsScreen(navController)
            }
        }

        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.ChangePassword.route) { ChangePasswordScreen(navController) }
        composable(Screen.ChangeEmail.route) { ChangeEmailScreen(navController) }
        composable(Screen.DeleteAccount.route) { DeleteAccountScreen(navController) }
        composable(Screen.SwitchAccount.route) { SwitchAccountScreen(navController) }
        composable(Screen.ChangeLanguage.route) { ChangeLanguageScreen(navController) }

        composable(Screen.ConfirmLogout.route) {
            AlertDialog(
                onDismissRequest = { navController.popBackStack() },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to sign out?") },
                confirmButton = {
                    Button(onClick = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }) { Text("Sign Out") }
                },
                dismissButton = { TextButton(onClick = { navController.popBackStack() }) { Text("Cancel") } }
            )
        }

        composable(Screen.KycVerification.route) { KycVerificationScreen(navController) }

        composable(Screen.TelegramBot.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                TelegramBotScreen(navController)
            }
        }

        composable(Screen.Coupons.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                com.sdd.marketplace.feature.profile.ui.screens.CouponsScreen(navController)
            }
        }

        composable(Screen.PrivacySettings.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                com.sdd.marketplace.feature.profile.ui.screens.PrivacySettingsScreen(navController)
            }
        }

        composable(Screen.ModerationPanel.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                com.sdd.marketplace.feature.moderation.ui.ModerationScreen(navController)
            }
        }

        composable(Screen.SuspensionNotice.route) {
            com.sdd.marketplace.feature.moderation.ui.SuspendedScreen(navController)
        }

        composable(Screen.Appeal.route) {
            if (!authState.isAuthenticated) {
                LoginRequiredScreen { navController.navigate(Screen.Login.route) }
            } else {
                com.sdd.marketplace.feature.moderation.ui.AppealScreen(
                    onSubmitted = { navController.navigate(Screen.SuspensionNotice.route) { popUpTo(Screen.Appeal.route) { inclusive = true } } },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            Screen.PaymentSuccess.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType; defaultValue = "" },
                navArgument("amount") { type = NavType.StringType; defaultValue = "" }
            )
        ) { back ->
            val orderId = back.arguments?.getString("orderId") ?: ""
            val amount = back.arguments?.getString("amount") ?: ""
            com.sdd.marketplace.feature.product.ui.screens.PaymentSuccessScreen(
                orderId = orderId,
                amount = if (amount.isNotBlank()) java.net.URLDecoder.decode(amount, "UTF-8") else "",
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = false } }
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.Orders.route) { popUpTo(Screen.PaymentSuccess.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.KycApproved.route) {
            com.sdd.marketplace.feature.kyc.ui.KycApprovedScreen(
                onContinue = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.KycApproved.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.TermsConditions.route) { TermsConditionsScreen(navController) }
        composable(Screen.PrivacyPolicy.route) { PrivacyPolicyScreen(navController) }
        composable(Screen.SellerTerms.route) { TermsConditionsScreen(navController) }
        composable(Screen.BuyerTerms.route) { TermsConditionsScreen(navController) }
        composable(Screen.HelpSupport.route) { HelpSupportScreen(navController) }
        composable(Screen.ReportBug.route) { ReportBugScreen(navController) }
        composable(Screen.RateApp.route) { RateAppScreen(navController) }

        composable(
            Screen.Followers.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("tab") { type = NavType.StringType; defaultValue = "followers" }
            )
        ) { FollowersScreen(navController) }
    }
}

@Composable
fun LoginRequiredScreen(onNavigateToLogin: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = "Login Required",
                modifier = Modifier.size(64.dp),
                tint = com.sdd.marketplace.core.ui.theme.SddPink
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Sign in required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Please sign in to access this feature",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNavigateToLogin,
                colors = ButtonDefaults.buttonColors(containerColor = com.sdd.marketplace.core.ui.theme.SddPink)
            ) { Text("Sign In") }
        }
    }
}

@Composable
fun MainScaffold(navController: NavController, authViewModel: AuthViewModel, content: @Composable () -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    Scaffold(
        bottomBar = {
            SddBottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) { launchSingleTop = true; restoreState = true }
                }
            )
        }
    ) { _ -> content() }
}
