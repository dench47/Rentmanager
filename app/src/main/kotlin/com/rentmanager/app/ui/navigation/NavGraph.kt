package com.rentmanager.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rentmanager.app.data.local.TokenManager
import com.rentmanager.app.ui.landlord.myproperties.MyPropertiesViewModel
import com.rentmanager.app.ui.auth.verify.VerifyScreen
import com.rentmanager.app.ui.home.HomeScreen
import com.rentmanager.app.ui.role.RoleScreen
import com.rentmanager.app.ui.role.UserRole
import com.rentmanager.app.ui.pin.PinEntryScreen
import com.rentmanager.app.ui.pin.PinSetupScreen
import com.rentmanager.app.ui.settings.SettingsScreen
import com.rentmanager.app.ui.services.ServicesScreen
import com.rentmanager.app.ui.tenant.properties.TenantPropertiesScreen
import com.rentmanager.app.ui.landlord.payment.PaymentScheduleScreen
import com.rentmanager.app.ui.finance.FinanceScreen
import com.rentmanager.app.ui.finance.SubscriptionScreen

@Composable
fun RentManagerNavGraph(
    tokenManager: TokenManager,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val propertiesViewModel: MyPropertiesViewModel = hiltViewModel()

    val accessToken by tokenManager.accessTokenFlow.collectAsState()
    val requirePin by tokenManager.requirePinFlow.collectAsState()
    val startDestination = remember(accessToken, tokenManager.hasPassword, tokenManager.defaultStartScreen, requirePin) {
        if (accessToken == null) {
            Screen.Verify.route
        } else if (requirePin || tokenManager.hasPassword) {
            Screen.PinEntry.route
        } else {
            tokenManager.startRoute()
        }
    }

    // Сохраняем текущий маршрут для возврата после PIN
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route
            if (route != null && route != Screen.PinEntry.route && route != Screen.Verify.route) {
                tokenManager.lastRoute = route
            }
        }
    }

    // Принудительная навигация на PIN при возврате из фона
    LaunchedEffect(requirePin) {
        if (requirePin) {
            navController.navigate(Screen.PinEntry.route) {
                launchSingleTop = true
            }
        }
    }

    // Принудительный разлогин — FCM push стёр токен → мгновенный переход на Verify
    LaunchedEffect(accessToken) {
        if (accessToken == null) {
            navController.navigate(Screen.Verify.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ========== Auth Flow (Verify) ==========
        composable(Screen.Verify.route) {
            VerifyScreen(
                onVerified = { isNewUser ->
                    if (isNewUser) {
                        navController.navigate(Screen.PinSetup.createRoute(onboarding = true)) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.MainScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ========== Home Screen ==========
        composable(Screen.MainScreen.route) {
            HomeScreen(
                onRoleSelected = { role ->
                    val roleType = if (role == UserRole.LANDLORD) "landlord" else "tenant"
                    navController.navigate(Screen.RoleScreen.createRoute(roleType))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ========== Role Screen (unified landlord/tenant) ==========
        composable(
            route = Screen.RoleScreen.route,
            arguments = listOf(navArgument("roleType") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleType = backStackEntry.arguments?.getString("roleType") ?: "landlord"
            val role = if (roleType == "landlord") UserRole.LANDLORD else UserRole.TENANT
            RoleScreen(
                role = role,
                    onNavigateToMyProperties = {
                        navController.navigate(Screen.MyProperties.route)
                    },
                onNavigateToTenants = {
                    navController.navigate(Screen.TenantsList.route)
                },
                onNavigateToOtherProperties = {
                    navController.navigate(Screen.OtherProperties.route)
                },
                onNavigateToFinance = {
                    navController.navigate(Screen.Finance.route)
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToLandlordsList = {
                    navController.navigate(Screen.LandlordsList.route)
                },
                onNavigateToTenantProperties = {
                    navController.navigate(Screen.TenantProperties.route)
                },
                onBackToMain = {
                    navController.navigate(Screen.MainScreen.route) {
                        popUpTo(Screen.MainScreen.route) { inclusive = true }
                    }
                }
            )
        }

        // ========== My Properties ==========
        composable(Screen.MyProperties.route) {
            com.rentmanager.app.ui.landlord.myproperties.MyPropertiesScreen(
                onPropertyClick = { propertyId ->
                    navController.navigate(Screen.PropertyDetail.createRoute(propertyId))
                },
                onCreateProperty = {
                    navController.navigate(Screen.CreateProperty.route)
                },
                onBack = { navController.popBackStack() },
                onFinanceClick = { navController.navigate(Screen.Finance.route) },
                onWriteClick = { navController.navigate(Screen.Messages.route) },
                viewModel = propertiesViewModel
            )
        }

        // ========== Create Property ==========
        composable(Screen.CreateProperty.route) {
            com.rentmanager.app.ui.landlord.createproperty.CreatePropertyScreen(
                onBack = { navController.popBackStack() },
                onCreated = {
                    propertiesViewModel.refresh()
                    navController.popBackStack()
                },
                onPaymentSchedule = { navController.navigate(Screen.PaymentSchedule.route) }
            )
        }

        // ========== Edit Property ==========
        composable(
            route = Screen.EditProperty.route,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
            com.rentmanager.app.ui.landlord.createproperty.CreatePropertyScreen(
                propertyId = propertyId,
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }

        // ========== Attach Tenant ==========
        composable(
            route = Screen.AttachTenant.route,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
            com.rentmanager.app.ui.landlord.myproperties.propertydetail.AttachTenantScreen(
                propertyId = propertyId,
                onDismiss = { navController.popBackStack() },
                onAttached = { navController.popBackStack() }
            )
        }

        // ========== Meter Detail ==========
        composable(
            route = Screen.MeterDetail.route,
            arguments = listOf(
                navArgument("propertyId") { type = NavType.StringType },
                navArgument("meterId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
            val meterId = backStackEntry.arguments?.getString("meterId") ?: ""
            com.rentmanager.app.ui.landlord.myproperties.propertydetail.MeterDetailScreen(
                propertyId = propertyId,
                meterId = meterId,
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Property Detail ==========
        composable(
            route = Screen.PropertyDetail.route,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
            com.rentmanager.app.ui.property.detail.PropertyDetailScreen(
                propertyId = propertyId,
                onBack = { navController.popBackStack() },
                onAddMeter = { },
                onCall = { },
                onWrite = {
                    navController.navigate(Screen.Chat.createRoute("landlord"))
                },
                onAttachTenant = {
                    navController.navigate(Screen.AttachTenant.createRoute(propertyId))
                },
                onEdit = {
                    navController.navigate(Screen.EditProperty.createRoute(propertyId))
                }
            )
        }

        // ========== Tenant Properties ==========
        composable(Screen.TenantProperties.route) {
            TenantPropertiesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Payment Schedule ==========
        composable(Screen.PaymentSchedule.route) {
            PaymentScheduleScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Tenants List ==========
        composable(Screen.TenantsList.route) {
            com.rentmanager.app.ui.landlord.tenants.TenantsListScreen(
                onTenantClick = { tenantId ->
                    navController.navigate(Screen.TenantDetail.createRoute(tenantId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Tenant Detail ==========
        composable(
            route = Screen.TenantDetail.route,
            arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tenantId = backStackEntry.arguments?.getString("tenantId") ?: ""
            com.rentmanager.app.ui.landlord.tenants.TenantDetailScreen(
                tenantId = tenantId,
                onBack = { navController.popBackStack() },
                onPropertyClick = { propertyId ->
                    navController.navigate(Screen.PropertyDetail.createRoute(propertyId))
                }
            )
        }

        // ========== Landlords List ==========
        composable(Screen.LandlordsList.route) {
            com.rentmanager.app.ui.landlord.otherproperties.LandlordsListScreen(
                onLandlordClick = { landlordId ->
                    // Navigate to landlord detail (to be implemented)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Other Properties ==========
        composable(Screen.OtherProperties.route) {
            com.rentmanager.app.ui.landlord.otherproperties.OtherPropertiesScreen(
                onPropertyClick = { propertyId ->
                    navController.navigate(Screen.OtherPropertyDetail.createRoute(propertyId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.OtherPropertyDetail.route,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
            com.rentmanager.app.ui.landlord.otherproperties.OtherPropertyDetailScreen(
                propertyId = propertyId,
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Finance ==========
        composable(Screen.Finance.route) {
            FinanceScreen(
                onBack = { navController.popBackStack() },
                onSubscription = { navController.navigate(Screen.Subscription.route) }
            )
        }

        // ========== Subscription ==========
        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Messages ==========
        composable(Screen.Messages.route) {
            com.rentmanager.app.ui.landlord.messages.MessagesScreen(
                onChatClick = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Chat ==========
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            com.rentmanager.app.ui.landlord.messages.ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Pin Setup ==========
        composable(
            route = Screen.PinSetup.route,
            arguments = listOf(navArgument("onboarding") { type = NavType.BoolType; defaultValue = false })
        ) { backStackEntry ->
            val onboarding = backStackEntry.arguments?.getBoolean("onboarding") ?: false
            PinSetupScreen(
                isOnboarding = onboarding,
                onBack = {
                    if (onboarding) {
                        navController.navigate(Screen.MainScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Settings.route)
                    }
                }
            )
        }

        // ========== Pin Entry ==========
        composable(Screen.PinEntry.route) {
            PinEntryScreen(
                tokenManager = tokenManager,
                onPinVerified = {
                    tokenManager.requirePin = false
                    if (tokenManager.accessToken == null) {
                        // Принудительный разлогин — на верификацию
                        navController.navigate(Screen.Verify.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else if (navController.previousBackStackEntry != null) {
                        // PIN был показан поверх другого экрана (таймер/фон) — просто возврат
                        navController.popBackStack()
                    } else {
                        // PIN — стартовый экран (холодный запуск) — навигация вперёд
                        val destination = tokenManager.lastRoute ?: tokenManager.startRoute()
                        tokenManager.lastRoute = null
                        navController.navigate(destination) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ========== Settings ==========
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.navigate(Screen.MainScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLoggedOut = {
                    navController.navigate(Screen.Verify.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onPinSetupClick = {
                    navController.navigate(Screen.PinSetup.createRoute())
                },
                onNavigateToPhoneVerify = { phone ->
                    navController.navigate(Screen.Verify.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ========== Services ==========
        composable(Screen.Services.route) {
            ServicesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun TokenManager.startRoute(): String = when (defaultStartScreen) {
    "landlord" -> Screen.RoleScreen.createRoute("landlord")
    "tenant" -> Screen.RoleScreen.createRoute("tenant")
    else -> Screen.MainScreen.route
}