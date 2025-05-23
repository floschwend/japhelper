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

package com.flo.japhelper.network

import okhttp3.Interceptor
import okhttp3.Response
import com.flo.japhelper.BuildConfig // Import your BuildConfig
import timber.log.Timber

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (BuildConfig.DEBUG) {
            val response = chain.proceed(request)

            Timber.d("Request: ${request.method} ${request.url}")
            Timber.d("Response Code: ${response.code}")
            Timber.d("Response Headers: ${response.headers}")
            Timber.d("Content-Type: ${response.headers["Content-Type"]}") // Check the encoding
            val responseBody = response.peekBody(Long.MAX_VALUE).string()
            Timber.d("Raw Response Body: $responseBody")

            return response
        } else {
            // In release builds, just proceed with the request without logging
            return chain.proceed(request)
        }
    }
}