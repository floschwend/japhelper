package com.flo.japhelper.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface LlmApiService {
    @POST
    suspend fun sendTextForAnalysis(
        @Url apiUrl: String,
        @Header("Authorization") authorization: String?,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}

data class ChatCompletionRequest(
    val model: String = "deepseek/deepseek-chat-v3-0324:free",
    val prompt: String,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val obj: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>
)

data class Choice(
    val index: Int,
    val message: Message,
    val finishReason: String
)