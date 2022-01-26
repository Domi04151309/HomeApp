package io.github.domi04151309.home.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.github.domi04151309.home.data.DeviceItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Random

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
            storedData = try {
                JSONObject(
                    _prefs.getString("devices_json", Global.DEFAULT_JSON)
                        ?: Global.DEFAULT_JSON
                )
            } catch (e: JSONException) {
                JSONObject(Global.DEFAULT_JSON)
            }
        }
        return storedData!!
    }

    private val devicesObject: JSONObject get() {
        return data.optJSONObject("devices") ?: JSONObject()
    }

    private val deviceOrder: JSONArray get() {
        if (!data.has("order")) {
            data.put("order", devicesObject.names() ?: JSONArray())
        }
        return data.getJSONArray("order")
    }

    private fun generateRandomId(): String {
        val random = Random()
        val sb = StringBuilder(8)
        for (i in 0 until 8)
            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
        return sb.toString()
    }

    private fun convertToDeviceItem(id: String): DeviceItem {
        val json = devicesObject.optJSONObject(id) ?: JSONObject()
        val device = DeviceItem(id)
        device.name = json.optString("name")
        device.address = json.optString("address")
        device.mode = json.optString("mode")
        device.iconName = json.optString("icon")
        device.hide = json.optBoolean("hide", false)
        device.directView = json.optBoolean("direct_view", false)
        return device
    }

    fun getDeviceById(id: String): DeviceItem {
        return convertToDeviceItem(id)
    }

    fun getDeviceByIndex(index: Int): DeviceItem {
        val id = deviceOrder.getString(index)
        return convertToDeviceItem(id)
    }

    val length: Int get() {
        return deviceOrder.length()
    }

    fun idExists(id: String): Boolean {
        return devicesObject.has(id)
    }

    fun addressExists(address: String): Boolean {
        val formattedAddress = DeviceItem.formatAddress(address)
        for (i in devicesObject.keys()) {
            if (devicesObject.getJSONObject(i).optString("address") == formattedAddress)
                return true
        }
        return false
    }

    fun generateNewId(): String {
        var id = generateRandomId()
        while (devicesObject.has(id)) id = generateRandomId()
        return id
    }

    fun addDevice(device: DeviceItem) {
        if (!idExists(device.id)) deviceOrder.put(device.id)
        val deviceObject = JSONObject()
            .put("name", device.name)
            .put("address", device.address)
            .put("mode", device.mode)
            .put("icon", device.iconName)
            .put("hide", device.hide)
            .put("direct_view", device.directView)
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