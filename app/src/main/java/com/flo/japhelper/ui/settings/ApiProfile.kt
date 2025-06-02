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

import com.flo.japhelper.utils.SharedPrefsHelper
import kotlinx.serialization.Serializable

@Serializable
data class ApiProfile(
    val id: String,
    val name: String,
    val apiEndpoint: String,
    val apiModel: String,
    val temperature: Float,
    val language: String,
    val apiKey: String = ""
) {
    companion object {
        fun createDefault(id: String = "default", name: String = "Default"): ApiProfile {
            return ApiProfile(
                id = id,
                name = name,
                apiEndpoint = SharedPrefsHelper.DEFAULT_FREE_API_ENDPOINT,
                apiModel = SharedPrefsHelper.DEFAULT_MODEL,
                temperature = SharedPrefsHelper.DEFAULT_TEMPERATURE.toFloat(),
                language = SharedPrefsHelper.DEFAULT_LANGUAGE
            )
        }
    }
}