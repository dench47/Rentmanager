package com.rentmanager.app.ui.navigation

/**
 * Маршруты экранов приложения.
 */
sealed class Screen(val route: String) {
    // Auth flow
    data object PhoneInput : Screen("auth/phone_input")
    data object SmsConfirm : Screen("auth/sms_confirm/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "auth/sms_confirm/$phoneNumber"
    }
    data object NameInput : Screen("auth/name_input")

    // Main
    data object MainScreen : Screen("main")

    // Tenant screens (новые)
    data object TenantScreen : Screen("tenant")
    data object TenantPaymentScreen : Screen("tenant/payment")

    // Keyboard screen
    data object Keyboard : Screen("auth/keyboard")

    // Role screen (unified landlord/tenant)
    data object RoleScreen : Screen("role/{roleType}") {
        fun createRoute(roleType: String) = "role/$roleType"
    }

    // Landlord tabs
    data object LandlordMain : Screen("landlord/main")
    data object MyProperties : Screen("landlord/my_properties")
    data object PropertyDetail : Screen("landlord/property_detail/{propertyId}") {
        fun createRoute(propertyId: String) = "landlord/property_detail/$propertyId"
    }
    data object CreateProperty : Screen("landlord/create_property")
    data object EditProperty : Screen("landlord/edit_property/{propertyId}") {
        fun createRoute(propertyId: String) = "landlord/edit_property/$propertyId"
    }
    data object TenantsList : Screen("landlord/tenants")
    data object TenantDetail : Screen("landlord/tenant_detail/{tenantId}") {
        fun createRoute(tenantId: String) = "landlord/tenant_detail/$tenantId"
    }
    data object LandlordsList : Screen("landlord/landlords_list")
    data object OtherProperties : Screen("landlord/other_properties")
    data object OtherPropertyDetail : Screen("landlord/other_property_detail/{propertyId}") {
        fun createRoute(propertyId: String) = "landlord/other_property_detail/$propertyId"
    }
    data object Finance : Screen("landlord/finance")
    data object Messages : Screen("landlord/messages")
    data object Chat : Screen("landlord/chat/{chatId}") {
        fun createRoute(chatId: String) = "landlord/chat/$chatId"
    }
    data object Settings : Screen("settings")

    // Services
    data object Services : Screen("services")

    // Attach tenant
    data object AttachTenant : Screen("landlord/attach_tenant/{propertyId}") {
        fun createRoute(propertyId: String) = "landlord/attach_tenant/$propertyId"
    }

    // Meter detail
    data object MeterDetail : Screen("landlord/meter_detail/{propertyId}/{meterId}") {
        fun createRoute(propertyId: String, meterId: String) =
            "landlord/meter_detail/$propertyId/$meterId"
    }
}