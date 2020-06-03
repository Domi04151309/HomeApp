package io.github.domi04151309.home.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.github.domi04151309.home.objects.Global
import io.github.domi04151309.home.data.DeviceItem
import org.json.JSONObject
import java.util.*

class Devices constructor(context: Context) {

    private val _prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun getDevicesObject(): JSONObject {
        val devicesJSON = _prefs.getString("devices_json", Global.DEFAULT_JSON) ?: Global.DEFAULT_JSON
        return JSONObject(devicesJSON).getJSONObject("devices")
    }

    private fun convertToDeviceItem(id: String, jsonObj: JSONObject): DeviceItem {
        val device = DeviceItem(id)
        device.name = jsonObj.getString("name")
        device.address = jsonObj.getString("address")
        device.mode = jsonObj.getString("mode")
        device.iconName = jsonObj.getString("icon")
        return device
    }

    fun getDeviceById(id: String): DeviceItem {
        val deviceObj = getDevicesObject().getJSONObject(id)
        return convertToDeviceItem(id, deviceObj)
    }

    fun getDeviceByIndex(index: Int): DeviceItem {
        val devicesObject = getDevicesObject().names()
        val id = devicesObject!!.getString(index)
        val deviceObj = getDevicesObject().getJSONObject(id)
        return convertToDeviceItem(id, deviceObj)
    }

    fun length(): Int {
        return getDevicesObject().length()
    }

    fun idExists(id: String): Boolean {
        return getDevicesObject().has(id)
    }

    companion object {
        private const val ALLOWED_CHARACTERS = "0123456789abcdefghijklmnobqrstuvw"
    }

    private fun generateRandomId(): String {
        val random = Random()
        val sb = StringBuilder(8)
        for (i in 0 until 8)
            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
        return sb.toString()
    }

    fun generateNewId(): String {
        var id = generateRandomId()
        while (getDevicesObject().has(id))
            id = generateRandomId()
        return id
    }

    fun addDevice(device: DeviceItem) {
        val newObject = getDevicesObject()
        val deviceObject = JSONObject()
                .put("name", device.name)
                .put("address", device.address)
                .put("mode", device.mode)
                .put("icon", device.iconName)
        newObject.put(device.id, deviceObject)
        _prefs.edit().putString("devices_json", JSONObject().put("devices", newObject).toString()).apply()
    }

    fun deleteDevice(id: String) {
        val newObject = getDevicesObject()
        newObject.remove(id)
        _prefs.edit().putString("devices_json", JSONObject().put("devices", newObject).toString()).apply()
    }
}