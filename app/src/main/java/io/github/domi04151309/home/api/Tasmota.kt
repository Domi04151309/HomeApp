package io.github.domi04151309.home.api

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.TasmotaHelper
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Tasmota(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?,
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(c)

    override fun loadList(
        callback: CallbackInterface,
        extended: Boolean,
    ) {
        super.loadList(callback, extended)
        val list = JSONArray(prefs.getString(deviceId, TasmotaHelper.EMPTY_ARRAY))
        val listItems: ArrayList<ListViewItem> = ArrayList(list.length())
        if (list.length() == 0) {
            listItems +=
                ListViewItem(
                    title = c.resources.getString(R.string.tasmota_empty_list),
                    summary = c.resources.getString(R.string.tasmota_empty_list_summary),
                    icon = R.drawable.ic_warning,
                )
        } else {
            var currentItem: JSONObject
            for (i in 0 until list.length()) {
                try {
                    currentItem = list.optJSONObject(i) ?: JSONObject()
                    listItems +=
                        ListViewItem(
                            title = currentItem.optString("title"),
                            summary = currentItem.optString("command"),
                            hidden = "tasmota_command#$i",
                            icon = R.drawable.ic_do,
                        )
                } catch (e: JSONException) {
                    Log.e(Global.LOG_TAG, e.toString())
                }
            }
        }

        if (extended) {
            listItems +=
                ListViewItem(
                    title = c.resources.getString(R.string.tasmota_add_command),
                    summary = c.resources.getString(R.string.tasmota_add_command_summary),
                    icon = R.drawable.ic_add,
                    hidden = "add",
                )

            listItems +=
                ListViewItem(
                    title = c.resources.getString(R.string.tasmota_execute_once),
                    summary = c.resources.getString(R.string.tasmota_execute_once_summary),
                    icon = R.drawable.ic_edit,
                    hidden = "execute_once",
                )
        }

        updateCache(listItems)
        callback.onItemsLoaded(UnifiedRequestCallback(listItems, deviceId), recyclerViewInterface)
    }

    override fun execute(
        path: String,
        callback: CallbackInterface,
    ) {
        val request =
            StringRequest(
                Request.Method.GET,
                url + path,
                { response ->
                    callback.onExecuted(response)
                },
                { error ->
                    Toast.makeText(c, Global.volleyError(c, error), Toast.LENGTH_LONG).show()
                },
            )
        queue.add(request)
    }
}
