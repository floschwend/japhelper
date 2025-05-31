package com.flo.japhelper.network

import com.flo.japhelper.model.ModelListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ModelInfoService {
    @GET("models")
    suspend fun getAvailableModels(
        //@Header("Authorization") auth: String,
        @Header("Accept") accept: String = "application/json",
        @Header("User-Agent") agent: String = "PostmanRuntime/7.43.3",
    ): Response<ModelListResponse>
}