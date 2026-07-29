package com.rentmanager.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rentmanager.app.ui.auth.code.SmsCodeScreen
import com.rentmanager.app.ui.auth.code.SmsCodeViewModel
import com.rentmanager.app.ui.auth.name.EnterNameScreen
import com.rentmanager.app.ui.auth.phone.PhoneNumberScreen
import com.rentmanager.app.ui.auth.phone.PhoneNumberViewModel
import com.rentmanager.app.ui.home.HomeScreen
import com.rentmanager.app.ui.keyboard.KeyboardScreen
import com.rentmanager.app.ui.landlord.main.LandlordScreen
import com.rentmanager.app.ui.property.detail.PropertyDetailScreen
import com.rentmanager.app.ui.settings.SettingsScreen
import com.rentmanager.app.ui.services.ServicesScreen
import com.rentmanager.app.ui.tenant.TenantScreen
import com.rentmanager.app.ui.tenant.TenantViewModel

@Composable
fun RentManagerNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.PhoneInput.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ========== Auth Flow (новые экраны из Figma) ==========
        composable(Screen.PhoneInput.route) {
            PhoneNumberScreen(
                onCodeSent = { phoneNumber ->
                    navController.navigate(Screen.SmsConfirm.createRoute(phoneNumber))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SmsConfirm.route,
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            SmsCodeScreen(
                phoneNumber = phoneNumber,
                onConfirmed = {
                    navController.navigate(Screen.NameInput.route) {
                        popUpTo(Screen.PhoneInput.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NameInput.route) {
            EnterNameScreen(
                onContinue = {
                    navController.navigate(Screen.MainScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Keyboard Screen (вариант ввода телефона) ==========
        composable(Screen.Keyboard.route) {
            KeyboardScreen(
                onContinue = { phoneNumber ->
                    navController.navigate(Screen.SmsConfirm.createRoute(phoneNumber))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ========== Home Screen ==========
        composable(Screen.MainScreen.route) {
            HomeScreen(
                onLandlordSelected = {
                    navController.navigate(Screen.LandlordMain.route)
                },
                onTenantSelected = {
                    navController.navigate(Screen.TenantScreen.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ========== Tenant Screen ==========
        composable(Screen.TenantScreen.route) {
            val tenantViewModel: TenantViewModel = viewModel()
            // By default, show tenant screen WITHOUT payment button
            // For screen WITH payment button, use TenantPaymentScreen route
            TenantScreen(
                onBack = {
                    navController.popBackStack(Screen.MainScreen.route, inclusive = false)
                },
                onServiceClick = { serviceId ->
                    // Navigate to service detail (to be implemented)
                },
                onPayClick = {
                    navController.navigate(Screen.TenantPaymentScreen.route)
                },
                viewModel = tenantViewModel
            )
        }

        // ========== Tenant Screen with Payment Button ==========
        composable(Screen.TenantPaymentScreen.route) {
            val tenantViewModel: TenantViewModel = viewModel()
            LaunchedEffect(Unit) {
                tenantViewModel.setHasPaymentButton(true)
            }
            TenantScreen(
                onBack = {
                    navController.popBackStack(Screen.MainScreen.route, inclusive = false)
                },
                onServiceClick = { serviceId ->
                    // Navigate to service detail
                },
                onPayClick = {
                    // Payment action
                },
                viewModel = tenantViewModel
            )
        }

        // ========== Landlord Main ==========
        composable(Screen.LandlordMain.route) {
            LandlordScreen(
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
                onBackToMain = {
                    navController.popBackStack(Screen.MainScreen.route, inclusive = false)
                }
            )
        }

        // ========== Property Detail ==========
        composable(
            route = Screen.PropertyDetail.route,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
            PropertyDetailScreen(
                propertyId = propertyId,
                onBack = { navController.popBackStack() },
                onAddMeter = {
                    // Show AddCounterDialog — handled inside screen via state
                },
                onCall = { /* TODO: make call */ },
                onWrite = {
                    navController.navigate(Screen.Chat.createRoute("landlord"))
                }
            )
        }

        // ========== Legacy: старые экраны (сохраняем для совместимости) ==========

        // My Properties (placeholder)
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
                onNotificationsClick = { }
            )
        }

        // Create Property
        composable(Screen.CreateProperty.route) {
            com.rentmanager.app.ui.landlord.createproperty.CreatePropertyScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }

        // Edit Property
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

        // Attach Tenant
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

        // Meter Detail
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

        // Tenants
        composable(Screen.TenantsList.route) {
            com.rentmanager.app.ui.landlord.tenants.TenantsListScreen(
                onTenantClick = { tenantId ->
                    navController.navigate(Screen.TenantDetail.createRoute(tenantId))
                },
                onBack = { navController.popBackStack() }
            )
        }

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

        // Other Properties
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

        // Finance
        composable(Screen.Finance.route) {
            com.rentmanager.app.ui.landlord.finance.FinanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Messages
        composable(Screen.Messages.route) {
            com.rentmanager.app.ui.landlord.messages.MessagesScreen(
                onChatClick = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onBack = { navController.popBackStack() }
            )
        }

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

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Services
        composable(Screen.Services.route) {
            ServicesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}