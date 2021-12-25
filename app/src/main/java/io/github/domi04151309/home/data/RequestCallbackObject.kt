package io.github.domi04151309.home.data

data class RequestCallbackObject<T>(
        val response: T?,
        val deviceId: String,
        val errorMessage: String = ""
)