package io.github.domi04151309.home.simplehome

import android.content.Context
import androidx.preference.PreferenceManager
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.*
import io.github.domi04151309.home.Global.volleyError
import io.github.domi04151309.home.R
import org.json.JSONObject

class SimpleHomeAPI(context: Context) {

    private val c = context

    interface RequestCallBack {
        fun onCommandsLoaded(
                context: Context,
                response: JSONObject?,
                device: String,
                errorMessage: String = ""
        )
        fun onExecutionFinished(context: Context, result: CharSequence)
    }

    fun loadCommands(device: String, callback: RequestCallBack) {
        val url = Devices(PreferenceManager.getDefaultSharedPreferences(c)).getAddress(device)
        val queue = Volley.newRequestQueue(c)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "commands", null,
                Response.Listener { response ->
                    callback.onCommandsLoaded(c, response, device)
                },
                Response.ErrorListener { error ->
                    callback.onCommandsLoaded(c, null, device, volleyError(c, error))
                }
        )
        queue.add(jsonObjectRequest)
    }

    fun executeCommand(url: String, callback: RequestCallBack) {
        val queue = Volley.newRequestQueue(c)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    Log.d(Global.LOG_TAG, response.toString())
                    try {
                        callback.onExecutionFinished(c, response.getString("toast"))
                    } catch (e: Exception) {
                        callback.onExecutionFinished(c, c.resources.getString(R.string.main_execution_completed))
                        Log.w(Global.LOG_TAG, e.toString())
                    }
                },
                Response.ErrorListener { error ->
                    callback.onExecutionFinished(c, volleyError(c, error))
                }
        )
        queue.add(jsonObjectRequest)
    }
}