package io.github.domi04151309.home.tasmota

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.*
import io.github.domi04151309.home.R
import org.json.JSONArray
import org.json.JSONObject
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONException


class Tasmota(context: Context, deviceId: String) {

    companion object {
        private const val EMPTY_ARRAY = "[]"
    }

    private val c = context
    private val selectedDevice = deviceId
    private var url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(c)
    private val nullParent: ViewGroup? = null

    fun loadList(): Array<ListViewItem> {
        val list = JSONArray(prefs.getString(selectedDevice, EMPTY_ARRAY))
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
        addItem.hidden = "add"
        listItems += addItem

        val executeItem = ListViewItem(c.resources.getString(R.string.tasmota_execute_once))
        executeItem.summary = c.resources.getString(R.string.tasmota_execute_once_summary)
        executeItem.icon = R.drawable.ic_edit
        executeItem.hidden = "execute_once"
        listItems += executeItem
        return listItems
    }

    fun addToList() {
        val view = LayoutInflater.from(c).inflate(R.layout.dialog_tasmota_add, nullParent, false)
        val title = view.findViewById<EditText>(R.id.title)
        val command = view.findViewById<EditText>(R.id.command)
        AlertDialog.Builder(c)
                .setTitle(R.string.tasmota_add_command)
                .setView(view)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val commandObj = JSONObject().put("title", title.text.toString()).put("command", command.text.toString())
                    prefs.edit().putString(selectedDevice, JSONArray(prefs.getString(selectedDevice, EMPTY_ARRAY)).put(commandObj).toString()).apply()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
    }

    fun removeFromList() {

    }

    fun execute(command: String) {
        val request = StringRequest(Request.Method.GET, url + command,
                Response.Listener { response ->
                    Toast.makeText(c, R.string.main_execution_completed, Toast.LENGTH_LONG).show()
                },
                Response.ErrorListener { error ->
                    Toast.makeText(c, Global.volleyError(c, error), Toast.LENGTH_LONG).show()
                }
        )
        queue.add(request)
    }

    fun executeOnce() {
        val view = LayoutInflater.from(c).inflate(R.layout.dialog_tasmota_execute_once, nullParent, false)
        val command = view.findViewById<EditText>(R.id.command)
        AlertDialog.Builder(c)
                .setTitle(R.string.tasmota_execute_once)
                .setView(LayoutInflater.from(c).inflate(R.layout.dialog_tasmota_execute_once, nullParent, false))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    execute(command.text.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
    }
}