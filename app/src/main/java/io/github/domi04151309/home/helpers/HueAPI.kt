package io.github.domi04151309.home.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.Global.volleyError
import org.json.JSONArray
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import io.github.domi04151309.home.activities.HueConnectActivity
import io.github.domi04151309.home.data.RequestCallbackObject
import io.github.domi04151309.home.custom.CustomJsonArrayRequest

class HueAPI(context: Context, deviceId: String) {

    private val c = context
    private val selectedDevice = deviceId
    private val url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)
    var readyForRequest: Boolean = true

    interface RequestCallBack {
        fun onGroupsLoaded(holder: RequestCallbackObject<JSONObject>)
        fun onLightsLoaded(holder: RequestCallbackObject<JSONObject>)
    }

    fun getUsername(): String {
        return PreferenceManager.getDefaultSharedPreferences(c).getString(selectedDevice, "") ?: ""
    }

    fun loadGroups(callback: RequestCallBack) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "api/${getUsername()}/groups", null,
                { response ->
                    try {
                        val processed = JSONObject()
                        val rooms: ArrayList<Group> = ArrayList(response.length() / 2)
                        val zones: ArrayList<Group> = ArrayList(response.length() / 2)
                        var currentObjectName: String
                        var currentObject: JSONObject
                        for (i in 0 until response.length()) {
                            currentObjectName = response.names()?.getString(i) ?: ""
                            currentObject = response.getJSONObject(currentObjectName)
                            if (currentObject.getString("type") == "Room") rooms.add(Group(currentObjectName, currentObject))
                            else zones.add(Group(currentObjectName, currentObject))
                        }
                        val sortedRooms = rooms.sortedWith(compareBy{ it.value.getString("name") })
                        for (i in sortedRooms.indices) {
                            processed.put(sortedRooms[i].key, sortedRooms[i].value)
                        }
                        val sortedZones = zones.sortedWith(compareBy{ it.value.getString("name") })
                        for (i in sortedZones.indices) {
                            processed.put(sortedZones[i].key, sortedZones[i].value)
                        }
                        callback.onGroupsLoaded(RequestCallbackObject(c, processed, selectedDevice))
                    } catch (e: Exception) {
                        callback.onGroupsLoaded(RequestCallbackObject(c, null, selectedDevice, volleyError(c, e)))
                    }
                },
                { error ->
                    callback.onGroupsLoaded(RequestCallbackObject(c, null, selectedDevice, volleyError(c, error)))
                    if (error is ParseError) c.startActivity(Intent(c, HueConnectActivity::class.java).putExtra("deviceId", selectedDevice))
                }
        )
        queue.add(jsonObjectRequest)
    }

    fun loadLightsByIDs(lightIDs: JSONArray, callback: RequestCallBack, forZone: Boolean = false) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "api/${getUsername()}/lights", null,
                { response ->
                    try {
                        val returnObject = JSONObject()
                        var lightID: String
                        for (i in 0 until lightIDs.length()) {
                            lightID = lightIDs.getString(i)
                            returnObject.put(lightID, response.getJSONObject(lightID))
                        }
                        callback.onLightsLoaded(RequestCallbackObject(c, returnObject, selectedDevice, forZone = forZone))
                    } catch (e: Exception) {
                        callback.onLightsLoaded(RequestCallbackObject(c, null, selectedDevice, c.resources.getString(R.string.err_wrong_format_summary)))
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                { error ->
                    callback.onLightsLoaded(RequestCallbackObject(c, null, selectedDevice, volleyError(c, error)))
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

    fun changeHueSat(lightID: String, hue: Int, sat: Int) {
        putObject("/lights/$lightID/state", "{\"hue\":$hue, \"sat\":$sat}")
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

    fun changeHueSatOfGroup(groupID: String, hue: Int, sat: Int) {
        putObject("/groups/$groupID/action", "{\"hue\":$hue, \"sat\":$sat}")
    }

    fun activateSceneOfGroup(groupID: String, scene: String) {
        putObject("/groups/$groupID/action", "{\"scene\":$scene}")
    }

    private fun putObject(address: String, requestObject: String) {
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/${getUsername()}$address", JSONObject(requestObject),
                { },
                { e -> Log.e(Global.LOG_TAG, e.toString()) }
        )
        if (readyForRequest) {
            readyForRequest = false
            queue.add(request)
            Handler(Looper.getMainLooper()).postDelayed({ readyForRequest = true }, 100)
        }
    }

    private data class Group(
            val key: String = "",
            val value: JSONObject = JSONObject()
    )
}