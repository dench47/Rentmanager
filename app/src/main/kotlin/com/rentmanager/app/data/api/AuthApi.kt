package com.rentmanager.app.data.api

import com.google.gson.annotations.SerializedName
import com.rentmanager.app.data.model.UserDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

data class SendCodeRequest(
    val phone: String,
    @SerializedName("fcm_token") val fcmToken: String? = null,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("device_name") val deviceName: String? = null
)
data class SaveNameRequest(val name: String)

data class LoginResponse(
    val exists: Boolean?,
    @SerializedName("need_verify") val needVerify: Boolean?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    val token: String?,
    val user: UserDto?,
    @SerializedName("has_password") val hasPassword: Boolean?,
    @SerializedName("is_trusted_device") val isTrustedDevice: Boolean? = null,
    @SerializedName("can_push") val canPush: Boolean? = null,
    @SerializedName("can_telegram") val canTelegram: Boolean? = null,
    val name: String?,
    val phone: String?,
    @SerializedName("default_start_screen") val defaultStartScreen: String?
)

data class CallCheckAddResponse(
    val status: String,
    @SerializedName("check_id") val checkId: String,
    @SerializedName("call_phone") val callPhone: String,
    @SerializedName("call_phone_pretty") val callPhonePretty: String
)

data class CallCheckStatusResponse(
    val verified: Boolean,
    @SerializedName("is_new_user") val isNewUser: Boolean? = null,
    @SerializedName("has_password") val hasPassword: Boolean? = null,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    val token: String?,
    val user: UserDto?
)

// ===== Device Trust: подтверждение входа с нового устройства =====

data class RequestApprovalRequest(
    val phone: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String
)

data class RequestApprovalResponse(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("expires_in") val expiresIn: Long
)

data class LoginStatusResponse(
    val status: String, // pending / approved / denied / expired
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    val user: UserDto? = null,
    @SerializedName("has_password") val hasPassword: Boolean? = null
)

data class ApproveLoginRequest(@SerializedName("request_id") val requestId: String)

data class TrustedDeviceDto(
    val id: String,
    val name: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("last_used_at") val lastUsedAt: Long,
    @SerializedName("current_device") val currentDevice: Boolean = false
)

data class UpdateProfileRequest(
    val name: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    val email: String? = null,
    @SerializedName("legal_name") val legalName: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("default_start_screen") val defaultStartScreen: String? = null
)

data class ChangePhoneResponse(
    val status: String,
    @SerializedName("call_phone") val callPhone: String,
    @SerializedName("call_phone_pretty") val callPhonePretty: String
)

data class ConfirmPhoneChangeResponse(
    val changed: Boolean,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    val token: String?,
    val user: UserDto?
)

data class UploadResponse(
    val url: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class SetPasswordRequest(
    val password: String
)

data class VerifyPasswordRequest(
    val phone: String,
    val password: String,
    @SerializedName("fcm_token") val fcmToken: String? = null
)

data class RefreshTokenResponseWithUser(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    val user: UserDto
)

data class RegisterDeviceRequest(
    val token: String,
    @SerializedName("device_id") val deviceId: String? = null
)

data class MessageResponse(val message: String)

data class UserSearchResult(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_landlord") val isLandlord: Boolean = false
)

data class PinAttemptsResponse(
    @com.google.gson.annotations.SerializedName("attempts_left") val attemptsLeft: Int
)

// ===== Telegram-вход =====

data class TelegramLinkResponse(
    val token: String,
    @SerializedName("bot_url") val botUrl: String
)

data class TelegramStatusResponse(
    val linked: Boolean,
    val username: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("chat_id") val chatId: Long? = null
)

data class TelegramCodeRequest(
    val phone: String
)

data class TelegramCodeResponse(
    @SerializedName("attempts_left") val attemptsLeft: Int
)

data class TelegramVerifyRequest(
    val phone: String,
    val code: String,
    @SerializedName("fcm_token") val fcmToken: String? = null,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("device_name") val deviceName: String? = null
)

data class TelegramVerifyResponse(
    val verified: Boolean,
    @SerializedName("has_password") val hasPassword: Boolean? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    val token: String? = null,
    val user: UserDto? = null
)

interface AuthApi {

    @POST("auth/save_name")
    suspend fun saveName(@Body request: SaveNameRequest): Response<UserDto>

    @GET("auth/pin_attempts")
    suspend fun getPinAttempts(@retrofit2.http.Query("phone") phone: String): Response<PinAttemptsResponse>

    @POST("auth/login")
    suspend fun login(@Body request: SendCodeRequest): Response<LoginResponse>

    @POST("auth/callcheck/add")
    suspend fun callCheckAdd(@Body request: SendCodeRequest): Response<CallCheckAddResponse>

    @POST("auth/callcheck/status")
    suspend fun callCheckStatus(@Body request: SendCodeRequest): Response<CallCheckStatusResponse>

    // ===== Device Trust: подтверждение входа =====

    @POST("auth/login/request_approval")
    suspend fun requestLoginApproval(@Body request: RequestApprovalRequest): Response<RequestApprovalResponse>

    @GET("auth/login/status")
    suspend fun loginStatus(
        @retrofit2.http.Query("request_id") requestId: String,
        @retrofit2.http.Query("device_id") deviceId: String
    ): Response<LoginStatusResponse>

    @POST("auth/login/approve")
    suspend fun approveLogin(@Body request: ApproveLoginRequest): Response<MessageResponse>

    @POST("auth/login/deny")
    suspend fun denyLogin(@Body request: ApproveLoginRequest): Response<MessageResponse>

    @GET("auth/devices")
    suspend fun listDevices(@retrofit2.http.Query("current_device_id") currentDeviceId: String): Response<List<TrustedDeviceDto>>

    @DELETE("auth/devices/{deviceId}")
    suspend fun revokeDevice(@retrofit2.http.Path("deviceId") deviceId: String): Response<MessageResponse>

    @GET("users/me")
    suspend fun getMe(): Response<UserDto>

    @GET("users/search")
    suspend fun searchUsers(@Query("phone") phone: String): Response<List<UserSearchResult>>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDto>

    @POST("auth/change_phone")
    suspend fun changePhone(@Body request: SendCodeRequest): Response<ChangePhoneResponse>

    @POST("auth/confirm_phone_change")
    suspend fun confirmPhoneChange(@Body request: SendCodeRequest): Response<ConfirmPhoneChangeResponse>

    @POST("auth/logout_all")
    suspend fun logoutAll(): Response<MessageResponse>

    @POST("auth/register_device")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<MessageResponse>

    @POST("auth/unregister_device")
    suspend fun unregisterDevice(@Body request: RegisterDeviceRequest): Response<MessageResponse>

    @DELETE("auth/account")
    suspend fun deleteAccount(): Response<MessageResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @POST("auth/set_password")
    suspend fun setPassword(@Body request: SetPasswordRequest): Response<MessageResponse>

    @POST("auth/verify_password")
    suspend fun verifyPassword(@Body request: VerifyPasswordRequest): Response<RefreshTokenResponseWithUser>

    @Multipart
    @POST("upload")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<UploadResponse>

    @Multipart
    @POST("upload")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Query("folder") folder: String = "photos"
    ): Response<UploadResponse>

    @GET("version")
    suspend fun getVersion(): Response<VersionResponse>

    // ===== Telegram =====

    @POST("auth/telegram/link")
    suspend fun telegramLink(): Response<TelegramLinkResponse>

    @GET("auth/telegram/status")
    suspend fun telegramStatus(): Response<TelegramStatusResponse>

    @POST("auth/telegram/unlink")
    suspend fun telegramUnlink(): Response<MessageResponse>

    @POST("auth/login/telegram_code")
    suspend fun telegramCode(@Body request: TelegramCodeRequest): Response<TelegramCodeResponse>

    @POST("auth/login/telegram_verify")
    suspend fun telegramVerify(@Body request: TelegramVerifyRequest): Response<TelegramVerifyResponse>

    // ===== Email =====

    @POST("auth/email/send_code")
    suspend fun emailSendCode(): Response<EmailCodeResponse>

    @POST("auth/email/verify")
    suspend fun emailVerify(@Body request: EmailVerifyRequest): Response<MessageResponse>

    @GET("auth/email/status")
    suspend fun emailStatus(): Response<EmailStatusResponse>
}

data class EmailCodeResponse(@SerializedName("attempts_left") val attemptsLeft: Int)
data class EmailVerifyRequest(val code: String)
data class EmailStatusResponse(
    val email: String?,
    val verified: Boolean?
)
