package io.github.domi04151309.home

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import org.json.JSONObject

object Devices {

    private var prefs: SharedPreferences? = null

    private fun getDevicesObject(c: Context): JSONObject {
        prefs = PreferenceManager.getDefaultSharedPreferences(c)
        return JSONObject(prefs!!.getString("devices_json", "{\"devices\":{}}")).getJSONObject("devices")
    }

    fun length(context: Context): Int {
        return getDevicesObject(context).length()
    }

    fun getName(index: Int, context: Context): String {
        return getDevicesObject(context).names().getString(index)
    }

    private fun getDeviceObject(name: String, c: Context): JSONObject {
        return JSONObject(getDevicesObject(c).getString(name))
    }

    fun getAddress(name: String, context: Context): String {
        return getDeviceObject(name, context).getString("address")
    }

    fun getIcon(name: String, context: Context): String {
        return getDeviceObject(name, context).getString("icon")
    }
}