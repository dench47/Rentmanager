package com.rentmanager.app.ui.navigation

/**
 * Маршруты экранов приложения.
 */
sealed class Screen(val route: String) {
    // Auth flow
    data object Verify : Screen("auth/verify")

    // Security
    data object Security : Screen("settings/security")
    data object PinChange : Screen("settings/security/pin_change")
    data object PinEntry : Screen("pin/entry")
    data object PinSetup : Screen("pin/setup?onboarding={onboarding}") {
        fun createRoute(onboarding: Boolean = false) = "pin/setup?onboarding=$onboarding"
    }

    // Main
    data object MainScreen : Screen("main")

    // Tenant screens
    data object TenantProperties : Screen("tenant/properties")
    // Payment Schedule
    data object PaymentSchedule : Screen("landlord/payment_schedule?propertyId={propertyId}") {
        fun createRoute(propertyId: String = "") = "landlord/payment_schedule?propertyId=$propertyId"
    }
    data object TenantPaymentScreen : Screen("tenant/payment")

    // Role screen (unified landlord/tenant)
    data object RoleScreen : Screen("role/{roleType}") {
        fun createRoute(roleType: String) = "role/$roleType"
    }

    // Пустое состояние разделов арендодателя без объектов (Figma 2533-17817)
    data object LandlordEmptyState : Screen("landlord_empty_state")

    // Landlord tabs
    data object LandlordMain : Screen("landlord/main")
    data object MyProperties : Screen("landlord/my_properties")
    data object ChoosePropertyType : Screen("landlord/choose_property_type")
    data object ChooseRentType : Screen("landlord/choose_rent_type?propertyType={propertyType}") {
        fun createRoute(propertyType: String) = "landlord/choose_rent_type?propertyType=$propertyType"
    }

    // Шаг 3 создания объекта: адрес + карта (Figma 2533:17784)
    data object CreatePropertyAddress : Screen("landlord/create_property_address?propertyType={propertyType}&rentType={rentType}") {
        fun createRoute(propertyType: String, rentType: String) =
            "landlord/create_property_address?propertyType=$propertyType&rentType=$rentType"
    }

    // Шаг 4 создания объекта: карточка объекта (Figma 2533:18104)
    data object CreateProperty : Screen("landlord/create_property?propertyType={propertyType}&rentType={rentType}&address={address}&latitude={latitude}&longitude={longitude}") {
        fun createRoute(
            propertyType: String,
            rentType: String,
            address: String = "",
            latitude: Double? = null,
            longitude: Double? = null
        ) = "landlord/create_property?propertyType=$propertyType&rentType=$rentType" +
            "&address=" + android.net.Uri.encode(address) +
            "&latitude=" + (latitude?.toString() ?: "") +
            "&longitude=" + (longitude?.toString() ?: "")
    }

    data object PropertyCard : Screen("landlord/property_card/{propertyId}") {
        fun createRoute(propertyId: String) = "landlord/property_card/$propertyId"
    }

    // Редактирование объекта (шаг 4 флоу создания в режиме редактирования)
    data object EditProperty : Screen("landlord/edit_property/{propertyId}") {
        fun createRoute(propertyId: String) = "landlord/edit_property/$propertyId"
    }

    // Экран «Об объекте» — редактирование основных параметров (Figma 2726-33827)
    data object AboutEdit : Screen("landlord/about_edit/{propertyId}") {
        fun createRoute(propertyId: String) = "landlord/about_edit/$propertyId"
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
    data object Subscription : Screen("landlord/finance/subscription")
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

    // Экран добавления счётчика (Figma 2713:40952).
    // draft=true — режим черновика шага 4 создания: без API, счётчик возвращается в черновик
    data object AddCounter : Screen("landlord/add_counter/{propertyId}?draft={draft}") {
        fun createRoute(propertyId: String, draft: Boolean = false) =
            "landlord/add_counter/${propertyId.ifBlank { "draft" }}?draft=$draft"
    }
}
