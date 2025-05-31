package com.flo.japhelper.model

data class ModelListResponse(
    val data: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    val name: String,
    val pricing: Pricing
)

data class Pricing(
    val prompt: String?,
    val completion: String?
)
