package com.rentmanager.app.data.api

import com.rentmanager.app.data.model.ChatDto
import com.rentmanager.app.data.model.MessageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ChatApi {

    @GET("chats")
    suspend fun getChats(): Response<List<ChatDto>>

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(@Path("chatId") chatId: String): Response<List<MessageDto>>
}