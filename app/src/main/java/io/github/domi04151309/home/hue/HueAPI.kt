package io.github.domi04151309.home.hue

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.*
import io.github.domi04151309.home.R
import io.github.domi04151309.home.Global.volleyError
import org.json.JSONArray
import org.json.JSONObject

class HueAPI(context: Context, device: String) {

    private val c = context
    private val selectedDevice = device
    private var url = Devices(PreferenceManager.getDefaultSharedPreferences(c)).getAddress(device)
    private val queue = Volley.newRequestQueue(c)

    interface RequestCallBack {
        fun onGroupsLoaded(
                context: Context,
                response: JSONObject?,
                device: String,
                errorMessage: String = ""
        )
        fun onLightsLoaded(
                context: Context,
                response: JSONObject?,
                device: String,
                errorMessage: String = ""
        )
    }

    fun getUsername(): String? {
        val username = PreferenceManager.getDefaultSharedPreferences(c).getString(selectedDevice, "none")
        return if (username == "none") {
            c.startActivity(Intent(c, HueConnectActivity::class.java).putExtra("Device", selectedDevice).putExtra("URL", url))
            null
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

    fun turnOnLightByID(lightID: String) {
        val jsonRequestObject = JSONObject("{\"on\":true}")
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + "/lights/" + lightID + "/state", jsonRequestObject,
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        queue.add(request)
    }

    fun turnOffLightByID(lightID: String) {
        val jsonRequestObject = JSONObject("{\"on\":false}")
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + "/lights/" + lightID + "/state", jsonRequestObject,
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        queue.add(request)
    }

    fun changeBrightness(lightID: String, bri: Int) {
        val jsonRequestObject = JSONObject("{\"bri\":$bri}")
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + "/lights/" + lightID + "/state", jsonRequestObject,
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        queue.add(request)
    }

    fun changeColorTemperature(lightID: String, ct: Int) {
        val jsonRequestObject = JSONObject("{\"ct\":$ct}")
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + "/lights/" + lightID + "/state", jsonRequestObject,
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        queue.add(request)
    }

    fun turnOnGroupByID(groupID: String) {
        val jsonRequestObject = JSONObject("{\"on\":true}")
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + "/groups/" + groupID + "/action", jsonRequestObject,
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        queue.add(request)
    }

    fun turnOffGroupByID(groupID: String) {
        val jsonRequestObject = JSONObject("{\"on\":false}")
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + "/groups/" + groupID + "/action", jsonRequestObject,
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        queue.add(request)
    }

    fun changeBrightnessOfGroup(groupID: String, bri: Int) {
        val jsonRequestObject = JSONObject("{\"bri\":$bri}")
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + "/groups/" + groupID + "/action", jsonRequestObject,
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        queue.add(request)
    }

    fun changeColorTemperatureOfGroup(groupID: String, ct: Int) {
        val jsonRequestObject = JSONObject("{\"ct\":$ct}")
        val request = CustomJsonArrayRequest(Request.Method.PUT, url + "api/" + getUsername() + "/groups/" + groupID + "/action", jsonRequestObject,
                Response.Listener { },
                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString())}
        )
        queue.add(request)
    }
}