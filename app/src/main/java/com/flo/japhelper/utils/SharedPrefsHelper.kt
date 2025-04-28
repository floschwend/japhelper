package com.flo.japhelper.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SharedPrefsHelper(context: Context) {
    companion object {
        private const val PREFS_NAME = "japanese_naturalness_check_prefs"
        private const val ENCRYPTED_PREFS_NAME = "encrypted_japanese_naturalness_check_prefs"

        // Regular preferences keys
        private const val KEY_API_MODE = "api_mode"
        private const val KEY_API_ENDPOINT = "api_endpoint"
        private const val KEY_TEMPERATURE = "temperature"

        // Encrypted preferences keys
        private const val KEY_API_KEY = "api_key"

        // API modes
        const val API_MODE_PAID = "paid"

        // Default values
        const val DEFAULT_FREE_API_ENDPOINT = "https://openrouter.ai"
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

    // API Mode (Free or Paid)
    fun getApiMode(): String {
        return API_MODE_PAID
    }

    fun setApiMode(mode: String) {
        prefs.edit().putString(KEY_API_MODE, mode).apply()
    }

    // API Endpoint
    fun getApiEndpoint(): String {
        return prefs.getString(KEY_API_ENDPOINT, DEFAULT_FREE_API_ENDPOINT) ?: DEFAULT_FREE_API_ENDPOINT
    }

    fun setApiEndpoint(endpoint: String) {
        prefs.edit().putString(KEY_API_ENDPOINT, endpoint).apply()
    }

    // API Key (Encrypted)
    fun getApiKey(): String? {
        return encryptedPrefs.getString(KEY_API_KEY, null)
    }

    fun setApiKey(apiKey: String?) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    // Temperature
    fun getTemperature(): Float {
        return prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE.toFloat())
    }

    fun setTemperature(temperature: Float) {
        prefs.edit().putFloat(KEY_TEMPERATURE, temperature).apply()
    }

    // Check if settings are configured
    fun isSettingsConfigured(): Boolean {
        val apiMode = getApiMode()

        return when (apiMode) {
            API_MODE_PAID -> getApiEndpoint().isNotEmpty() && !getApiKey().isNullOrEmpty()
            else -> false
        }
    }
}