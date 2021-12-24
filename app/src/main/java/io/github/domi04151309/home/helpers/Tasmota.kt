package io.github.domi04151309.home.helpers

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import org.json.JSONArray
import org.json.JSONObject
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONException

class Tasmota(private val c: Context, deviceId: String) {

    companion object {
        private const val EMPTY_ARRAY = "[]"
    }

    private val selectedDevice = deviceId
    private var url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(c)
    private val nullParent: ViewGroup? = null

    interface RequestCallBack {
        fun onItemsChanged()
        fun onResponse(response: String)
    }

    fun loadList(): ArrayList<ListViewItem> {
        val list = JSONArray(prefs.getString(selectedDevice, EMPTY_ARRAY))
        val listItems: ArrayList<ListViewItem> = ArrayList(list.length())
        if (list.length() == 0) {
            listItems += ListViewItem(
                    title = c.resources.getString(R.string.tasmota_empty_list),
                    summary = c.resources.getString(R.string.tasmota_empty_list_summary),
                    icon = R.drawable.ic_warning
            )
        } else {
            var currentItem: JSONObject
            for (i in 0 until list.length()) {
                try {
                    currentItem = list.getJSONObject(i)
                    listItems += ListViewItem(
                            title = currentItem.getString("title"),
                            summary = currentItem.getString("command"),
                            hidden = "tasmota_command#$i",
                            icon = R.drawable.ic_do
                    )
                } catch (e: JSONException) {
                    Log.e(Global.LOG_TAG, e.toString())
                }
            }
        }

        listItems += ListViewItem(
                title = c.resources.getString(R.string.tasmota_add_command),
                summary = c.resources.getString(R.string.tasmota_add_command_summary),
                icon = R.drawable.ic_add,
                hidden = "add"
        )

        listItems += ListViewItem(
                title = c.resources.getString(R.string.tasmota_execute_once),
                summary = c.resources.getString(R.string.tasmota_execute_once_summary),
                icon = R.drawable.ic_edit,
                hidden = "execute_once"
        )
        return listItems
    }

    fun updateItem(callback: RequestCallBack, index: Int) {
        val array = JSONArray(prefs.getString(selectedDevice, EMPTY_ARRAY))
        val arrayItem = array.getJSONObject(index)
        val view = LayoutInflater.from(c).inflate(R.layout.dialog_tasmota_add, nullParent, false)
        val titleTxt = view.findViewById<EditText>(R.id.title)
        val commandTxt = view.findViewById<EditText>(R.id.command)
        titleTxt.setText(arrayItem.getString("title"))
        commandTxt.setText(arrayItem.getString("command"))
        AlertDialog.Builder(c)
            .setTitle(R.string.tasmota_add_command)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newTitle = titleTxt.text.toString()
                val newCommand = commandTxt.text.toString()
                array.remove(index)
                prefs.edit().putString(selectedDevice, array.put(
                    JSONObject()
                        .put("title", if (newTitle == "") c.resources.getString(R.string.tasmota_add_command_dialog_title_empty) else newTitle)
                        .put("command", if (newCommand == "") c.resources.getString(R.string.tasmota_add_command_dialog_command_empty) else newCommand)
                ).toString()).apply()
                callback.onItemsChanged()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    fun addToList(callback: RequestCallBack, title: String = "", command: String = "") {
        val view = LayoutInflater.from(c).inflate(R.layout.dialog_tasmota_add, nullParent, false)
        val titleTxt = view.findViewById<EditText>(R.id.title)
        val commandTxt = view.findViewById<EditText>(R.id.command)
        titleTxt.setText(title)
        commandTxt.setText(command)
        AlertDialog.Builder(c)
                .setTitle(R.string.tasmota_add_command)
                .setView(view)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val newTitle = titleTxt.text.toString()
                    val newCommand = commandTxt.text.toString()
                    prefs.edit().putString(selectedDevice, JSONArray(
                            prefs.getString(selectedDevice, EMPTY_ARRAY)).put(JSONObject()
                                    .put("title", if (newTitle == "") c.resources.getString(R.string.tasmota_add_command_dialog_title_empty) else newTitle)
                                    .put("command", if (newCommand == "") c.resources.getString(R.string.tasmota_add_command_dialog_command_empty) else newCommand)
                            ).toString()).apply()
                    callback.onItemsChanged()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
    }

    fun removeFromList(callback: RequestCallBack, index: Int) {
        val array = JSONArray(prefs.getString(selectedDevice, EMPTY_ARRAY))
        array.remove(index)
        prefs.edit().putString(selectedDevice, array.toString()).apply()
        callback.onItemsChanged()
    }

    fun execute(callback: RequestCallBack, command: String) {
        val request = StringRequest(Request.Method.GET, url + command,
                { response ->
                    callback.onResponse(response)
                },
                { error ->
                    Toast.makeText(c, Global.volleyError(c, error), Toast.LENGTH_LONG).show()
                }
        )
        queue.add(request)
    }

    fun executeOnce(callback: RequestCallBack) {
        val view = LayoutInflater.from(c).inflate(R.layout.dialog_tasmota_execute_once, nullParent, false)
        val command = view.findViewById<EditText>(R.id.command)
        AlertDialog.Builder(c)
                .setTitle(R.string.tasmota_execute_once)
                .setView(LayoutInflater.from(c).inflate(R.layout.dialog_tasmota_execute_once, nullParent, false))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    execute(callback, command.text.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
    }
}