package io.github.domi04151309.home.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.github.domi04151309.home.data.DeviceItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class Devices(private val context: Context) {

    companion object {
        private const val ALLOWED_CHARACTERS = "0123456789abcdefghijklmnobqrstuvw"
        private var storedData: JSONObject? = null

        fun reloadFromPreferences() {
            storedData = null
        }
    }

    private val _prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val data: JSONObject get() {
        if (storedData == null) {
            storedData = JSONObject(
                _prefs.getString("devices_json", Global.DEFAULT_JSON)
                    ?: Global.DEFAULT_JSON
            )
        }
        return storedData!!
    }

    private val devicesObject: JSONObject get() {
        return data.getJSONObject("devices")
    }

    private val deviceOrder: JSONArray get() {
        if (!data.has("order")) {
            data.put("order", devicesObject.names() ?: JSONArray())
        }
        return data.getJSONArray("order")
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
        return convertToDeviceItem(id, devicesObject.getJSONObject(id))
    }

    fun getDeviceByIndex(index: Int): DeviceItem {
        val id = deviceOrder.getString(index)
        return convertToDeviceItem(id, devicesObject.getJSONObject(id))
    }

    fun length(): Int {
        return devicesObject.length()
    }

    fun idExists(id: String): Boolean {
        return devicesObject.has(id)
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
        while (devicesObject.has(id))
            id = generateRandomId()
        return id
    }

    fun addDevice(device: DeviceItem) {
        if (!idExists(device.id)) deviceOrder.put(device.id)
        val deviceObject = JSONObject()
                .put("name", device.name)
                .put("address", device.address)
                .put("mode", device.mode)
                .put("icon", device.iconName)
        devicesObject.put(device.id, deviceObject)
        saveChanges()
    }

    fun deleteDevice(id: String) {
        for (i in 0 until deviceOrder.length()) {
            if (deviceOrder[i] == id) {
                deviceOrder.remove(i)
                break
            }
        }
        devicesObject.remove(id)
        saveChanges()
        DeviceSecrets(context, id).deleteDeviceSecrets()
    }

    fun moveDevice(from: Int, to: Int) {
        val list = MutableList(deviceOrder.length()) {
            deviceOrder.getString(it)
        }
        list.add(to, list.removeAt(from))
        data.put("order", JSONArray(list))
    }

    fun saveChanges() {
        _prefs.edit().putString("devices_json", data.toString()).apply()
    }
}