package com.flo.japhelper.repository

import com.flo.japhelper.network.Message

class ModelException(
    val messages: List<Message> = emptyList(),
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

}