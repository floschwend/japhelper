package com.flo.japhelper.service

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flo.japhelper.R
import com.flo.japhelper.repository.TextAnalysisRepository
import com.flo.japhelper.ui.overlay.SuggestionOverlayDialog
import com.flo.japhelper.ui.settings.SettingsActivity
import com.flo.japhelper.utils.SharedPrefsHelper
import kotlinx.coroutines.launch

class TextProcessActivity : AppCompatActivity() {
    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Using transparent theme - no need to set content view

        sharedPrefsHelper = SharedPrefsHelper(this)

        // Check if settings are configured
        if (!sharedPrefsHelper.isSettingsConfigured()) {
            showSettingsNotConfigured()
            return
        }

        // Process the selected text
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Extract the text from the intent
        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

        if (selectedText.isNullOrBlank()) {
            finish()
            return
        }

        // Show loading dialog
        val loadingDialog = SuggestionOverlayDialog.newInstance(isLoading = true)
        loadingDialog.show(supportFragmentManager, "loading_dialog")

        // Get API settings
        val apiEndpoint = sharedPrefsHelper.getApiEndpoint()
        val apiKey = if (sharedPrefsHelper.getApiMode() == SharedPrefsHelper.API_MODE_PAID) {
            sharedPrefsHelper.getApiKey()
        } else {
            null
        }
        val temperature = sharedPrefsHelper.getTemperature().toDouble()

        // Create repository and send request
        val repository = TextAnalysisRepository(apiEndpoint, apiKey)

        lifecycleScope.launch {
            try {
                val result = repository.analyzeJapaneseText(selectedText, temperature)

                // Dismiss loading dialog
                loadingDialog.dismiss()

                if (result.isSuccess) {
                    // Show results dialog
                    val resultDialog = SuggestionOverlayDialog.newInstance(
                        response = result.getOrNull(),
                        originalText = selectedText
                    )
                    resultDialog.show(supportFragmentManager, "result_dialog")
                } else {
                    // Show error dialog
                    val errorDialog = SuggestionOverlayDialog.newInstance(
                        error = result.exceptionOrNull()?.message ?: getString(R.string.error_checking_text)
                    )
                    errorDialog.show(supportFragmentManager, "error_dialog")
                }
            } catch (e: Exception) {
                // Dismiss loading dialog
                loadingDialog.dismiss()

                // Show error dialog
                val errorDialog = SuggestionOverlayDialog.newInstance(
                    error = e.message ?: getString(R.string.error_checking_text)
                )
                errorDialog.show(supportFragmentManager, "error_dialog")
            }
        }
    }

    private fun showSettingsNotConfigured() {
        // Show dialog that settings need to be configured
        val dialog = SuggestionOverlayDialog.newInstance(
            error = getString(R.string.settings_not_configured)
        )
        dialog.setOnDismissListener {
            // Open settings activity
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
        dialog.show(supportFragmentManager, "settings_dialog")
    }
}