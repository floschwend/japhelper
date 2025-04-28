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
