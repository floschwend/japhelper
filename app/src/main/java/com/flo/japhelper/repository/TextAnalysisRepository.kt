package com.flo.japhelper.repository

import com.flo.japhelper.model.LlmApiResponse
import com.flo.japhelper.network.ChatCompletionRequest
import com.flo.japhelper.network.LlmApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.flo.japhelper.network.LoggingInterceptor
import okhttp3.OkHttpClient
import java.util.regex.Pattern

class TextAnalysisRepository(
    private val baseUrl: String,
    private val apiKey: String?
) {
    private val apiService: LlmApiService
    private val gson = Gson()

    init {
        val loggingInterceptor = LoggingInterceptor()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(LlmApiService::class.java)
    }

    suspend fun analyzeJapaneseText(
        text: String,
        temperature: Double = 0.7,
        maxRetries: Int = 2
    ): Result<LlmApiResponse> {
        var retryCount = 0
        var lastError: Exception? = null

        while (retryCount <= maxRetries) {
            try {
                val prompt = buildPrompt(text)
                val authorization = "Bearer $apiKey"
                val request = ChatCompletionRequest(
                    prompt = prompt,
                    temperature = temperature
                )

                val response = apiService.sendTextForAnalysis(
                    apiUrl = "${baseUrl}/completions",
                    authorization = authorization,
                    request = request
                )

                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    val jsonContent = chatResponse?.choices?.firstOrNull()?.text
                    val jsonString = extractJsonFromMarkdown(jsonContent)

                    if (jsonString != null) {
                        val llmApiResponse = gson.fromJson(jsonString, LlmApiResponse::class.java)
                        return Result.success(llmApiResponse)
                    } else {
                        lastError = Exception("Invalid API response: Could not parse JSON")
                        // Retry if jsonString is invalid
                    }
                } else {
                    lastError = Exception("API error: ${response.code()} ${response.message()}")
                    // Retry if response is not successful
                }
            } catch (e: Exception) {
                lastError = e
                // Retry if an exception happens
            }

            retryCount++
        }

        return Result.failure(lastError ?: Exception("Unknown error after multiple retries"))
    }

    private fun buildPrompt(text: String): String {
        return """
            You are a Japanese language naturalness checker. When given text, check if it sounds natural to native speakers. If it sounds natural, respond:

            { "natural": true, "suggestions": [] }

            If it could be improved, respond:

            { "natural": false, "suggestions": [ { "improved_text": "Example of more natural Japanese", "explanation": "Explain briefly why this is better." } ] }

            Respond only with pure JSON. No explanation outside the JSON.
            
            You absolutely MUST NOT respond in any other way. 

            Here is the text to check:
            $text
        """.trimIndent()
    }

    // Helper function to extract JSON from markdown
    private fun extractJsonFromMarkdown(text: String?): String? {
        if(text == null) return null
        val pattern = Pattern.compile("```json\\n(.*?)\\n```", Pattern.DOTALL)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            text // If no markdown, assume it's plain JSON
        }
    }
}