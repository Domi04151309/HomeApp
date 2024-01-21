package io.github.domi04151309.home.api

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.activities.HueConnectActivity
import io.github.domi04151309.home.activities.HueLampActivity
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Global.volleyError
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import org.json.JSONArray
import org.json.JSONObject

@Suppress("TooManyFunctions")
class HueAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface? = null,
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {
    private val parser = HueAPIParser(c.resources)
    var readyForRequest: Boolean = true

    init {
        dynamicSummaries = false
        needsRealTimeData = true
    }

    interface RequestCallback {
        fun onLightsLoaded(response: JSONObject?)
    }

    fun getUsername(): String =
        PreferenceManager.getDefaultSharedPreferences(c)
            .getString(deviceId, "")
            ?: ""

    // For unified API
    override fun loadList(
        callback: CallbackInterface,
        extended: Boolean,
    ) {
        super.loadList(callback, extended)
        val jsonObjectRequest =
            JsonObjectRequest(
                Request.Method.GET,
                url + "api/${getUsername()}/groups",
                null,
                { response ->
                    val listItems = parser.parseResponse(response)
                    updateCache(listItems)
                    callback.onItemsLoaded(
                        UnifiedRequestCallback(
                            listItems,
                            deviceId,
                        ),
                        recyclerViewInterface,
                    )
                },
                { error ->
                    callback.onItemsLoaded(
                        UnifiedRequestCallback(
                            null,
                            deviceId,
                            volleyError(c, error),
                        ),
                        null,
                    )
                    if (error is ParseError) {
                        c.startActivity(
                            Intent(
                                c,
                                HueConnectActivity::class.java,
                            ).putExtra("deviceId", deviceId),
                        )
                    }
                },
            )
        queue.add(jsonObjectRequest)
    }

    override fun loadStates(
        callback: RealTimeStatesCallback,
        offset: Int,
    ) {
        if (!readyForRequest) return
        val jsonObjectRequest =
            JsonObjectRequest(
                Request.Method.GET,
                url + "api/${getUsername()}/groups",
                null,
                { response ->
                    callback.onStatesLoaded(parser.parseStates(response), offset, dynamicSummaries)
                },
                { },
            )
        queue.add(jsonObjectRequest)
    }

    override fun execute(
        path: String,
        callback: CallbackInterface,
    ) {
        c.startActivity(
            Intent(c, HueLampActivity::class.java)
                .putExtra("id", path)
                .putExtra(Devices.INTENT_EXTRA_DEVICE, deviceId),
        )
    }

    override fun changeSwitchState(
        id: String,
        state: Boolean,
    ) {
        switchGroupById(id, state)
    }

    fun loadLightsByIds(
        lightIds: JSONArray,
        callback: RequestCallback,
    ) {
        val jsonObjectRequest =
            JsonObjectRequest(
                Request.Method.GET,
                url + "api/${getUsername()}/lights",
                null,
                { response ->
                    val returnObject = JSONObject()
                    var lightId: String
                    for (i in 0 until lightIds.length()) {
                        lightId = lightIds.getString(i)
                        returnObject.put(lightId, response.getJSONObject(lightId))
                    }
                    callback.onLightsLoaded(returnObject)
                },
                { callback.onLightsLoaded(null) },
            )
        queue.add(jsonObjectRequest)
    }

    fun switchLightById(
        lightId: String,
        on: Boolean,
    ) {
        putObject(getLightPath(lightId), "{\"on\":$on}")
    }

    fun changeBrightness(
        lightId: String,
        bri: Int,
    ) {
        putObject(getLightPath(lightId), "{\"bri\":$bri}")
    }

    fun changeColorTemperature(
        lightId: String,
        ct: Int,
    ) {
        putObject(getLightPath(lightId), "{\"ct\":$ct}")
    }

    fun changeHue(
        lightId: String,
        hue: Int,
    ) {
        putObject(getLightPath(lightId), "{\"hue\":$hue}")
    }

    fun changeSaturation(
        lightId: String,
        sat: Int,
    ) {
        putObject(getLightPath(lightId), "{\"sat\":$sat}")
    }

    fun changeHueSat(
        lightId: String,
        hue: Int,
        sat: Int,
    ) {
        putObject(getLightPath(lightId), """{ "hue": $hue, "sat": $sat }""")
    }

    fun switchGroupById(
        groupId: String,
        on: Boolean,
    ) {
        putObject(getGroupPath(groupId), "{\"on\":$on}")
    }

    fun changeBrightnessOfGroup(
        groupId: String,
        bri: Int,
    ) {
        putObject(getGroupPath(groupId), "{\"bri\":$bri}")
    }

    fun changeColorTemperatureOfGroup(
        groupId: String,
        ct: Int,
    ) {
        putObject(getGroupPath(groupId), "{\"ct\":$ct}")
    }

    fun changeHueOfGroup(
        groupId: String,
        hue: Int,
    ) {
        putObject(getGroupPath(groupId), "{\"hue\":$hue}")
    }

    fun changeSaturationOfGroup(
        groupId: String,
        sat: Int,
    ) {
        putObject(getGroupPath(groupId), "{\"sat\":$sat}")
    }

    fun changeHueSatOfGroup(
        groupId: String,
        hue: Int,
        sat: Int,
    ) {
        putObject(getGroupPath(groupId), """{ "hue": $hue, "sat": $sat }""")
    }

    fun activateSceneOfGroup(
        groupId: String,
        scene: String,
    ) {
        putObject(getGroupPath(groupId), """{ "scene": $scene }""")
    }

    private fun getLightPath(lightId: String) = "/lights/$lightId/state"

    private fun getGroupPath(groupId: String) = "/groups/$groupId/action"

    private fun putObject(
        address: String,
        requestObject: String,
    ) {
        val request =
            CustomJsonArrayRequest(
                Request.Method.PUT,
                url + "api/${getUsername()}$address",
                JSONObject(requestObject),
                { },
                { e -> Log.e(Global.LOG_TAG, e.toString()) },
            )
        if (readyForRequest) {
            readyForRequest = false
            queue.add(request)
            Handler(Looper.getMainLooper()).postDelayed({ readyForRequest = true }, UPDATE_DELAY)
        }
    }

    companion object {
        private const val UPDATE_DELAY = 100L
    }
}
