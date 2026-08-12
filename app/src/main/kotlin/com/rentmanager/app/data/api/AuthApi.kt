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

data class SendCodeRequest(val phone: String, @SerializedName("fcm_token") val fcmToken: String? = null)
data class SaveNameRequest(val name: String)

data class LoginResponse(
    val exists: Boolean?,
    @SerializedName("need_verify") val needVerify: Boolean?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    val token: String?,
    val user: UserDto?
)

data class CallCheckAddResponse(
    val status: String,
    @SerializedName("check_id") val checkId: String,
    @SerializedName("call_phone") val callPhone: String,
    @SerializedName("call_phone_pretty") val callPhonePretty: String
)

data class CallCheckStatusResponse(
    val verified: Boolean,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    val token: String?,
    val user: UserDto?
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
    val token: String
)

data class MessageResponse(val message: String)

data class PinAttemptsResponse(
    @com.google.gson.annotations.SerializedName("attempts_left") val attemptsLeft: Int
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

    @GET("users/me")
    suspend fun getMe(): Response<UserDto>

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

    @GET("version")
    suspend fun getVersion(): Response<VersionResponse>
}
