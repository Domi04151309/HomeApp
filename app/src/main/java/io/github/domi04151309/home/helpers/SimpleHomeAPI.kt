package io.github.domi04151309.home.helpers

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.helpers.Global.volleyError
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import org.json.JSONException

class SimpleHomeAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    override fun loadList(callback: UnifiedAPI.CallbackInterface) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "commands", null,
            { response ->
                val commandsObject = Commands(response.getJSONObject("commands"))
                val listItems: ArrayList<ListViewItem> = ArrayList(commandsObject.length())
                for (i in 0 until commandsObject.length()) {
                    try {
                        commandsObject.selectCommand(i)
                        listItems += ListViewItem(
                            title = commandsObject.getSelectedTitle(),
                            summary = commandsObject.getSelectedSummary(),
                            hidden = Devices(c).getDeviceById(deviceId).address + commandsObject.getSelected(),
                            icon = R.drawable.ic_do
                        )
                    } catch (e: JSONException) {
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                }
                callback.onItemsLoaded(UnifiedRequestCallback(listItems, deviceId), recyclerViewInterface)
            },
            { error ->
                callback.onItemsLoaded(UnifiedRequestCallback(null, deviceId, volleyError(c, error)), null)
            }
        )
        queue.add(jsonObjectRequest)
    }

    override fun execute(url: String, callback: UnifiedAPI.CallbackInterface) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                callback.onExecuted(
                    response.optString("toast", c.resources.getString(R.string.main_execution_completed)),
                    deviceId,
                    response.optBoolean("refresh", false)
                )
            },
            { error ->
                callback.onExecuted(volleyError(c, error))
            }
        )
        queue.add(jsonObjectRequest)
    }
}