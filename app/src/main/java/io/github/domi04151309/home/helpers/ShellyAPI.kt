package io.github.domi04151309.home.helpers

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.custom.JsonObjectRequestAuth
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.RequestCallbackObject
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ShellyAPI(private val c: Context, deviceId: String, private val version: Int) {

    private val selectedDevice = deviceId
    private val url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)
    private val secrets = DeviceSecrets(c, deviceId)

    interface RequestCallBack {
        fun onSwitchesLoaded(holder: RequestCallbackObject<ArrayList<ListViewItem>>)
    }

    fun loadSwitches(callback: RequestCallBack) {
        val jsonObjectRequest = when (version) {
            1 -> JsonObjectRequestAuth(
                Request.Method.GET, url + "settings", secrets, null,
                { response ->
                    val relays = response.optJSONArray("relays") ?: JSONArray()
                    var currentItem: JSONObject
                    for (i in 0 until relays.length()) {
                        currentItem = relays.getJSONObject(i)
                        if (!currentItem.has("name") || currentItem.isNull("name"))
                            currentItem.put("name", "")
                    }
                    callback.onSwitchesLoaded(RequestCallbackObject(
                        c,
                        parseItems(relays.toJSONObject(JSONArray(IntArray(relays.length()) { it }))),
                        selectedDevice
                    ))
                },
                { error ->
                    callback.onSwitchesLoaded(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
                }
            )
            2 -> JsonObjectRequest(
                Request.Method.GET, url + "rpc/Shelly.GetConfig", null,
                { response ->
                    val relayNames = mutableMapOf<Int, String>()
                    for (i in response.keys()) {
                        if (i.contains("switch:")) {
                            val properties = response.getJSONObject(i)
                            relayNames[properties.getInt("id")] =
                                if (properties.isNull("name")) ""
                                else properties.getString("name")
                        }
                    }

                    val relays = HashMap<String, JSONObject>(relayNames.size)
                    var completedRequests = 0
                    for (i in relayNames.keys) {
                        queue.add(JsonObjectRequest(
                            Request.Method.GET, url + "relay/$i", null,
                            { secondResponse ->
                                secondResponse.put("name", relayNames[i])
                                relays[i.toString()] = secondResponse
                                completedRequests++
                                if (completedRequests == relayNames.size) {
                                    callback.onSwitchesLoaded(RequestCallbackObject(
                                        c,
                                        parseItems(JSONObject(TreeMap(relays).toMap())),
                                        selectedDevice
                                    ))
                                }
                            },
                            { error ->
                                callback.onSwitchesLoaded(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
                            }
                        ))
                    }
                },
                { error ->
                    callback.onSwitchesLoaded(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
                }
            )
            else -> null
        }
        queue.add(jsonObjectRequest)
    }

    fun changeSwitchState(id: Int, state: Boolean) {
        val requestUrl = url + "relay/$id?turn=" + (if (state) "on" else "off")
        val jsonObjectRequest = when (version) {
            1 -> JsonObjectRequestAuth(
                Request.Method.GET, requestUrl, secrets, null,
                { },
                { e -> Log.e(Global.LOG_TAG, e.toString()) }
            )
            2 -> JsonObjectRequest(
                Request.Method.GET, requestUrl, null,
                { },
                { e -> Log.e(Global.LOG_TAG, e.toString()) }
            )
            else -> null
        }
        queue.add(jsonObjectRequest)
    }

    private fun parseItems(response : JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()
        var currentState: Boolean
        var currentName: String
        for (i in response.keys()) {
            currentState = response.getJSONObject(i).getBoolean("ison")
            currentName = response.getJSONObject(i).optString("name", "")
            if (currentName.trim() == "") {
                currentName = c.resources.getString(R.string.shelly_switch_title, i.toInt() + 1)
            }
            listItems += ListViewItem(
                title = currentName,
                summary = c.resources.getString(
                    if (currentState) R.string.shelly_switch_summary_on
                    else R.string.shelly_switch_summary_off
                ),
                hidden = i,
                state = currentState,
                icon = R.drawable.ic_do
            )
        }
        return listItems
    }
}
