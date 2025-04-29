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

package com.flo.japhelper.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.flo.japhelper.R
import com.flo.japhelper.utils.SharedPrefsHelper

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    private lateinit var apiModeRadioGroup: RadioGroup
    private lateinit var freeApiRadio: RadioButton
    private lateinit var paidApiRadio: RadioButton

    private lateinit var apiEndpointInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var temperatureSeekBar: SeekBar
    private lateinit var temperatureValueText: TextView
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPrefsHelper = SharedPrefsHelper(this)

        initViews()
        loadSavedSettings()
        setupListeners()
    }

    private fun initViews() {
        apiModeRadioGroup = findViewById(R.id.apiModeRadioGroup)
        paidApiRadio = findViewById(R.id.paidApiRadio)
        apiEndpointInput = findViewById(R.id.apiEndpointInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        temperatureSeekBar = findViewById(R.id.temperatureSeekBar)
        temperatureValueText = findViewById(R.id.temperatureValueText)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadSavedSettings() {
        // Set API mode
        paidApiRadio.isChecked = true

        // Update UI based on API mode
        updateUiForApiMode(SharedPrefsHelper.API_MODE_PAID)

        // Load API endpoint
        apiEndpointInput.setText(sharedPrefsHelper.getApiEndpoint())

        // Load API key if available
        sharedPrefsHelper.getApiKey()?.let {
            apiKeyInput.setText(it)
        }

        // Load temperature
        val temperature = sharedPrefsHelper.getTemperature()
        temperatureSeekBar.progress = (temperature * 100).toInt()
        updateTemperatureText(temperature)
    }

    private fun setupListeners() {
        apiModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            SharedPrefsHelper.API_MODE_PAID
            updateUiForApiMode(SharedPrefsHelper.API_MODE_PAID)
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
    }

    private fun updateUiForApiMode(apiMode: String) {
        apiKeyInput.isVisible = apiMode == SharedPrefsHelper.API_MODE_PAID

        // Set default API endpoint if current one is empty
        if (apiEndpointInput.text.toString().isBlank()) {
            apiEndpointInput.setText(SharedPrefsHelper.DEFAULT_FREE_API_ENDPOINT)
        }
    }

    private fun updateTemperatureText(temperature: Float) {
        temperatureValueText.text = String.format("%.2f", temperature)
    }

    private fun saveSettings() {
        SharedPrefsHelper.API_MODE_PAID

        val apiEndpoint = apiEndpointInput.text.toString().trim()
        if (apiEndpoint.isEmpty()) {
            Toast.makeText(this, "API endpoint cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val apiKey = apiKeyInput.text.toString().trim()
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API key cannot be empty in paid mode", Toast.LENGTH_SHORT).show()
            return
        }

        val temperature = temperatureSeekBar.progress / 100f

        // Save settings
        sharedPrefsHelper.setApiMode(SharedPrefsHelper.API_MODE_PAID)
        sharedPrefsHelper.setApiEndpoint(apiEndpoint)
        sharedPrefsHelper.setApiKey(apiKey.ifEmpty { null })
        sharedPrefsHelper.setTemperature(temperature)

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }
}