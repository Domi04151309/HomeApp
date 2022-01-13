package io.github.domi04151309.home.api

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.Global.volleyError
import org.json.JSONArray
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import io.github.domi04151309.home.activities.HueConnectActivity
import io.github.domi04151309.home.activities.HueLampActivity
import io.github.domi04151309.home.data.RequestCallbackObject
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

class HueAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface? = null
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    var readyForRequest: Boolean = true
    init {
        dynamicSummaries = false
        needsRealTimeData = true
    }

    interface RequestCallback {
        fun onLightsLoaded(holder: RequestCallbackObject<JSONObject>)
    }

    fun getUsername(): String {
        return PreferenceManager.getDefaultSharedPreferences(c).getString(deviceId, "") ?: ""
    }

    // For unified API
    override fun loadList(callback: CallbackInterface) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "api/${getUsername()}/groups", null,
            { response ->
                try {
                    val listItems: ArrayList<ListViewItem> = ArrayList(response.length())
                    val rooms: ArrayList<Group> = ArrayList(response.length() / 2)
                    val zones: ArrayList<Group> = ArrayList(response.length() / 2)
                    var currentObject: JSONObject
                    for (i in response.keys()) {
                        currentObject = response.getJSONObject(i)
                        if (currentObject.getString("type") == "Room") rooms.add(Group(i, currentObject))
                        else zones.add(Group(i, currentObject))
                    }
                    val sortedRooms = rooms.sortedWith(compareBy{ it.value.getString("name") })
                    for (i in sortedRooms.indices) {
                        listItems.add(parseGroupObj(sortedRooms[i].key, sortedRooms[i].value, false))
                    }
                    val sortedZones = zones.sortedWith(compareBy{ it.value.getString("name") })
                    for (i in sortedZones.indices) {
                        listItems.add(parseGroupObj(sortedZones[i].key, sortedZones[i].value, true))
                    }
                    callback.onItemsLoaded(UnifiedRequestCallback(listItems, deviceId), recyclerViewInterface)
                } catch (e: Exception) {
                    callback.onItemsLoaded(UnifiedRequestCallback(null, deviceId, volleyError(c, e)), null)
                }
            },
            { error ->
                callback.onItemsLoaded(UnifiedRequestCallback( null, deviceId, volleyError(c, error)), null)
                if (error is ParseError) c.startActivity(Intent(c, HueConnectActivity::class.java).putExtra("deviceId", deviceId))
            }
        )
        queue.add(jsonObjectRequest)
    }

    override fun loadStates(callback: RealTimeStatesCallback, offset: Int) {
        if (!readyForRequest) return
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "api/${getUsername()}/groups", null,
            { response ->
                try {
                    val states: ArrayList<Boolean?> = ArrayList(response.length())
                    val rooms: ArrayList<Group> = ArrayList(response.length() / 2)
                    val zones: ArrayList<Group> = ArrayList(response.length() / 2)
                    var currentObject: JSONObject
                    for (i in response.keys()) {
                        currentObject = response.getJSONObject(i)
                        if (currentObject.getString("type") == "Room") rooms.add(Group(i, currentObject))
                        else zones.add(Group(i, currentObject))
                    }
                    val sortedRooms = rooms.sortedWith(compareBy{ it.value.getString("name") })
                    for (i in sortedRooms.indices) {
                        states.add(
                            sortedRooms[i].value
                                .getJSONObject("state")
                                .getBoolean("any_on")
                        )
                    }
                    val sortedZones = zones.sortedWith(compareBy{ it.value.getString("name") })
                    for (i in sortedZones.indices) {
                        states.add(
                            sortedZones[i].value
                                .getJSONObject("state")
                                .getBoolean("any_on")
                        )
                    }
                    callback.onStatesLoaded(states, offset, dynamicSummaries)
                } catch (e: Exception) {}
            },
            { }
        )
        queue.add(jsonObjectRequest)
    }

    override fun execute(path: String, callback: CallbackInterface) {
        c.startActivity(
            Intent(c, HueLampActivity::class.java)
                .putExtra("ID", path)
                .putExtra("Device", deviceId)
        )
    }

    override fun changeSwitchState(id: String, state: Boolean) {
        switchGroupByID(id.substring(id.lastIndexOf('#') + 1), state)
    }

    private fun parseGroupObj(key: String, obj: JSONObject, isZone: Boolean): ListViewItem {
        return ListViewItem(
            title = obj.getString("name"),
            summary = c.resources.getString(R.string.hue_tap),
            hidden = "${if (isZone) "zone" else "room"}#$key",
            icon = if (isZone) R.drawable.ic_zone else R.drawable.ic_room,
            state = obj.getJSONObject("action").getBoolean("on")
        )
    }

    fun loadLightsByIDs(lightIDs: JSONArray, callback: RequestCallback) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "api/${getUsername()}/lights", null,
                { response ->
                    try {
                        val returnObject = JSONObject()
                        var lightID: String
                        for (i in 0 until lightIDs.length()) {
                            lightID = lightIDs.getString(i)
                            returnObject.put(lightID, response.getJSONObject(lightID))
                        }
                        callback.onLightsLoaded(RequestCallbackObject(returnObject, deviceId))
                    } catch (e: Exception) {
                        callback.onLightsLoaded(RequestCallbackObject(null, deviceId, c.resources.getString(R.string.err_wrong_format_summary)))
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                { error ->
                    callback.onLightsLoaded(RequestCallbackObject(null, deviceId, volleyError(c, error)))
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