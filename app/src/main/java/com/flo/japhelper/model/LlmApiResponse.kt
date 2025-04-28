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