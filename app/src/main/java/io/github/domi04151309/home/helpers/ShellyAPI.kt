package io.github.domi04151309.home.helpers

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.custom.JsonObjectRequestAuth
import io.github.domi04151309.home.data.RequestCallbackObject
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class ShellyAPI(private val c: Context, deviceId: String, private val version: Int) {

    private val selectedDevice = deviceId
    private val url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)
    private val secrets = DeviceSecrets(c, deviceId)

    interface RequestCallBack {
        fun onSwitchesLoaded(holder: RequestCallbackObject<JSONObject>)
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
                        relays.toJSONObject(JSONArray(IntArray(relays.length()) { it })),
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
                    val names = response.names() ?: JSONArray()
                    val relayNames = mutableMapOf<Int, String>()
                    var currentName: String
                    for (i in 0 until names.length()) {
                        currentName = names.getString(i)
                        if (currentName.contains("switch:")) {
                            val properties = response.getJSONObject(currentName)
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
                                        JSONObject(TreeMap(relays).toMap()),
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
}
