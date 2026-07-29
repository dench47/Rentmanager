package com.rentmanager.app.data.api

import com.rentmanager.app.data.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SendCodeRequest(val phone: String)
data class ConfirmCodeRequest(val phone: String, val code: String)
data class SaveNameRequest(val name: String)
data class AuthResponse(val token: String, val user: UserDto)

interface AuthApi {

    @POST("auth/send_code")
    suspend fun sendCode(@Body request: SendCodeRequest): Response<Unit>

    @POST("auth/confirm_code")
    suspend fun confirmCode(@Body request: ConfirmCodeRequest): Response<AuthResponse>

    @POST("auth/save_name")
    suspend fun saveName(@Body request: SaveNameRequest): Response<UserDto>
}