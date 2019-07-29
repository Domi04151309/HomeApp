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
import org.json.JSONException

class SimpleHomeAPI(context: Context) {

    private val c = context

    interface RequestCallBack {
        fun onCommandsLoaded(
                context: Context,
                errorMessage: String,
                device: String = "",
                titles: Array<String?> = arrayOfNulls(0),
                summaries: Array<String?> = arrayOfNulls(0),
                commandAddresses: Array<String?> = arrayOfNulls(0)
        )
        fun onExecutionFinished(context: Context, result: CharSequence)
    }

    fun loadCommands(device: String, callback: RequestCallBack) {
        val url = Devices(PreferenceManager.getDefaultSharedPreferences(c)).getAddress(device)
        val queue = Volley.newRequestQueue(c)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "commands", null,
                Response.Listener { response ->
                    try {
                        Log.d(Global.LOG_TAG, response.toString())
                        val commandsObject = Commands(response.getJSONObject("commands"))
                        val count = commandsObject.length()
                        val titles = arrayOfNulls<String>(count)
                        val summaries = arrayOfNulls<String>(count)
                        val commandAddresses = arrayOfNulls<String>(count)
                        var i = 0
                        while (i < count) {
                            try {
                                commandsObject.selectCommand(i)
                                titles[i] = commandsObject.getSelectedTitle()
                                summaries[i] = commandsObject.getSelectedSummary()
                                commandAddresses[i] = url + commandsObject.getSelected()
                            } catch (e: JSONException) {
                                Log.e(Global.LOG_TAG, e.toString())
                            }
                            i++
                        }

                        callback.onCommandsLoaded(c, "", device, titles, summaries, commandAddresses)
                    } catch (e: Exception) {
                        callback.onCommandsLoaded(c, c.resources.getString(R.string.err_wrong_format_summary))
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                Response.ErrorListener { error ->
                    callback.onCommandsLoaded(c, volleyError(c, error))
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