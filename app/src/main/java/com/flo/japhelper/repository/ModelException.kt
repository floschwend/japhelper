/*
 Copyright (c) 2024 YOUR_NAME <YOUR_EMAIL>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY and FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.flo.japhelper.repository

import com.flo.japhelper.model.Message

class ModelException(
    val messages: List<Message> = emptyList(),
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

}