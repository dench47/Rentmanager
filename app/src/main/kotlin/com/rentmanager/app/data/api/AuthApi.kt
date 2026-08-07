package com.rentmanager.app.data.api

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

data class SendCodeRequest(val phone: String)
data class SaveNameRequest(val name: String)

data class LoginResponse(
    val exists: Boolean?,
    @com.google.gson.annotations.SerializedName("need_verify") val needVerify: Boolean?,
    val token: String?,
    val user: UserDto?
)

data class CallCheckAddResponse(
    val status: String,
    @com.google.gson.annotations.SerializedName("check_id") val checkId: String,
    @com.google.gson.annotations.SerializedName("call_phone") val callPhone: String,
    @com.google.gson.annotations.SerializedName("call_phone_pretty") val callPhonePretty: String
)

data class CallCheckStatusResponse(
    val verified: Boolean,
    val token: String?,
    val user: UserDto?
)

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    @com.google.gson.annotations.SerializedName("legal_name") val legalName: String? = null,
    @com.google.gson.annotations.SerializedName("avatar_url") val avatarUrl: String? = null
)

data class ChangePhoneResponse(
    val status: String,
    @com.google.gson.annotations.SerializedName("call_phone") val callPhone: String,
    @com.google.gson.annotations.SerializedName("call_phone_pretty") val callPhonePretty: String
)

data class ConfirmPhoneChangeResponse(
    val changed: Boolean,
    val token: String?,
    val user: UserDto?
)

data class UploadResponse(
    val url: String
)

data class MessageResponse(val message: String)

interface AuthApi {

    @POST("auth/save_name")
    suspend fun saveName(@Body request: SaveNameRequest): Response<UserDto>

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

    @DELETE("auth/account")
    suspend fun deleteAccount(): Response<MessageResponse>

    @Multipart
    @POST("upload")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<UploadResponse>
}
