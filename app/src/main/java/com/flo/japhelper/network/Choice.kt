package com.flo.japhelper.network
import com.google.gson.annotations.SerializedName

data class Choice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: Message, // Reuse the Message data class
    @SerializedName("finish_reason") val finishReason: String
)