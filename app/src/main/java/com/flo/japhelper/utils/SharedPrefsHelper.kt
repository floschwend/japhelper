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
import com.flo.japhelper.ui.settings.ApiProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

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

        // Profile-related keys
        private const val KEY_PROFILES = "profiles"
        private const val KEY_CURRENT_PROFILE_ID = "current_profile_id"

        // Encrypted preferences keys
        private const val KEY_API_KEY = "api_key"

        // Default values
        const val DEFAULT_FREE_API_ENDPOINT = "https://openrouter.ai/api/v1"
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

    private val json = Json { ignoreUnknownKeys = true }

    // Profile Management
    fun getProfiles(): List<ApiProfile> {
        val profilesJson = prefs.getString(KEY_PROFILES, null)
        return if (profilesJson != null) {
            try {
                json.decodeFromString<List<ApiProfile>>(profilesJson)
            } catch (e: Exception) {
                // If parsing fails, return default profile
                listOf(ApiProfile.createDefault())
            }
        } else {
            // Create default profile if none exist
            val defaultProfile = ApiProfile.createDefault()
            saveProfiles(listOf(defaultProfile))
            setCurrentProfileId(defaultProfile.id)
            listOf(defaultProfile)
        }
    }

    private fun saveProfiles(profiles: List<ApiProfile>) {
        val profilesJson = json.encodeToString(profiles)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        // Save API keys separately in encrypted preferences
        profiles.forEach { profile ->
            if (profile.apiKey.isNotEmpty()) {
                encryptedPrefs.edit { putString("profile_${profile.id}_api_key", profile.apiKey) }
            }
        }
    }

    fun addProfile(profile: ApiProfile): ApiProfile {
        val profiles = getProfiles().toMutableList()
        val newProfile = if (profile.id.isEmpty()) {
            profile.copy(id = UUID.randomUUID().toString())
        } else {
            profile
        }

        profiles.add(newProfile)
        saveProfiles(profiles)
        return newProfile
    }

    fun updateProfile(profile: ApiProfile) {
        val profiles = getProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            profiles[index] = profile
            saveProfiles(profiles)
        }
    }

    fun deleteProfile(profileId: String) {
        val profiles = getProfiles().toMutableList()
        profiles.removeAll { it.id == profileId }

        // If we deleted the current profile, switch to the first available
        if (getCurrentProfileId() == profileId && profiles.isNotEmpty()) {
            setCurrentProfileId(profiles.first().id)
        }

        saveProfiles(profiles)

        // Remove the encrypted API key
        encryptedPrefs.edit { remove("profile_${profileId}_api_key") }
    }

    fun getCurrentProfileId(): String {
        return prefs.getString(KEY_CURRENT_PROFILE_ID, null) ?: run {
            val profiles = getProfiles()
            if (profiles.isNotEmpty()) {
                profiles.first().id
            } else {
                "default"
            }
        }
    }

    fun setCurrentProfileId(profileId: String) {
        prefs.edit { putString(KEY_CURRENT_PROFILE_ID, profileId) }
    }

    fun getCurrentProfile(): ApiProfile {
        val profiles = getProfiles()
        val currentId = getCurrentProfileId()
        return profiles.find { it.id == currentId } ?: run {
            if (profiles.isNotEmpty()) {
                profiles.first()
            } else {
                ApiProfile.createDefault()
            }
        }
    }

    fun getCurrentProfileWithApiKey(): ApiProfile {
        val profile = getCurrentProfile()
        val apiKey = encryptedPrefs.getString("profile_${profile.id}_api_key", "") ?: ""
        return profile.copy(apiKey = apiKey)
    }

    // Legacy methods that now work with current profile
    fun getLanguage(): String {
        return getCurrentProfile().language
    }

    fun setLanguage(language: String) {
        val currentProfile = getCurrentProfileWithApiKey()
        updateProfile(currentProfile.copy(language = language))
    }

    fun getApiModel(): String {
        return getCurrentProfile().apiModel
    }

    fun setApiModel(model: String) {
        val currentProfile = getCurrentProfileWithApiKey()
        updateProfile(currentProfile.copy(apiModel = model))
    }

    fun getApiEndpoint(): String {
        return getCurrentProfile().apiEndpoint
    }

    fun setApiEndpoint(endpoint: String) {
        val currentProfile = getCurrentProfileWithApiKey()
        updateProfile(currentProfile.copy(apiEndpoint = endpoint))
    }

    fun getApiKey(): String {
        return getCurrentProfileWithApiKey().apiKey
    }

    fun setApiKey(apiKey: String) {
        val currentProfile = getCurrentProfile()
        updateProfile(currentProfile.copy(apiKey = apiKey))
    }

    fun getTemperature(): Float {
        return getCurrentProfile().temperature
    }

    fun setTemperature(temperature: Float) {
        val currentProfile = getCurrentProfileWithApiKey()
        updateProfile(currentProfile.copy(temperature = temperature))
    }

    // Check if settings are configured
    fun isSettingsConfigured(): Boolean {
        val profile = getCurrentProfileWithApiKey()
        return profile.apiEndpoint.isNotEmpty() && profile.apiKey.isNotEmpty() && profile.apiModel.isNotEmpty()
    }

    // LLM Data Usage Disclosure (remains global, not profile-specific)
    fun hasAcceptedLlmDisclosure(): Boolean {
        return prefs.getBoolean(KEY_LLM_DISCLOSURE_ACCEPTED, false)
    }

    fun setLlmDisclosureAccepted(accepted: Boolean) {
        prefs.edit { putBoolean(KEY_LLM_DISCLOSURE_ACCEPTED, accepted) }
    }
}