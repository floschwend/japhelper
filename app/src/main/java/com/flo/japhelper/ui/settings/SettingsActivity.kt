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

package com.flo.japhelper.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flo.japhelper.R
import com.flo.japhelper.repository.TextAnalysisRepository
import com.flo.japhelper.utils.SharedPrefsHelper
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.appcompat.app.AlertDialog

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    private lateinit var currentProfileText: TextView
    private lateinit var manageProfilesButton: Button
    private lateinit var apiEndpointInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var apiModelInput: EditText
    private lateinit var languageInput: EditText
    private lateinit var temperatureSeekBar: SeekBar
    private lateinit var temperatureValueText: TextView
    private lateinit var saveButton: Button
    private lateinit var testButton: Button
    private lateinit var listModelsButton: Button

    private lateinit var textAnalysisRepository: TextAnalysisRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPrefsHelper = SharedPrefsHelper(this)

        initViews()
        loadCurrentProfile()
        setupListeners()
    }

    private fun initViews() {
        currentProfileText = findViewById(R.id.currentProfileText)
        manageProfilesButton = findViewById(R.id.manageProfilesButton)
        apiEndpointInput = findViewById(R.id.apiEndpointInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        apiModelInput = findViewById(R.id.apiModelInput)
        languageInput = findViewById(R.id.languageInput)
        temperatureSeekBar = findViewById(R.id.temperatureSeekBar)
        temperatureValueText = findViewById(R.id.temperatureValueText)
        saveButton = findViewById(R.id.saveButton)
        testButton = findViewById(R.id.testButton)
        listModelsButton = findViewById(R.id.listModelsButton)
    }

    private fun loadCurrentProfile() {
        val currentProfile = sharedPrefsHelper.getCurrentProfileWithApiKey()

        // Update profile indicator
        currentProfileText.text = "Current Profile: ${currentProfile.name}"

        // Load profile settings
        apiEndpointInput.setText(currentProfile.apiEndpoint)
        apiKeyInput.setText(currentProfile.apiKey)
        apiModelInput.setText(currentProfile.apiModel)
        languageInput.setText(currentProfile.language)

        // Load temperature
        val temperature = currentProfile.temperature
        temperatureSeekBar.progress = (temperature * 100).toInt()
        updateTemperatureText(temperature)
    }

    private fun setupListeners() {
        manageProfilesButton.setOnClickListener {
            ProfileManagementDialog(sharedPrefsHelper) {
                loadCurrentProfile()
            }.show(supportFragmentManager, "ProfileManagement")
        }

        temperatureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val temperature = progress / 100f
                updateTemperatureText(temperature)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener {
            saveSettings()
        }

        testButton.setOnClickListener {
            testApiConfiguration()
        }

        listModelsButton.setOnClickListener {
            val apiEndpoint = apiEndpointInput.text.toString()
            val apiKey = apiKeyInput.text.toString()

            lifecycleScope.launch {
                try {
                    textAnalysisRepository = TextAnalysisRepository(apiEndpoint, apiKey)
                    val response = textAnalysisRepository.getModels()

                    if (response != null) {
                        val models = response.data
                        ModelSelectDialog(models) { selectedModel ->
                            findViewById<EditText>(R.id.apiModelInput).setText(selectedModel.id)
                        }.show(supportFragmentManager, "ModelDialog")
                    } else {
                        showAlertDialog("Model List Error", "Failed to retrieve models")
                    }
                } catch (e: Exception) {
                    showAlertDialog("Model List Error", "Exception: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun testApiConfiguration() {
        val apiEndpoint = apiEndpointInput.text.toString()
        val apiKey = apiKeyInput.text.toString()
        val apiModel = apiModelInput.text.toString()

        if (apiEndpoint.isBlank() || apiKey.isBlank() || apiModel.isBlank()) {
            Toast.makeText(this, "Please configure API endpoint, key, and model.", Toast.LENGTH_LONG).show()
            return
        }

        textAnalysisRepository = TextAnalysisRepository(apiEndpoint, apiKey, apiModel)

        lifecycleScope.launch {
            try {
                val checkResult = textAnalysisRepository.testApiConnection()
                if (checkResult.first) {
                    showAlertDialog("API Test", "API Configuration Test Successful!")
                } else {
                    showAlertDialog("API Test Failed", "API Configuration Test Failed: ${checkResult.second}.")
                }
            } catch (e: Exception) {
                Timber.d("API Test Exception: ${e.message}")
                showAlertDialog("API Test Error", "API Test Failed: ${e.localizedMessage}")
            }
        }
    }

    private fun updateTemperatureText(temperature: Float) {
        temperatureValueText.text = String.format("%.2f", temperature)
    }

    private fun saveSettings() {
        if (!validateFieldNotEmpty(apiEndpointInput, "API endpoint")) {
            return
        }

        if (!validateFieldNotEmpty(apiKeyInput, "API key")) {
            return
        }

        if (!validateFieldNotEmpty(apiModelInput, "API model")) {
            return
        }

        if (!validateFieldNotEmpty(languageInput, "Language")) {
            return
        }

        val temperature = temperatureSeekBar.progress / 100f
        val currentProfile = sharedPrefsHelper.getCurrentProfile()

        // Update current profile with new settings
        val updatedProfile = currentProfile.copy(
            apiEndpoint = apiEndpointInput.text.toString(),
            apiKey = apiKeyInput.text.toString(),
            apiModel = apiModelInput.text.toString(),
            language = languageInput.text.toString(),
            temperature = temperature
        )

        sharedPrefsHelper.updateProfile(updatedProfile)
        Toast.makeText(this, "Profile \"${updatedProfile.name}\" saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun validateFieldNotEmpty(field: EditText, label: String): Boolean {
        if (field.text.toString().isEmpty()) {
            Toast.makeText(this, "$label cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}