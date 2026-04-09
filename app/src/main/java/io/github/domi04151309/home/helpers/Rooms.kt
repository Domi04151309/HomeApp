package io.github.domi04151309.home.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.github.domi04151309.home.data.RoomItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Random

@Suppress("TooManyFunctions")
class Rooms(private val context: Context) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val length: Int get() = roomOrder.length()

    private val data: JSONObject get() {
        if (storedData == null) {
            storedData =
                try {
                    JSONObject(
                        preferences.getString(PREF_KEY, DEFAULT_JSON)
                            ?: DEFAULT_JSON,
                    )
                } catch (e: JSONException) {
                    Log.w(Rooms::class.simpleName, e)
                    JSONObject(DEFAULT_JSON)
                }
        }
        return storedData!!
    }

    private val roomsObject: JSONObject get() = data.optJSONObject(ROOMS) ?: JSONObject()

    private val roomOrder: JSONArray get() {
        if (!data.has(ORDER)) {
            data.put(ORDER, roomsObject.names() ?: JSONArray())
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

    private fun convertToRoomItem(id: String): RoomItem {
        val json = roomsObject.optJSONObject(id) ?: JSONObject()
        return RoomItem(
            id,
            json.optString(NAME),
            json.optString(ICON, DEFAULT_ICON),
        )
    }

    fun getRoomById(id: String): RoomItem = convertToRoomItem(id)

    fun getRoomByIndex(index: Int): RoomItem = convertToRoomItem(roomOrder.getString(index))

    fun idExists(id: String): Boolean = roomsObject.has(id)

    fun generateNewId(): String {
        var id = generateRandomId()
        while (roomsObject.has(id)) id = generateRandomId()
        return id
    }

    fun addRoom(room: RoomItem) {
        if (!idExists(room.id)) roomOrder.put(room.id)
        val roomObject =
            JSONObject()
                .put(NAME, room.name)
                .put(ICON, room.iconName)
        roomsObject.put(room.id, roomObject)
        saveChanges()
    }

    fun updateRoom(room: RoomItem) {
        if (!idExists(room.id)) return
        val roomObject =
            JSONObject()
                .put(NAME, room.name)
                .put(ICON, room.iconName)
        roomsObject.put(room.id, roomObject)
        saveChanges()
    }

    fun deleteRoom(id: String) {
        // Remove room from order
        for (i in 0 until roomOrder.length()) {
            if (roomOrder[i] == id) {
                roomOrder.remove(i)
                break
            }
        }
        roomsObject.remove(id)
        saveChanges()
    }

    fun moveRoom(
        from: Int,
        to: Int,
    ) {
        val list =
            MutableList(roomOrder.length()) {
                roomOrder.getString(it)
            }
        list.add(to, list.removeAt(from))
        data.put(ORDER, JSONArray(list))
    }

    fun saveChanges() {
        preferences.edit { putString(PREF_KEY, data.toString()) }
    }

    fun getAllRooms(): List<RoomItem> {
        val rooms = mutableListOf<RoomItem>()
        for (i in 0 until roomOrder.length()) {
            rooms.add(getRoomByIndex(i))
        }
        return rooms
    }

    fun getRoomNames(): List<String> {
        val names = mutableListOf<String>()
        for (i in 0 until roomOrder.length()) {
            names.add(getRoomByIndex(i).name)
        }
        return names
    }

    fun getRoomIdByName(name: String): String? {
        for (i in 0 until roomOrder.length()) {
            val room = getRoomByIndex(i)
            if (room.name == name) return room.id
        }
        return null
    }

    fun getRoomNameById(id: String): String {
        return if (idExists(id)) {
            getRoomById(id).name
        } else {
            ""
        }
    }

    companion object {
        const val PREF_KEY: String = "rooms_json"
        const val DEFAULT_JSON: String = "{\"rooms\":{}}"
        const val DEFAULT_ICON: String = "Lamp"

        private const val ROOMS = "rooms"
        private const val ORDER = "order"
        private const val NAME = "name"
        private const val ICON = "icon"
        private const val ALLOWED_CHARACTERS = "0123456789abcdefghijklmnobqrstuvw"
        private const val ID_LENGTH = 8
        private var storedData: JSONObject? = null

        fun reloadFromPreferences() {
            storedData = null
        }
    }
}
