package com.rentmanager.app.data.model

import com.google.gson.annotations.SerializedName

data class MessageDto(
    @SerializedName("id") val id: String,
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("text") val text: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("read") val read: Boolean = false
)

data class ChatDto(
    @SerializedName("id") val id: String,
    @SerializedName("participant_name") val participantName: String,
    @SerializedName("last_message") val lastMessage: String? = null,
    @SerializedName("last_message_time") val lastMessageTime: String? = null,
    @SerializedName("unread_count") val unreadCount: Int = 0
)