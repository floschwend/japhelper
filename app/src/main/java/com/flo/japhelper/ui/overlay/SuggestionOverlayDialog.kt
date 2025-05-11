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

package com.flo.japhelper.ui.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.flo.japhelper.network.Message

class SuggestionOverlayDialog : DialogFragment() {
    private var onDismissListener: (() -> Unit)? = null
    private var onReplaceClickListener: ((String) -> Unit)? = null

    private lateinit var loadingView: ProgressBar
    private lateinit var naturalTextView: TextView
    private lateinit var improvementsLabelView: TextView
    private lateinit var suggestionsContainer: LinearLayout
    private lateinit var errorTextView: TextView
    private lateinit var closeButton: Button

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

        // Set close button listener
        closeButton.setOnClickListener {
            dismiss()
        }

        // Process arguments and update UI
        processArguments()
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
            val messages = args.getParcelableArrayList<Message>(ARG_MESSAGES) ?: emptyList()
            showErrorState(error, messages)
            return
        }

        // Process response
        val response = args.getParcelable<LlmApiResponse>(ARG_RESPONSE)
        val originalText = args.getString(ARG_ORIGINAL_TEXT)
        val showReplace = args.getBoolean(ARG_SHOW_REPLACE, false)

        if (response != null && originalText != null) {
            showResponseState(response, originalText, showReplace)
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
    }

    private fun showErrorState(errorMessage: String, messages: List<Message>) {
        loadingView.isVisible = false
        naturalTextView.isVisible = false
        improvementsLabelView.isVisible = false
        suggestionsContainer.isVisible = false
        errorTextView.isVisible = true

        errorTextView.text = errorMessage
    }

    private fun showResponseState(response: LlmApiResponse, originalText: String, showReplace: Boolean) {
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
                addSuggestionView(suggestion, originalText, showReplace)
            }
        }
    }

    private fun addSuggestionView(suggestion: Suggestion, originalText: String, showReplace: Boolean) {
        val inflater = LayoutInflater.from(requireContext())
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