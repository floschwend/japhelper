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

package com.flo.japhelper.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class LlmApiResponse(
    @SerializedName("natural")
    val isNatural: Boolean,

    @SerializedName("suggestions")
    val suggestions: List<Suggestion>
) : Parcelable

@Parcelize
data class Suggestion(
    @SerializedName("improved_text")
    val improvedText: String,

    @SerializedName("explanation")
    val explanation: String
) : Parcelable