package io.github.domi04151309.home.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import io.github.domi04151309.home.data.DeviceItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class Devices(context: Context) {

    companion object {
        private const val ALLOWED_CHARACTERS = "0123456789abcdefghijklmnobqrstuvw"
        private var storedData: JSONObject? = null
    }

    private val _prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    init {
        if (storedData == null) {
            storedData = JSONObject(
                _prefs.getString("devices_json", Global.DEFAULT_JSON)
                    ?: Global.DEFAULT_JSON
            )
        }
    }

    private fun getDevicesObject(): JSONObject {
        return storedData?.getJSONObject("devices")!!
    }

    private fun getDeviceOrder(): JSONArray {
        if (storedData?.has("order") != true) {
            storedData?.put("order", getDevicesObject().names())
        }
        return storedData?.getJSONArray("order")!!
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
        return convertToDeviceItem(id, getDevicesObject().getJSONObject(id))
    }

    fun getDeviceByIndex(index: Int): DeviceItem {
        val id = getDeviceOrder()!!.getString(index)
        return convertToDeviceItem(id, getDevicesObject().getJSONObject(id))
    }

    fun length(): Int {
        return getDevicesObject().length()
    }

    fun idExists(id: String): Boolean {
        return getDevicesObject().has(id)
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
        if (!idExists(device.id)) getDeviceOrder().put(device.id)
        val deviceObject = JSONObject()
                .put("name", device.name)
                .put("address", device.address)
                .put("mode", device.mode)
                .put("icon", device.iconName)
        getDevicesObject().put(device.id, deviceObject)
        saveChanges()
    }

    fun deleteDevice(id: String) {
        for (i in 0 until getDeviceOrder().length()) {
            if (getDeviceOrder()[i] == id) {
                getDeviceOrder().remove(i)
                break
            }
        }
        getDevicesObject().remove(id)
        saveChanges()
    }

    fun moveDevice(from: Int, to: Int) {
        val list = MutableList(getDeviceOrder().length()) {
            getDeviceOrder().getString(it)
        }
        list.add(to, list.removeAt(from))
        storedData?.put("order", JSONArray(list))
        Log.e(Global.LOG_TAG, list.toString())
    }

    fun saveChanges() {
        _prefs.edit().putString("devices_json", storedData.toString()).apply()
    }
}