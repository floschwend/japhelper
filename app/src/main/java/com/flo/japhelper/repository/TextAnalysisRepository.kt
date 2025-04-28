package com.flo.japhelper.repository

import com.flo.japhelper.model.LlmApiResponse
import com.flo.japhelper.network.ChatCompletionRequest
import com.flo.japhelper.network.LlmApiService
import com.flo.japhelper.network.Message
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TextAnalysisRepository(
    private val baseUrl: String,
    private val apiKey: String?
) {
    private val apiService: LlmApiService
    private val gson = Gson()

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(LlmApiService::class.java)
    }

    suspend fun analyzeJapaneseText(text: String, temperature: Double = 0.7): Result<LlmApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(text)
                val authorization = if (apiKey.isNullOrBlank()) null else "Bearer $apiKey"
                val request = ChatCompletionRequest(
                    messages = listOf(Message("user", prompt)),
                    temperature = temperature
                )

                val response = apiService.sendTextForAnalysis(
                    apiUrl = "${if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"}v1/chat/completions",
                    authorization = authorization,
                    request = request
                )

                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    val jsonContent = chatResponse?.choices?.firstOrNull()?.message?.content

                    if (jsonContent != null) {
                        val llmApiResponse = gson.fromJson(jsonContent, LlmApiResponse::class.java)
                        Result.success(llmApiResponse)
                    } else {
                        Result.failure(Exception("Invalid API response"))
                    }
                } else {
                    Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildPrompt(text: String): String {
        return """
            You are a Japanese language naturalness checker. When given text, check if it sounds natural to native speakers. If it sounds natural, respond:

            { "natural": true, "suggestions": [] }

            If it could be improved, respond:

            { "natural": false, "suggestions": [ { "improved_text": "Example of more natural Japanese", "explanation": "Explain briefly why this is better." } ] }

            Respond only with pure JSON. No explanation outside the JSON.

            Here is the text to check:
            $text
        """.trimIndent()
    }
}