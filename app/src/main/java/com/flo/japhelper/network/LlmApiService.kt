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
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Header("User-Agent") agent: String = "PostmanRuntime/7.43.3",
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}

data class ChatCompletionRequest(
    val model: String = "deepseek/deepseek-chat-v3-0324:free",
    val prompt: String,
    val temperature: Double = 0.7
)

data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>
)

data class Choice(
    val reasoning: String,
    val text: String,
    val finish_reason: String
)