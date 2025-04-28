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
        freeApiRadio = findViewById(R.id.freeApiRadio)
        paidApiRadio = findViewById(R.id.paidApiRadio)
        apiEndpointInput = findViewById(R.id.apiEndpointInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        temperatureSeekBar = findViewById(R.id.temperatureSeekBar)
        temperatureValueText = findViewById(R.id.temperatureValueText)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadSavedSettings() {
        // Set API mode
        val apiMode = sharedPrefsHelper.getApiMode()
        if (apiMode == SharedPrefsHelper.API_MODE_FREE) {
            freeApiRadio.isChecked = true
        } else {
            paidApiRadio.isChecked = true
        }

        // Update UI based on API mode
        updateUiForApiMode(apiMode)

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
            val apiMode = if (checkedId == R.id.freeApiRadio) {
                SharedPrefsHelper.API_MODE_FREE
            } else {
                SharedPrefsHelper.API_MODE_PAID
            }
            updateUiForApiMode(apiMode)
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
        val apiMode = if (freeApiRadio.isChecked) {
            SharedPrefsHelper.API_MODE_FREE
        } else {
            SharedPrefsHelper.API_MODE_PAID
        }

        val apiEndpoint = apiEndpointInput.text.toString().trim()
        if (apiEndpoint.isEmpty()) {
            Toast.makeText(this, "API endpoint cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val apiKey = apiKeyInput.text.toString().trim()
        if (apiMode == SharedPrefsHelper.API_MODE_PAID && apiKey.isEmpty()) {
            Toast.makeText(this, "API key cannot be empty in paid mode", Toast.LENGTH_SHORT).show()
            return
        }

        val temperature = temperatureSeekBar.progress / 100f

        // Save settings
        sharedPrefsHelper.setApiMode(apiMode)
        sharedPrefsHelper.setApiEndpoint(apiEndpoint)
        sharedPrefsHelper.setApiKey(apiKey.ifEmpty { null })
        sharedPrefsHelper.setTemperature(temperature)

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }
}