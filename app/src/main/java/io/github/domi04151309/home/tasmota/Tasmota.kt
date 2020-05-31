package io.github.domi04151309.home.tasmota

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.*
import io.github.domi04151309.home.R
import org.json.JSONArray
import org.json.JSONObject
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONException


class Tasmota(context: Context, deviceId: String) {

    private val c = context
    private val selectedDevice = deviceId
    private var url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(c)

    fun loadList(): Array<ListViewItem> {
        val list = JSONArray(prefs.getString(selectedDevice, "[]"))
        var listItems: Array<ListViewItem> = arrayOf()
        if (list.length() == 0) {
            val listItem = ListViewItem(c.resources.getString(R.string.tasmota_empty_list))
            listItem.summary = c.resources.getString(R.string.tasmota_empty_list_summary)
            listItem.icon = R.drawable.ic_warning
            listItems += listItem
        } else {
            var currentItem: JSONObject
            for (i in 0 until list.length()) {
                try {
                    currentItem = list.getJSONObject(i)
                    val listItem = ListViewItem(currentItem.getString("title"))
                    listItem.summary = currentItem.getString("command")
                    listItem.hidden = url + currentItem.getString("command")
                    listItem.icon = R.drawable.ic_do
                    listItems += listItem
                } catch (e: JSONException) {
                    Log.e(Global.LOG_TAG, e.toString())
                }
            }
        }

        val addItem = ListViewItem(c.resources.getString(R.string.tasmota_add_command))
        addItem.summary = c.resources.getString(R.string.tasmota_add_command_summary)
        addItem.icon = R.drawable.ic_add
        listItems += addItem

        val executeItem = ListViewItem(c.resources.getString(R.string.tasmota_execute_once))
        executeItem.summary = c.resources.getString(R.string.tasmota_execute_once_summary)
        executeItem.icon = R.drawable.ic_edit
        listItems += executeItem
        return listItems
    }

    fun addToList() {

    }

    fun removeFromList() {

    }

    fun execute() {

    }
}