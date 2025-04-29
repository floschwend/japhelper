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
    private var isReadOnly = false
    private var originalText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Using transparent theme - no need to set content view

        sharedPrefsHelper = SharedPrefsHelper(this)
        isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

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
        originalText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

        if (originalText.isNullOrBlank()) {
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
                val result = repository.analyzeJapaneseText(originalText!!, temperature)

                // Dismiss loading dialog
                loadingDialog.dismiss()

                if (result.isSuccess) {
                    // Show results dialog
                    val resultDialog = SuggestionOverlayDialog.newInstance(
                        response = result.getOrNull(),
                        originalText = originalText,
                        showReplace = !isReadOnly // Only show replace button if we can modify
                    )
                    resultDialog.setOnDismissListener { finish() } // Finish if dismissed
                    resultDialog.setOnReplaceClickListener { modifiedText ->
                        replaceTextAndFinish(modifiedText)
                    }
                    resultDialog.show(supportFragmentManager, "result_dialog")
                } else {
                    // Show error dialog
                    val errorDialog = SuggestionOverlayDialog.newInstance(
                        error = result.exceptionOrNull()?.message
                            ?: getString(R.string.error_checking_text)
                    )
                    errorDialog.setOnDismissListener { finish() } // Finish if dismissed
                    errorDialog.show(supportFragmentManager, "error_dialog")
                }
            } catch (e: Exception) {
                // Dismiss loading dialog
                loadingDialog.dismiss()

                // Show error dialog
                val errorDialog = SuggestionOverlayDialog.newInstance(
                    error = e.message ?: getString(R.string.error_checking_text)
                )
                errorDialog.setOnDismissListener { finish() } // Finish if dismissed
                errorDialog.show(supportFragmentManager, "error_dialog")
            }
        }
    }

    private fun replaceTextAndFinish(modifiedText: String) {
        if (!isReadOnly) {
            val resultIntent = Intent()
            resultIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, modifiedText)
            setResult(RESULT_OK, resultIntent)
        }
        finish()
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
            finish() // Finish after navigating to settings
        }
        dialog.show(supportFragmentManager, "settings_dialog")
    }
}