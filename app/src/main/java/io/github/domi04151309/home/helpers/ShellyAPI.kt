package io.github.domi04151309.home.helpers

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.data.RequestCallbackObject
import org.json.JSONArray

class ShellyAPI(private val c: Context, deviceId: String, private val version: Int) {

    private val selectedDevice = deviceId
    private var url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)

    interface RequestCallBack {
        fun onResponse(holder: RequestCallbackObject)
        fun onSwitchesLoaded(holder: RequestCallbackObject)
    }

    fun getBasicDeviceInfo(callback: RequestCallBack) {
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url + "shelly", null,
            { response ->
                callback.onResponse(RequestCallbackObject(c, response, selectedDevice))
            },
            { error ->
                callback.onResponse(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
            }
        )
        queue.add(jsonObjectRequest)
    }

    fun loadSwitches(callback: RequestCallBack) {
        //TODO: Authenticate first
        val jsonObjectRequest = when (version) {
            1 -> JsonObjectRequest(
                Request.Method.GET, url + "status", null,
                { response ->
                    val relays = response.getJSONArray("relays")
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
                    val relayIds = ArrayList<Int>(names.length() / 2)
                    var currentName: String
                    for (i in 0 until names.length()) {
                        currentName = names.getString(i)
                        if (currentName.contains("switch:")) relayIds.add(currentName.substring(7).toInt())
                    }

                    val relays = JSONArray()
                    var completedRequests = 0
                    for (i in 0 until relayIds.size) {
                        //TODO: authenticate
                        queue.add(JsonObjectRequest(
                            Request.Method.GET, url + "relay/$i", null,
                            { secondResponse ->
                                relays.put(secondResponse)
                                completedRequests++
                                if (completedRequests == relayIds.size) {
                                    callback.onSwitchesLoaded(RequestCallbackObject(
                                        c,
                                        relays.toJSONObject(JSONArray(IntArray(relays.length()) { it })),
                                        selectedDevice
                                    ))
                                }
                            },
                            { error ->
                                callback.onResponse(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
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

    fun changeSwitchState(id: Int, state: Boolean, callback: RequestCallBack) {
        //TODO: Authenticate first
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url + "relay/$id?turn=" + (if (state) "on" else "off"), null,
            { response ->
                callback.onResponse(RequestCallbackObject(c, response, selectedDevice))
            },
            { error ->
                callback.onResponse(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
            }
        )
        queue.add(jsonObjectRequest)
    }
}