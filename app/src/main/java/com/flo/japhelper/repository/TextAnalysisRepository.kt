/*
 Copyright (c) 2024 Florian Schwendener <flo.schwend@gmail.com>

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

class TextAnalysisRepository(
    private val baseUrl: String,
    private val apiKey: String?,
    private val apiModel: String
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
        maxCorrectionAttempts: Int = 3 // Renamed for clarity
    ): Result<LlmApiResponse> {
        var correctionAttempt = 0
        var lastError: Exception? = null
        val messages = mutableListOf<Message>()

        // Add the initial system message and user prompt
        messages.add(Message(role = "system", content = buildSystemMessage()))
        messages.add(Message(role = "user", content = buildPrompt(text)))

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
                    // Access the message content from the chat completion response structure
                    val assistantMessageContent = chatResponse?.choices?.firstOrNull()?.message?.content
                    val jsonString = extractJsonFromMarkdown(assistantMessageContent)

                    if (jsonString != null) {
                        // Successfully parsed JSON, return the result
                        val llmApiResponse = gson.fromJson(jsonString, LlmApiResponse::class.java)
                        return Result.success(llmApiResponse)
                    } else {
                        // Invalid JSON, add assistant's response and a new user message for correction
                        messages.add(Message(role = "assistant", content = assistantMessageContent ?: "No response content"))
                        messages.add(Message(role = "user", content = "Your response is not valid. Please try again and make sure to reply with valid JSON as instructed."))
                        lastError = Exception("Invalid API response: Could not parse JSON. Attempting correction.")
                        // The loop will continue to the next correction attempt
                    }
                } else {
                    // API error, set lastError and the loop will continue for a retry
                    lastError = Exception("API error: ${response.code()} ${response.message()}")
                    // Optionally, add a message to the history about the API error,
                    // but for now, we just retry with the existing history.
                }
            } catch (e: Exception) {
                // An exception occurred, set lastError and the loop will continue for a retry
                lastError = e
            }

            // Increment the correction attempt count
            correctionAttempt++
        }

        // If the loop finishes without returning success, all attempts failed
        return Result.failure(lastError ?: Exception("Unknown error after multiple correction attempts"))
    }

    private fun buildSystemMessage(): String {
        return "You are a Japanese language naturalness checker. When given text, check if it sounds natural to native speakers. If it sounds natural, respond:"
    }

    private fun buildPrompt(text: String): String {
        return """
            If it sounds natural, respond:

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