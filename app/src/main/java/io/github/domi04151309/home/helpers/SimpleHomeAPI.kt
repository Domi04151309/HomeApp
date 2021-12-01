package io.github.domi04151309.home.helpers

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.helpers.Global.volleyError
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.RequestCallbackObject

class SimpleHomeAPI(context: Context) {

    private val c = context

    interface RequestCallBack {
        fun onCommandsLoaded(holder: RequestCallbackObject)
        fun onExecutionFinished(context: Context, result: CharSequence, refresh: Boolean = false, deviceId: String = "")
    }

    fun loadCommands(deviceId: String, callback: RequestCallBack) {
        val url = Devices(c).getDeviceById(deviceId).address
        val queue = Volley.newRequestQueue(c)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "commands", null,
                { response ->
                    callback.onCommandsLoaded(RequestCallbackObject(c, response, deviceId))
                },
                { error ->
                    callback.onCommandsLoaded(RequestCallbackObject(c, null, deviceId, volleyError(c, error)))
                }
        )
        queue.add(jsonObjectRequest)
    }

    fun executeCommand(deviceId: String, url: String, callback: RequestCallBack) {
        val queue = Volley.newRequestQueue(c)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                { response ->
                    try {
                        callback.onExecutionFinished(
                            c,
                            response.optString("toast", c.resources.getString(R.string.main_execution_completed)),
                            response.optBoolean("refresh", false),
                            deviceId
                        )

                    } catch (e: Exception) {
                        callback.onExecutionFinished(c, c.resources.getString(R.string.main_execution_completed))
                        Log.w(Global.LOG_TAG, e.toString())
                    }
                },
                { error ->
                    callback.onExecutionFinished(c, volleyError(c, error))
                }
        )
        queue.add(jsonObjectRequest)
    }
}