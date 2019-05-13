package io.github.domi04151309.home

import android.content.SharedPreferences
import org.json.JSONObject

class Devices constructor(prefs: SharedPreferences) {

    private val _prefs: SharedPreferences? = prefs

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
        var url = getDeviceObject(name).getString("address")
        if (!(url.startsWith("https://") || url.startsWith("http://")))
            url = "http://$url"
        if (!url.endsWith("/"))
            url += "/"
        return url
    }

    fun getIconName(name: String): String {
        return getDeviceObject(name).getString("icon")
    }

    fun getIconId(name: String): Int {
        return when (getIconName(name)) {
            "Lamp" -> R.drawable.ic_device_lamp
            "Laptop" -> R.drawable.ic_device_laptop
            "Phone" -> R.drawable.ic_device_phone
            "Raspberry Pi" -> R.drawable.ic_device_raspberry_pi
            "Speaker" -> R.drawable.ic_device_speaker
            "Stack" -> R.drawable.ic_device_stack
            "Tablet" -> R.drawable.ic_device_tablet
            "TV" -> R.drawable.ic_device_tv
            else -> {
                R.drawable.ic_warning
            }
        }
    }

    fun getMode(name: String): String {
        return getDeviceObject(name).getString("mode")
    }

    fun addDevice(name: String, address: String, icon: String, mode: String) {
        val newObject = getDevicesObject()
        val deviceObject = JSONObject().put("address", address).put("icon", icon).put("mode", mode)
        newObject.put(name, deviceObject)
        _prefs!!.edit().putString("devices_json", JSONObject().put("devices", newObject).toString()).apply()
    }

    fun deleteDevice(name: String) {
        val newObject = getDevicesObject()
        newObject.remove(name)
        _prefs!!.edit().putString("devices_json", JSONObject().put("devices", newObject).toString()).apply()
    }
}