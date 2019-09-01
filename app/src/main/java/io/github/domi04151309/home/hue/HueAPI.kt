package io.github.domi04151309.home.hue

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.*
import io.github.domi04151309.home.R
import io.github.domi04151309.home.Global.volleyError
import org.json.JSONArray
import org.json.JSONObject
import android.os.Handler


class HueAPI(context: Context, deviceId: String) {

    private val c = context
    private val selectedDevice = deviceId
    private var url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)
    private var readyForRequest = true

    interface RequestCallBack {
        fun onGroupsLoaded(
                context: Context,
                response: JSONObject?,
                deviceId: String,
                errorMessage: String = ""
        )
        fun onLightsLoaded(
                context: Context,
                response: JSONObject?,
                deviceId: String,
                errorMessage: String = ""
        )
    }

    fun getUsername(): String {
        val username = PreferenceManager.getDefaultSharedPreferences(c).getString(selectedDevice, "none") ?: ""
        return if (username == "none") {
            c.startActivity(Intent(c, HueConnectActivity::class.java).putExtra("deviceId", selectedDevice))
            ""
        } else {
            username
        }
    }

    fun loadGroups(callback: RequestCallBack) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "api/" + getUsername() + "/groups", null,
                Response.Listener { response ->
                    callback.onGroupsLoaded(c, response, selectedDevice)
                },
                Response.ErrorListener { error ->
                    callback.onGroupsLoaded(c, null, selectedDevice, volleyError(c, error))
                }
        )
        queue.add(jsonObjectRequest)
    }

    fun loadLightsByIDs(lightIDs: JSONArray, callback: RequestCallBack) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "api/" + getUsername() + "/lights", null,
                Response.Listener { response ->
                    try {
                        val returnObject = JSONObject()
                        val count = lightIDs.length()
                        for (i in 0 until count) {
                            val lightID = lightIDs.getString(i)
                            returnObject.put(lightID, response.getJSONObject(lightID))
                        }
                        callback.onLightsLoaded(c, returnObject, selectedDevice)
                    } catch (e: Exception) {
                        callback.onLightsLoaded(c, null, selectedDevice, c.resources.getString(R.string.err_wrong_format_summary))
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                Response.ErrorListener { error ->
                    callback.onLightsLoaded(c, null, selectedDevice, volleyError(c, error))
                }
        )
        queue.add(jsonObjectRequest)
    }

    fun switchLightByID(lightID: String, on: Boolean) {
        putObject("/lights/$lightID/state", "{\"on\":$on}")
    }

    fun changeBrightness(lightID: String, bri: Int) {
        putObject("/lights/$lightID/state", "{\"bri\":$bri}")
    }

    fun changeColorTemperature(lightID: String, ct: Int) {
        putObject("/lights/$lightID/state", "{\"ct\":$ct}")
    }

    fun changeHue(lightID: String, hue: Int) {
        putObject("/lights/$lightID/state", "{\"hue\":$hue}")
    }

    fun changeSaturation(lightID: String, sat: Int) {
        putObject("/lights/$lightID/state", "{\"sat\":$sat}")
    }

    fun switchGroupByID(groupID: String, on: Boolean) {
        putObject("/groups/$groupID/action", "{\"on\":$on}")
    }

    fun changeBrightnessOfGroup(groupID: String, bri: Int) {
        putObject("/groups/$groupID/action", "{\"bri\":$bri}")
    }

    fun changeColorTemperatureOfGroup(groupID: String, ct: Int) {
        putObject("/groups/$groupID/action", "{\"ct\":$ct}")
    }

    fun changeHueOfGroup(groupID: String, hue: Int) {
        putObject("/groups/$groupID/action", "{\"hue\":$hue}")
    }

    fun changeSaturationOfGroup(groupID: String, sat: Int) {
        putObject("/groups/$groupID/action", "{\"sat\":$sat}")
    }

    fun activateSceneOfGroup(groupID: String, scene: String) {
        putObject("/groups/$groupID/action", "{\"scene\":$scene}")
    }

    private fun putObject(address: String, requestObject: String) {
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + address, JSONObject(requestObject),
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        if (readyForRequest) {
            queue.add(request)
            readyForRequest = false
            Handler().postDelayed({ readyForRequest = true }, 100)
        }
    }
}