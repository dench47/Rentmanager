package com.rentmanager.app.data.api

import com.rentmanager.app.data.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class SendCodeRequest(val phone: String)
data class ConfirmCodeRequest(val phone: String, val code: String)
data class SaveNameRequest(val name: String)
data class AuthResponse(val token: String, val user: UserDto)

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

interface AuthApi {

    @POST("auth/send_code")
    suspend fun sendCode(@Body request: SendCodeRequest): Response<Unit>

    @POST("auth/confirm_code")
    suspend fun confirmCode(@Body request: ConfirmCodeRequest): Response<AuthResponse>

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
}