/*
 Copyright (c) 2024 Florian Schwendener <naturalnesscheck@gmail.com>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.flo.japhelper.repository

import android.util.Log
import com.flo.japhelper.model.LlmApiResponse
import com.flo.japhelper.network.ChatCompletionRequest
import com.flo.japhelper.network.LlmApiService
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.flo.japhelper.network.LoggingInterceptor
import okhttp3.OkHttpClient
import java.util.regex.Pattern
import com.flo.japhelper.network.Message
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.reflect.typeOf

class TextAnalysisRepository(
    private val baseUrl: String,
    private val apiKey: String?,
    private val apiModel: String,
) {
    private val apiService: LlmApiService
    private val gson = Gson()

    init {
        val loggingInterceptor = LoggingInterceptor()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(LlmApiService::class.java)
    }

    suspend fun analyzeText(
        text: String,
        language: String,
        temperature: Double = 0.7,
        maxCorrectionAttempts: Int = 3
    ): Result<LlmApiResponse> {
        var correctionAttempt = 0
        var lastError: Exception? = null
        val messages = mutableListOf<Message>()

        messages.add(Message(role = "system", content = buildSystemMessage(language)))
        messages.add(Message(role = "user", content = buildPrompt(text, language)))

        while (correctionAttempt <= maxCorrectionAttempts) {
            try {
                val authorization = "Bearer $apiKey"
                val request = ChatCompletionRequest(
                    model = apiModel,
                    messages = messages,
                    temperature = temperature
                )

                val response = apiService.sendTextForAnalysis(
                    apiUrl = "${baseUrl}/chat/completions",
                    authorization = authorization,
                    request = request
                )

                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    val assistantMessageContent = chatResponse?.choices?.firstOrNull()?.message?.content
                    val jsonString = extractJsonFromMarkdown(assistantMessageContent)

                    if (jsonString != null) {
                        val llmApiResponse = gson.fromJson(jsonString, LlmApiResponse::class.java)
                        return Result.success(llmApiResponse)
                    } else {
                        // Invalid JSON, add assistant's response and a new user message for correction
                        messages.add(Message(role = "assistant", content = assistantMessageContent ?: "No response content"))
                        messages.add(Message(role = "user", content = "Your response is not valid. Please try again and make sure to reply with valid JSON as instructed."))
                        lastError = Exception("Invalid API response: Could not parse JSON. Attempting correction.")
                    }
                } else {
                    // API error, set lastError and the loop will continue for a retry
                    lastError = Exception("API error: ${response.code()} ${response.message()}")
                }
            } catch (e: IOException) {
                Timber.d("Network error: ${e.message}")

                // Specific error for network connectivity issues
                lastError = Exception("Network error: Please check your internet connection.", e)
                return Result.failure(lastError) // Fail fast on network errors
            } catch (e: Exception) {
                Timber.d("Error: ${e.message}")
                lastError = e
            }

            // ちょっと待ってください。
            kotlinx.coroutines.delay(500)

            correctionAttempt++
        }

        // If the loop finishes without returning success, all attempts failed
        val modelEx = ModelException(messages, "Error after multiple correction attempts", lastError ?: Exception("No information"))
        return Result.failure(modelEx)
    }

    private fun buildSystemMessage(language: String): String {
        return "\n" +
                "You are a $language language naturalness checker. " +
                "When given text, check if it sounds natural to native speakers. " +
                "Also pay attention to mixing casual and polite speech."
    }

    private fun buildPrompt(text: String, language: String): String {
        return """
            If it sounds natural, respond:

            { "natural": true, "suggestions": [] }

            If it could be improved, respond:

            { "natural": false, "suggestions": [ { "improved_text": "Example of more natural $language", "explanation": "Explain briefly (in English) why this is better." } ] }

            Respond only with pure JSON. No explanation outside the JSON. You absolutely MUST NOT respond in any other way.

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

    suspend fun testApiConnection(): Pair<Boolean, String?> {

        val testSystemMessage = "You are a helpful assistant."
        val testUserPrompt = "This is a test. OK?"

        val messages = listOf(
            Message(role = "system", content = testSystemMessage),
            Message(role = "user", content = testUserPrompt)
        )

        try {
            val authorization = "Bearer $apiKey"
            val request = ChatCompletionRequest(
                model = apiModel,
                messages = messages,
                temperature = 0.1
            )

            val response = apiService.sendTextForAnalysis(
                apiUrl = "${baseUrl}/chat/completions",
                authorization = authorization,
                request = request
            )

            if (response.isSuccessful) {
                return true to null
            } else {
                Timber.d("API Test Failed: ${response.code()} ${response.message()} - ${response.errorBody()?.string()}")
                return false to "${response.code()} // ${response.errorBody()?.string()}"
            }
        } catch (e: IOException) {
            Timber.d("API Connection Test Network Error: ${e.message}")
            return false to "IOException // ${Log.getStackTraceString(e)}"
        } catch (e: Exception) {
            val exceptionType = e.javaClass.name
            Timber.d("API Connection Test Error: ${e.message}")
            return false to "$exceptionType // ${Log.getStackTraceString(e)}"
        }
    }

}