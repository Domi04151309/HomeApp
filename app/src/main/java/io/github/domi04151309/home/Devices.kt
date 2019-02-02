package io.github.domi04151309.home

import android.content.SharedPreferences
import org.json.JSONObject

class Devices constructor(prefs: SharedPreferences) {

    private var _prefs: SharedPreferences? = prefs

    private fun getDevicesObject(): JSONObject {
        return JSONObject(_prefs!!.getString("devices_json", Global.DEFAULT_JSON)).getJSONObject("devices")
    }

    fun length(): Int {
        return getDevicesObject().length()
    }

    fun getName(index: Int): String {
        return getDevicesObject().names().getString(index)
    }

    private fun getDeviceObject(name: String): JSONObject {
        return JSONObject(getDevicesObject().getString(name))
    }

    fun getAddress(name: String): String {
        return getDeviceObject(name).getString("address")
    }

    fun getIcon(name: String): String {
        return getDeviceObject(name).getString("icon")
    }

    fun getMode(name: String): String {
        return getDeviceObject(name).getString("mode")
    }
}