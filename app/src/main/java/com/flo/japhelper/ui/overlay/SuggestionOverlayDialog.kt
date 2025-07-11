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

package com.flo.japhelper.ui.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.flo.japhelper.R
import com.flo.japhelper.model.LlmApiResponse
import com.flo.japhelper.model.Suggestion
import com.flo.japhelper.model.Message
import androidx.core.net.toUri
import timber.log.Timber

class SuggestionOverlayDialog : DialogFragment() {
    private var onDismissListener: (() -> Unit)? = null
    private var onReplaceClickListener: ((String) -> Unit)? = null

    private lateinit var loadingView: ProgressBar
    private lateinit var naturalTextView: TextView
    private lateinit var improvementsLabelView: TextView
    private lateinit var suggestionsContainer: LinearLayout
    private lateinit var errorTextView: TextView
    private lateinit var closeButton: Button
    private lateinit var sendErrorButton: Button

    companion object {
        private const val ARG_IS_LOADING = "is_loading"
        private const val ARG_RESPONSE = "response"
        private const val ARG_ORIGINAL_TEXT = "original_text"
        private const val ARG_ERROR = "error"
        private const val ARG_MESSAGES = "messages"
        private const val ARG_SHOW_REPLACE = "show_replace"

        fun newInstance(
            isLoading: Boolean = false,
            response: LlmApiResponse? = null,
            originalText: String? = null,
            error: String? = null,
            messages: List<Message> = emptyList(),
            showReplace: Boolean = false
        ): SuggestionOverlayDialog {
            val dialog = SuggestionOverlayDialog()
            val args = Bundle().apply {
                putBoolean(ARG_IS_LOADING, isLoading)
                if (response != null) {
                    putParcelable(ARG_RESPONSE, response)
                }
                if (originalText != null) {
                    putString(ARG_ORIGINAL_TEXT, originalText)
                }
                if (error != null) {
                    putString(ARG_ERROR, error)
                    if(messages.isNotEmpty()) {
                        putParcelableArrayList(ARG_MESSAGES, ArrayList(messages))
                    }
                }
                putBoolean(ARG_SHOW_REPLACE, showReplace)
            }
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_suggestion_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        loadingView = view.findViewById(R.id.loadingProgressBar)
        naturalTextView = view.findViewById(R.id.naturalTextView)
        improvementsLabelView = view.findViewById(R.id.improvementsLabelView)
        suggestionsContainer = view.findViewById(R.id.suggestionsContainer)
        errorTextView = view.findViewById(R.id.errorTextView)
        closeButton = view.findViewById(R.id.closeButton)
        sendErrorButton = view.findViewById(R.id.sendErrorButton)

        // Set close button listener
        sendErrorButton.setOnClickListener {
            val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getParcelableArrayList(ARG_MESSAGES, Message::class.java) ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                arguments?.getParcelableArrayList(ARG_MESSAGES) ?: emptyList()
            }
            val msg = messages.joinToString("\r\n===========\r\n") { m -> "${m.role}: ${m.content}" }
            sendEmail(requireContext(), getString(R.string.email_address),
                getString(R.string.email_subject), msg)
        }

        // Set send error button listener
        closeButton.setOnClickListener {
            dismiss()
        }

        // Process arguments and update UI
        processArguments()
    }

    private fun sendEmail(context: Context, recipient: String, subject: String, body: String) {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            context.startActivity(emailIntent)
        } catch (e: Exception) {
            Timber.w("No email app found: ${e.message}")
        }
    }

    private fun processArguments() {
        val args = arguments ?: return

        // Check if we're in loading state
        val isLoading = args.getBoolean(ARG_IS_LOADING, false)
        if (isLoading) {
            showLoadingState()
            return
        }

        // Check if there's an error
        val error = args.getString(ARG_ERROR)
        if (error != null) {
            val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                args.getParcelableArrayList(ARG_MESSAGES, Message::class.java) ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                args.getParcelableArrayList(ARG_MESSAGES) ?: emptyList()
            }
            showErrorState(error, messages)
            return
        }

        // Process response
        val response = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable(ARG_RESPONSE, LlmApiResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable<LlmApiResponse>(ARG_RESPONSE)
        }
        val originalText = args.getString(ARG_ORIGINAL_TEXT)
        val showReplace = args.getBoolean(ARG_SHOW_REPLACE, false)

        if (response != null && originalText != null) {
            showResponseState(response, showReplace)
        } else {
            showErrorState(getString(R.string.error_checking_text), emptyList())
        }
    }

    private fun showLoadingState() {
        loadingView.isVisible = true
        naturalTextView.isVisible = false
        improvementsLabelView.isVisible = false
        suggestionsContainer.isVisible = false
        errorTextView.isVisible = false
        sendErrorButton.isVisible = false
    }

    private fun showErrorState(errorMessage: String, messages: List<Message>) {
        loadingView.isVisible = false
        naturalTextView.isVisible = false
        improvementsLabelView.isVisible = false
        suggestionsContainer.isVisible = false
        errorTextView.isVisible = true
        sendErrorButton.isVisible = (messages.isNotEmpty())

        errorTextView.text = errorMessage
    }

    private fun showResponseState(response: LlmApiResponse, showReplace: Boolean) {
        loadingView.isVisible = false
        errorTextView.isVisible = false

        if (response.isNatural) {
            // Text is natural
            naturalTextView.isVisible = true
            improvementsLabelView.isVisible = false
            suggestionsContainer.isVisible = false

            // Set green color for natural text indicator
            naturalTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        } else {
            // Text needs improvement
            naturalTextView.isVisible = false
            improvementsLabelView.isVisible = true
            suggestionsContainer.isVisible = true

            // Clear previous suggestions
            suggestionsContainer.removeAllViews()

            // Add suggestion views
            for (suggestion in response.suggestions) {
                addSuggestionView(suggestion, showReplace)
            }
        }
    }

    private fun addSuggestionView(suggestion: Suggestion, showReplace: Boolean) {
        val inflater = getLayoutInflater()
        val suggestionView = inflater.inflate(R.layout.item_suggestion, suggestionsContainer, false)

        val improvedTextView = suggestionView.findViewById<TextView>(R.id.improvedTextView)
        val explanationView = suggestionView.findViewById<TextView>(R.id.explanationView)
        val copyButton = suggestionView.findViewById<Button>(R.id.copyButton)
        val replaceButton = suggestionView.findViewById<Button>(R.id.replaceButton)

        // Set the text
        improvedTextView.text = suggestion.improvedText
        explanationView.text = suggestion.explanation

        // Set copy button listener
        copyButton.setOnClickListener {
            copyToClipboard(suggestion.improvedText)
        }

        // Set replace button listener
        if (showReplace) {
            replaceButton.setOnClickListener {
                onReplaceClickListener?.invoke(suggestion.improvedText)
                dismiss()
            }
            replaceButton.isVisible = true
        } else {
            replaceButton.isVisible = false
        }

        // Add to container
        suggestionsContainer.addView(suggestionView)
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Improved Text", text)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(requireContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }

    fun setOnReplaceClickListener(listener: (String) -> Unit) {
        onReplaceClickListener = listener
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }
}