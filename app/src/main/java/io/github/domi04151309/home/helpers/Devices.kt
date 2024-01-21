package io.github.domi04151309.home.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import io.github.domi04151309.home.data.DeviceItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Random

@Suppress("TooManyFunctions")
class Devices(private val context: Context) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val length: Int get() = deviceOrder.length()

    private val data: JSONObject get() {
        if (storedData == null) {
            storedData =
                try {
                    JSONObject(
                        preferences.getString("devices_json", Global.DEFAULT_JSON)
                            ?: Global.DEFAULT_JSON,
                    )
                } catch (e: JSONException) {
                    Log.w(Devices::class.simpleName, e)
                    JSONObject(Global.DEFAULT_JSON)
                }
        }
        return storedData!!
    }

    private val devicesObject: JSONObject get() = data.optJSONObject("devices") ?: JSONObject()

    private val deviceOrder: JSONArray get() {
        if (!data.has(ORDER)) {
            data.put(ORDER, devicesObject.names() ?: JSONArray())
        }
        return data.getJSONArray(ORDER)
    }

    private fun generateRandomId(): String {
        val random = Random()
        val builder = StringBuilder(ID_LENGTH)
        for (index in 0 until ID_LENGTH) {
            builder.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
        }
        return builder.toString()
    }

    private fun convertToDeviceItem(id: String): DeviceItem {
        val json = devicesObject.optJSONObject(id) ?: JSONObject()
        val device =
            DeviceItem(
                id,
                json.optString("name"),
                json.optString("mode"),
                json.optString("icon"),
                json.optBoolean("hide", false),
                json.optBoolean("direct_view", false),
            )
        device.address = json.optString(ADDRESS)
        return device
    }

    fun getDeviceById(id: String): DeviceItem = convertToDeviceItem(id)

    fun getDeviceByIndex(index: Int): DeviceItem = convertToDeviceItem(deviceOrder.getString(index))

    fun idExists(id: String): Boolean = devicesObject.has(id)

    fun addressExists(address: String): Boolean {
        val formattedAddress = DeviceItem.formatAddress(address)
        for (i in devicesObject.keys()) {
            if (devicesObject.getJSONObject(i).optString(ADDRESS) == formattedAddress) {
                return true
            }
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
        val deviceObject =
            JSONObject()
                .put("name", device.name)
                .put(ADDRESS, device.address)
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

    fun moveDevice(
        from: Int,
        to: Int,
    ) {
        val list =
            MutableList(deviceOrder.length()) {
                deviceOrder.getString(it)
            }
        list.add(to, list.removeAt(from))
        data.put(ORDER, JSONArray(list))
    }

    fun saveChanges() {
        preferences.edit().putString("devices_json", data.toString()).apply()
    }

    companion object {
        const val INTENT_EXTRA_DEVICE: String = "device"

        private const val ALLOWED_CHARACTERS = "0123456789abcdefghijklmnobqrstuvw"
        private const val ID_LENGTH = 8
        private const val ORDER = "order"
        private const val ADDRESS = "address"
        private var storedData: JSONObject? = null

        fun reloadFromPreferences() {
            storedData = null
        }
    }
}
