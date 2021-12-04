package io.github.domi04151309.home.data

import android.content.Context

data class RequestCallbackObject<T>(
        val context: Context,
        val response: T?,
        val deviceId: String,
        val errorMessage: String = "",
        val forZone: Boolean = false
)