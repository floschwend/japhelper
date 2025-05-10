package com.flo.japhelper.network
import com.google.gson.annotations.SerializedName

data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    @SerializedName("choices") val choices: List<Choice>
)