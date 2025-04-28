package com.flo.japhelper.network

import okhttp3.Interceptor
import okhttp3.Response

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val responseBody = response.peekBody(Long.MAX_VALUE).string()
        println("Raw Response Body: $responseBody")

        return response
    }
}
