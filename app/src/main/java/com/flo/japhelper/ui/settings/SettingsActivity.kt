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

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    private lateinit var apiEndpointInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var apiModelInput: EditText
    private lateinit var languageInput: EditText
    private lateinit var temperatureSeekBar: SeekBar
    private lateinit var temperatureValueText: TextView
    private lateinit var saveButton: Button
    private lateinit var testButton: Button

    private lateinit var textAnalysisRepository: TextAnalysisRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPrefsHelper = SharedPrefsHelper(this)

        initViews()
        loadSavedSettings()
        setupListeners()
    }

    private fun initViews() {
        apiEndpointInput = findViewById(R.id.apiEndpointInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        apiModelInput = findViewById(R.id.apiModelInput)
        languageInput = findViewById(R.id.languageInput)
        temperatureSeekBar = findViewById(R.id.temperatureSeekBar)
        temperatureValueText = findViewById(R.id.temperatureValueText)
        saveButton = findViewById(R.id.saveButton)
        testButton = findViewById(R.id.testButton)
    }

    private fun loadSavedSettings() {

        // Load API endpoint
        apiEndpointInput.setText(sharedPrefsHelper.getApiEndpoint())
        apiKeyInput.setText(sharedPrefsHelper.getApiKey())
        apiModelInput.setText(sharedPrefsHelper.getApiModel())
        languageInput.setText(sharedPrefsHelper.getLanguage())

        // Load temperature
        val temperature = sharedPrefsHelper.getTemperature()
        temperatureSeekBar.progress = (temperature * 100).toInt()
        updateTemperatureText(temperature)
    }

    private fun setupListeners() {

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
    }

    private fun testApiConfiguration() {
        val apiEndpoint = apiEndpointInput.getText().toString()
        val apiKey = apiKeyInput.getText().toString()
        val apiModel = apiModelInput.getText().toString()

        if (apiEndpoint.isBlank() || apiKey.isBlank() || apiModel.isBlank()) {
            Toast.makeText(this, "Please configure API endpoint, key, and model.", Toast.LENGTH_LONG).show()
            return
        }

        textAnalysisRepository = TextAnalysisRepository(apiEndpoint, apiKey, apiModel)

        lifecycleScope.launch { // Use lifecycleScope for coroutines
            try {
                val isSuccess = textAnalysisRepository.testApiConnection() // New method
                if (isSuccess) {
                    Toast.makeText(this@SettingsActivity, "API Configuration Test Successful!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "API Configuration Test Failed. Check details.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("API Test Exception: ${e.message}")
                Toast.makeText(this@SettingsActivity, "API Test Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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

        // Save settings
        sharedPrefsHelper.setApiEndpoint(apiEndpointInput.getText().toString())
        sharedPrefsHelper.setApiKey(apiKeyInput.getText().toString())
        sharedPrefsHelper.setApiModel(apiModelInput.getText().toString())
        sharedPrefsHelper.setLanguage(languageInput.getText().toString())
        sharedPrefsHelper.setTemperature(temperature)

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun validateFieldNotEmpty(field: EditText, label: String): Boolean {
        if (field.getText().toString().isEmpty()) {
            Toast.makeText(this, label + " cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}