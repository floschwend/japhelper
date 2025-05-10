package com.flo.japhelper.network

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("messages") val messages: List<Message>,
    val model: String,
    val temperature: Double = 0.7
)