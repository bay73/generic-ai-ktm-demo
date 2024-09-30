package com.bay.aidemo.clients

data class AiResponse<T>(
    val response: T? = null,
    val tokenCount: Int = 0,
    val errorMessage: String? = null,
)
