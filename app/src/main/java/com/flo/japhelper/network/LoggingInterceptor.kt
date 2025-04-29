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

package com.flo.japhelper.network

import okhttp3.Interceptor
import okhttp3.Response

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        println("Authorization Header being sent: ${request.header("Authorization")}") // Log the header

        val response = chain.proceed(request)

        println("Request: ${request.method} ${request.url}")
        println("Response Code: ${response.code}")
        println("Response Headers: ${response.headers}")
        println("Content-Type: ${response.headers["Content-Type"]}") // Check the encoding
        val responseBody = response.peekBody(Long.MAX_VALUE).string()
        println("Raw Response Body: $responseBody")

        return response
    }
}
