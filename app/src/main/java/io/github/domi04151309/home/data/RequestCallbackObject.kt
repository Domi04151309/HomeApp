package io.github.domi04151309.home.data

import android.content.Context
import org.json.JSONObject

data class RequestCallbackObject(
        val context: Context,
        val response: JSONObject?,
        val deviceId: String,
        val errorMessage: String = "",
        val forZone: Boolean = false
)