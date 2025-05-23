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

package com.flo.japhelper.ui.overlay // Using the package name from your context

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

@Composable
fun LlmDisclosureDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    llmProviderName: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Important: How Your Text is Used") },
        text = {
            val annotatedString = buildAnnotatedString {
                append("This feature sends your text to an AI for correction and analysis. ")
                append("Your text will be sent to ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(llmProviderName)
                }
                append(" for processing. ")
                append("\n\nBy clicking 'Accept', you agree to this. Please review our ")

                val linkText = "Privacy Policy"
                val linkStartIndex = length
                append(linkText)
                val linkEndIndex = length

                addLink(
                    url = LinkAnnotation.Url(url = "https://github.com/floschwend/japhelper/blob/master/PRIVACY.md"),
                    start = linkStartIndex,
                    end = linkEndIndex
                )

                addStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = linkStartIndex,
                    end = linkEndIndex
                )
                append(" for more details.")
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Decline")
            }
        }
    )
}