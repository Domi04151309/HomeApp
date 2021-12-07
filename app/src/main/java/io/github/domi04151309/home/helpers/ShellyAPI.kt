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
                { settingsResponse ->
                    queue.add(JsonObjectRequest(
                        Request.Method.GET, url + "status", null,
                        { statusResponse ->
                            val parser = ShellyAPIParser(url, c.resources)
                            callback.onSwitchesLoaded(RequestCallbackObject(
                                    c,
                                    parser.parseListItemsJsonV1(settingsResponse, statusResponse),
                                    selectedDevice
                            ))
                        },
                        { error ->
                            callback.onSwitchesLoaded(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
                        }
                    ))
                },
                { error ->
                    callback.onSwitchesLoaded(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
                }
            )
            2 -> JsonObjectRequest(
                Request.Method.GET, url + "rpc/Shelly.GetConfig", null,
                { configResponse ->
                    queue.add(JsonObjectRequest(
                        Request.Method.GET, url + "rpc/Shelly.GetStatus", null,
                        { statusResponse ->
                            val parser = ShellyAPIParser(url, c.resources)
                            callback.onSwitchesLoaded(RequestCallbackObject(
                                    c,
                                    parser.parseListItemsJsonV2(configResponse, statusResponse),
                                    selectedDevice
                            ))
                        },
                        { error ->
                            callback.onSwitchesLoaded(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
                        }
                    ))
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
