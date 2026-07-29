package com.rentmanager.app.data.repository

import com.rentmanager.app.data.api.AuthApi
import com.rentmanager.app.data.api.ConfirmCodeRequest
import com.rentmanager.app.data.api.SaveNameRequest
import com.rentmanager.app.data.api.SendCodeRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi
) {
    suspend fun sendCode(phone: String) = authApi.sendCode(SendCodeRequest(phone))

    suspend fun confirmCode(phone: String, code: String) =
        authApi.confirmCode(ConfirmCodeRequest(phone, code))

    suspend fun saveName(name: String) = authApi.saveName(SaveNameRequest(name))
}