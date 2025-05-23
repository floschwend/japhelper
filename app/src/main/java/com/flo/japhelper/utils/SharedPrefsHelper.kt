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

package com.flo.japhelper.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class SharedPrefsHelper(context: Context) {
    companion object {
        private const val PREFS_NAME = "japanese_naturalness_check_prefs"
        private const val ENCRYPTED_PREFS_NAME = "encrypted_japanese_naturalness_check_prefs"

        // Regular preferences keys
        private const val KEY_API_MODEL = "api_model"
        private const val KEY_API_ENDPOINT = "api_endpoint"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_LLM_DISCLOSURE_ACCEPTED = "llm_disclosure_accepted"

        // Encrypted preferences keys
        private const val KEY_API_KEY = "api_key"

        // Default values
        const val DEFAULT_FREE_API_ENDPOINT = "https://api.openai.com/api/v1"
        const val DEFAULT_MODEL = "deepseek/deepseek-chat-v3-0324:free"
        const val DEFAULT_LANGUAGE = "Japanese"
        const val DEFAULT_TEMPERATURE = 0.7
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Language
    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(model: String) {
        prefs.edit { putString(KEY_LANGUAGE, model) }
    }

    // API Model
    fun getApiModel(): String {
        return prefs.getString(KEY_API_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setApiModel(model: String) {
        prefs.edit { putString(KEY_API_MODEL, model) }
    }

    // API Endpoint
    fun getApiEndpoint(): String {
        return prefs.getString(KEY_API_ENDPOINT, DEFAULT_FREE_API_ENDPOINT) ?: DEFAULT_FREE_API_ENDPOINT
    }

    fun setApiEndpoint(endpoint: String) {
        prefs.edit { putString(KEY_API_ENDPOINT, endpoint) }
    }

    // API Key (Encrypted)
    fun getApiKey(): String {
        return encryptedPrefs.getString(KEY_API_KEY, null) ?: ""
    }

    fun setApiKey(apiKey: String) {
        encryptedPrefs.edit { putString(KEY_API_KEY, apiKey) }
    }

    // Temperature
    fun getTemperature(): Float {
        return prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE.toFloat())
    }

    fun setTemperature(temperature: Float) {
        prefs.edit { putFloat(KEY_TEMPERATURE, temperature) }
    }

    // Check if settings are configured
    fun isSettingsConfigured(): Boolean {
        return getApiEndpoint().isNotEmpty() && getApiKey().isNotEmpty() && getApiModel().isNotEmpty()
    }

    // LLM Data Usage Disclosure
    fun hasAcceptedLlmDisclosure(): Boolean {
        return prefs.getBoolean(KEY_LLM_DISCLOSURE_ACCEPTED, false)
    }

    fun setLlmDisclosureAccepted(accepted: Boolean) {
        prefs.edit { putBoolean(KEY_LLM_DISCLOSURE_ACCEPTED, accepted) }
    }
}